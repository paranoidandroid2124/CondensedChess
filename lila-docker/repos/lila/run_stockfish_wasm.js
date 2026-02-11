#!/usr/bin/env node
'use strict';

const fs = require('node:fs');

// stockfish.wasm on Node 22 can fail by preferring fetch() with unsupported local paths.
// Force fs-based loading path.
globalThis.fetch = undefined;

const Stockfish = require('stockfish.wasm');

const JSON_START = '---JSON_START---';
const JSON_END = '---JSON_END---';
const DEFAULT_DEPTH = 10;
const DEFAULT_MULTI_PV = 3;
const SEARCH_TIMEOUT_MS = 30000;
const UCI_REGEX = /^[a-h][1-8][a-h][1-8][qrbn]?$/i;

function parsePositiveInt(raw, fallback) {
  const parsed = Number.parseInt(raw, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseArgs(argv) {
  if (argv[0] === '--input') {
    const inputPath = argv[1];
    if (!inputPath) throw new Error('Missing value for --input');
    const raw = fs.readFileSync(inputPath, 'utf8');
    const parsed = JSON.parse(raw);
    const fens = Array.isArray(parsed.fens) ? parsed.fens.filter(f => typeof f === 'string' && f.trim()) : [];
    const playedMovesByFen = normalizePlayedMovesByFen(parsed.playedMovesByFen);
    return {
      fens,
      playedMovesByFen,
      depth: parsePositiveInt(parsed.depth, DEFAULT_DEPTH),
      multiPv: parsePositiveInt(parsed.multiPv, DEFAULT_MULTI_PV),
    };
  }

  const fen = argv[0];
  if (!fen || !fen.trim()) {
    throw new Error('Usage: node run_stockfish_wasm.js "<fen>" [depth] [multiPv] OR --input <jsonFile>');
  }

  return {
    fens: [fen],
    playedMovesByFen: {},
    depth: parsePositiveInt(argv[1], DEFAULT_DEPTH),
    multiPv: parsePositiveInt(argv[2], DEFAULT_MULTI_PV),
  };
}

function normalizePlayedMovesByFen(raw) {
  if (!raw || typeof raw !== 'object') return {};

  const normalized = {};
  for (const [fen, moves] of Object.entries(raw)) {
    if (typeof fen !== 'string' || !fen.trim() || !Array.isArray(moves)) continue;

    const uniqueMoves = [...new Set(
      moves
        .filter(m => typeof m === 'string')
        .map(m => m.trim().toLowerCase())
        .filter(m => UCI_REGEX.test(m))
    )];

    if (uniqueMoves.length) normalized[fen] = uniqueMoves;
  }
  return normalized;
}

function parseInfoLine(line) {
  if (!line.startsWith('info ')) return null;

  const pvPos = line.indexOf(' pv ');
  if (pvPos < 0) return null;

  const scoreMatch = line.match(/\bscore (cp|mate) (-?\d+)/);
  if (!scoreMatch) return null;

  const depthMatch = line.match(/\bdepth (\d+)/);
  const multipvMatch = line.match(/\bmultipv (\d+)/);
  const moves = line
    .slice(pvPos + 4)
    .trim()
    .split(/\s+/)
    .filter(Boolean);

  if (!moves.length) return null;

  return {
    depth: depthMatch ? Number.parseInt(depthMatch[1], 10) : 0,
    multipv: multipvMatch ? Number.parseInt(multipvMatch[1], 10) : 1,
    scoreType: scoreMatch[1],
    scoreValue: Number.parseInt(scoreMatch[2], 10),
    moves,
  };
}

function whitePerspectiveSign(fen) {
  const fields = String(fen || '').trim().split(/\s+/);
  return fields[1] === 'b' ? -1 : 1;
}

function normalizeLine(parsed, perspectiveSign) {
  const sign = perspectiveSign === -1 ? -1 : 1;
  return {
    moves: parsed.moves,
    scoreCp: parsed.scoreType === 'cp' ? parsed.scoreValue * sign : 0,
    mate: parsed.scoreType === 'mate' ? parsed.scoreValue * sign : null,
    depth: parsed.depth,
  };
}

function setOption(engine, name, value) {
  engine.post(`setoption name ${name} value ${value}`);
}

function waitReady(engine, timeoutMs) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      cleanup();
      reject(new Error('Timeout waiting for readyok'));
    }, timeoutMs);

    const onLine = line => {
      if (line === 'readyok') {
        cleanup();
        resolve();
      }
    };

    const cleanup = () => {
      clearTimeout(timeout);
      engine.listeners.delete(onLine);
    };

    engine.listeners.add(onLine);
    engine.post('isready');
  });
}

function analyzeFen(engine, fen, depth, multiPv, searchMoves = []) {
  return new Promise((resolve, reject) => {
    const infoByPv = new Map();
    const perspectiveSign = whitePerspectiveSign(fen);

    const timeout = setTimeout(() => {
      cleanup();
      try {
        engine.post('stop');
      } catch (_err) {
        // no-op
      }
      reject(new Error(`Search timeout for fen: ${fen}`));
    }, SEARCH_TIMEOUT_MS);

    const onLine = line => {
      if (line.startsWith('info ')) {
        const parsed = parseInfoLine(line);
        if (!parsed) return;
        if (parsed.multipv > multiPv) return;

        const prev = infoByPv.get(parsed.multipv);
        if (!prev || parsed.depth > prev.depth || (parsed.depth === prev.depth && parsed.moves.length >= prev.moves.length))
          infoByPv.set(parsed.multipv, parsed);
        return;
      }

      if (line.startsWith('bestmove')) {
        const bestMove = line.split(/\s+/)[1];
        const variations = [...infoByPv.entries()]
          .sort((a, b) => a[0] - b[0])
          .map(([, parsed]) => normalizeLine(parsed, perspectiveSign));

        if (!variations.length && bestMove && bestMove !== '(none)') {
          variations.push({ moves: [bestMove], scoreCp: 0, mate: null, depth: 0 });
        }

        cleanup();
        resolve(variations);
      }
    };

    const cleanup = () => {
      clearTimeout(timeout);
      engine.listeners.delete(onLine);
    };

    engine.listeners.add(onLine);
    engine.post(`position fen ${fen}`);
    const searchSuffix = searchMoves.length ? ` searchmoves ${searchMoves.join(' ')}` : '';
    engine.post(`go depth ${depth}${searchSuffix}`);
  });
}

function mergeVariations(primary, extras) {
  const merged = [...primary];

  for (const extra of extras) {
    const head = extra.moves[0];
    if (!head) continue;

    const idx = merged.findIndex(v => v.moves[0] === head);
    if (idx < 0) merged.push(extra);
    else {
      const current = merged[idx];
      const currentDepth = Number.isFinite(current.depth) ? current.depth : 0;
      const extraDepth = Number.isFinite(extra.depth) ? extra.depth : 0;
      if (extraDepth > currentDepth) merged[idx] = extra;
    }
  }

  return merged;
}

async function createEngine() {
  const sf = await Stockfish();
  const listeners = new Set();

  sf.addMessageListener(line => {
    const text = String(line).trim();
    if (!text) return;
    for (const listener of listeners) listener(text);
  });

  const engine = {
    listeners,
    post: command => sf.postMessage(command),
    close: () => sf.postMessage('quit'),
  };

  await new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      cleanup();
      reject(new Error('Timeout waiting for uciok'));
    }, SEARCH_TIMEOUT_MS);

    const onLine = line => {
      if (line === 'uciok') {
        cleanup();
        resolve();
      }
    };

    const cleanup = () => {
      clearTimeout(timeout);
      listeners.delete(onLine);
    };

    listeners.add(onLine);
    engine.post('uci');
  });

  await waitReady(engine, SEARCH_TIMEOUT_MS);
  return engine;
}

async function run() {
  const config = parseArgs(process.argv.slice(2));
  const uniqueFens = [...new Set(config.fens)];
  const result = {};

  const engine = await createEngine();

  try {
    setOption(engine, 'Threads', 1);
    setOption(engine, 'Hash', 16);
    await waitReady(engine, SEARCH_TIMEOUT_MS);

    for (const fen of uniqueFens) {
      setOption(engine, 'MultiPV', config.multiPv);
      await waitReady(engine, SEARCH_TIMEOUT_MS);
      const topVariations = await analyzeFen(engine, fen, config.depth, config.multiPv);
      const requiredMoves = config.playedMovesByFen[fen] || [];
      const missingMoves = requiredMoves.filter(uci => !topVariations.some(v => v.moves[0] === uci));
      const fallbackLines = [];

      for (const playedMove of missingMoves) {
        const searched = await analyzeFen(engine, fen, config.depth, 1, [playedMove]);
        const playedLine = searched.find(v => v.moves[0] === playedMove);
        if (playedLine) fallbackLines.push(playedLine);
      }

      result[fen] = mergeVariations(topVariations, fallbackLines);
    }
  } finally {
    try {
      engine.close();
    } catch (_err) {
      // no-op
    }
  }

  console.log(JSON_START);
  console.log(JSON.stringify(result));
  console.log(JSON_END);
}

run().catch(err => {
  console.error(err && err.stack ? err.stack : String(err));
  process.exit(1);
});
