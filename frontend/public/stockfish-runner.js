importScripts('stockfish.js');

Stockfish({
    locateFile: function (file) {
        if (file.endsWith('.wasm')) return '/stockfish.wasm';
        return file;
    }
}).then(function (sf) {
    self.onmessage = function (event) {
        sf.postMessage(event.data);
    };

    sf.addMessageListener(function (line) {
        self.postMessage(line);
    });
});
