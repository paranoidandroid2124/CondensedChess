import { escapeHtml } from 'lib';
import { pubsub } from 'lib/pubsub';

let timeout: Timeout | undefined;

interface Announcement {
  msg?: string;
  date?: string;
}

const kill = () => {
  if (timeout) clearTimeout(timeout);
  timeout = undefined;
  $('#announce').remove();
};

const display = (d: Announcement) => {
  kill();
  if (d.msg) {
    $('body')
      .append(
        '<div id="announce" class="announce">' +
          escapeHtml(d.msg) +
          (d.date ? '<time class="timeago" datetime="' + d.date + '"></time>' : '') +
          '<div class="actions"><a class="close">×</a></div>' +
          '</div>',
      )
      .find('#announce .close')
      .on('click', kill);
    const millis = d.date ? new Date(d.date).getTime() - Date.now() : 5000;
    if (millis > 0) timeout = setTimeout(kill, millis);
    else kill();
    if (d.date) pubsub.emit('content-loaded');
  }
};

const fromPage = (): Announcement | undefined => {
  const pageAnnounce = document.body.getAttribute('data-announce');
  return pageAnnounce && JSON.parse(pageAnnounce);
};

const announcement = fromPage();
if (announcement) display(announcement);
