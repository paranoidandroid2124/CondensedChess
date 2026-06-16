import { domDialog, alert } from 'lib/view';
import { escapeHtml } from 'lib';
import { text as xhrText } from 'lib/xhr';

export function initModule({ input }: { input: HTMLInputElement }) {
  input.autocomplete = 'off';
  $(input).on('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
      execute(input.value);
      input.blur();
    }
  });
}

function execute(q: string) {
  const text = q.trim();
  if (!text) return;
  if (text[0] === '/') return command(text.slice(1));
  if (text.match(/^([1-8pnbrqk]+\/){7}.*/i))
    return (location.href = '/analysis/standard/' + text.replace(/ /g, '_'));
  alert('Paste a FEN or type /help.');
}

function command(q: string) {
  const exec = q.trim().split(/\s+/, 1)[0] ?? '';

  const is = function (commands: string) {
    return commands.split(' ').includes(exec);
  };

  if (is('light dark transp system'))
    xhrText(`/pref/bg?v=${encodeURIComponent(exec)}`, { method: 'post' }).then(() => location.reload());
  else if (is('help')) help();
  else alert(`Unknown command: "${q}". Type /help for the list of commands`);
}

function commandHelp(aliases: string, args: string, desc: string) {
  return (
    '<div class="command"><div>' +
    aliases
      .split(' ')
      .map(a => `<p>${a} ${escapeHtml(args)}</p>`)
      .join('') +
    `</div> <span>${desc}<span></div>`
  );
}

function help() {
  domDialog({
    css: [{ hashed: 'cli.help' }],
    class: 'clinput-help',
    modal: true,
    show: true,
    htmlText:
      '<div><h3>Commands</h3>' +
      commandHelp('/light /dark /transp /system', '', 'Change the background theme') +
      '<h3>Global hotkeys</h3>' +
      commandHelp('/', '', 'Type a command') +
      commandHelp('esc', '', 'Close modals like this one') +
      '</div>',
  });
}
