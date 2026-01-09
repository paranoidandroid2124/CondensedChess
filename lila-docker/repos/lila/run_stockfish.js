const stockfish = require('./public/npm/stockfish.js/stockfish.js');
const fs = require('fs');

const fens = process.argv.slice(2);
if (fens.length === 0) {
    console.error('Usage: node run_stockfish.js <fen1> <fen2> ...');
    process.exit(1);
}

async function analyze(fen) {
    return new Promise((resolve) => {
        const engine = stockfish();
        let variations = [];
        let depthReached = 0;

        engine.onmessage = (line) => {
            if (line.startsWith('info depth')) {
                const parts = line.split(' ');
                const depthIdx = parts.indexOf('depth');
                const multiPvIdx = parts.indexOf('multipv');
                const cpIdx = parts.indexOf('cp');
                const mateIdx = parts.indexOf('mate');
                const pvIdx = parts.indexOf('pv');

                if (multiPvIdx !== -1 && pvIdx !== -1) {
                    const depth = parseInt(parts[depthIdx + 1]);
                    const multipv = parseInt(parts[multiPvIdx + 1]);
                    const pv = parts.slice(pvIdx + 1);

                    let scoreCp = 0;
                    let mate = null;
                    if (cpIdx !== -1) scoreCp = parseInt(parts[cpIdx + 1]);
                    if (mateIdx !== -1) mate = parseInt(parts[mateIdx + 1]);

                    variations[multipv - 1] = {
                        moves: pv,
                        scoreCp: scoreCp,
                        mate: mate,
                        depth: depth
                    };
                    depthReached = depth;
                }
            } else if (line === 'bestmove (none)' || line.startsWith('bestmove')) {
                resolve({ fen, variations: variations.filter(v => v) });
            }
        };

        engine.postMessage('uci');
        engine.postMessage('setoption name MultiPV value 3');
        engine.postMessage(`position fen ${fen}`);
        engine.postMessage('go depth 15');
    });
}

(async () => {
    const results = {};
    for (const fen of fens) {
        console.error(`Analyzing ${fen}...`);
        const result = await analyze(fen);
        results[fen] = result.variations;
    }
    console.log(JSON.stringify(results, null, 2));
})();
