type StudyPageOpts = {
  study?: { name?: string };
};

function renderDisabledMessage(title: string) {
  const host =
    (document.querySelector('main.analyse') as HTMLElement | null) ??
    (document.getElementById('main-wrap') as HTMLElement | null) ??
    document.body;

  const box = document.createElement('div');
  box.className = 'box';
  box.innerHTML = `
    <div class="box__top"><h1>${escapeHtml(title)}</h1></div>
    <div class="box__pad">
      <p>This study view is disabled in CondensedChess.</p>
      <p><a class="button" href="/analysis">Go to analysis</a></p>
    </div>
  `;
  host.appendChild(box);
}

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, c => {
    switch (c) {
      case '&':
        return '&amp;';
      case '<':
        return '&lt;';
      case '>':
        return '&gt;';
      case '"':
        return '&quot;';
      case "'":
        return '&#39;';
      default:
        return c;
    }
  });
}

export function initModule(opts?: StudyPageOpts) {
  renderDisabledMessage(opts?.study?.name ?? 'Study');
}

export default initModule;
