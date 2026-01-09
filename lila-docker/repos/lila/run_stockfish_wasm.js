const stockfish = require('./public/npm/stockfish.wasm/stockfish.js');
const fs = require('fs');

const fens = process.argv.slice(2);
if (fens.length === 0) {
    console.error('Usage: node run_stockfish_wasm.js <fen1> <fen2> ...');
    process.exit(1);
}

const wasmBinary = fs.readFileSync('./public/npm/stockfish.wasm/stockfish.wasm');

async function analyze(fen) {
    return new Promise((resolve) => {
        stockfish({
            wasmBinary,
            print: (m) => {
                // Suppress SF internal banner to stdout
                process.stderr.write(m + '\n');
            }
        }).then(sf => {
            let variations = [];
            let depthReached = 0;

            sf.addMessageListener((line) => {
                if (typeof line !== 'string') return;

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
                        if (mateIdx !== -1) {
                            mate = parseInt(parts[mateIdx + 1]);
                            // Stockfish mate score is distance to mate in half-moves
                        }

                        variations[multipv - 1] = {
                            moves: pv,
                            scoreCp: scoreCp,
                            mate: mate,
                            depth: depth
                        };
                        depthReached = depth;
                    }
                } else if (line.startsWith('bestmove')) {
                    // console.error(`Analysis complete for ${fen}`);
                    resolve(variations.filter(v => v));
                    sf.terminate();
                }
            });

            sf.postMessage('uci');
            sf.postMessage('setoption name MultiPV value 3');
            sf.postMessage(`position fen ${fen}`);
            sf.postMessage('go depth 12'); // Fixed depth for deterministic evaluation in tests
        });
    });
}

(async () => {
    const results = {};
    for (const fen of fens) {
        process.stderr.write(`Analyzing: ${fen}\n`);
        const result = await analyze(fen);
        results[fen] = result;
    }
    process.stdout.write('---JSON_START---\n');
    process.stdout.write(JSON.stringify(results, null, 2));
    process.stdout.write('\n---JSON_END---\n');
    process.exit(0);
})();
