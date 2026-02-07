document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('chesstory-app');
    if (!container) return;

    const narrativeEl = container.querySelector('.chesstory__narrative');
    const conceptsEl = container.querySelector('.chesstory__concepts');

    async function fetchNarrative(fen, lastMove) {
        narrativeEl.innerHTML = '<p class="text-muted">Analyzing position with Chesstory...</p>';
        conceptsEl.innerHTML = '';

        try {
            const payload = {
                fen: fen,
                lastMove: lastMove,
                eval: null, // Optional: add client-side eval if available
                context: {
                    opening: "Unknown", // Frontend doesn't know opening yet
                    phase: "Middlegame",
                    ply: 0
                }
            };

            const response = await fetch('/api/llm/position-analysis', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) throw new Error('Chesstory Engine Unavailable');

            const data = await response.json();
            render(data);
        } catch (err) {
            narrativeEl.innerHTML = `<p class="error">Chesstory API Error: ${err.message}</p>`;
        }
    }

    function render(data) {
        const htmlInfo = data.commentary
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .split('\n\n')
            .map(p => `<p>${p}</p>`)
            .join('');

        narrativeEl.innerHTML = htmlInfo;

        if (data.concepts && data.concepts.length) {
            conceptsEl.innerHTML = data.concepts
                .map(c => `<span class="concept-tag">${c}</span>`)
                .join('');
        }
    }

    const app = window.ChesstoryAnalysis;
    if (app) {
        const btn = document.createElement('button');
        btn.className = 'button button-metal';
        btn.innerText = 'Ask Chesstory';
        btn.style.marginTop = '10px';
        btn.onclick = () => fetchNarrative(app.currentFen, app.lastMove);
        container.querySelector('.chesstory__intro').appendChild(btn);
    }
});
