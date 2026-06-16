import { commonDateFormat, toDate, formatAgo } from 'lib/format';

interface TimeagoElement extends Element {
  timeagoDate: Date;
}

export const renderTimeAgo = (parent?: HTMLElement): number =>
  requestAnimationFrame(() => {
    const now = Date.now();
    Array.from((parent ?? document).getElementsByClassName('timeago'))
      .slice(0, 99)
      .forEach(node => {
        const timeago = node as TimeagoElement;
        const cl = node.classList,
          abs = cl.contains('abs'),
          set = cl.contains('set');
        timeago.timeagoDate = timeago.timeagoDate || toDate(node.getAttribute('datetime')!);
        if (!set) {
          const str = commonDateFormat(timeago.timeagoDate);
          if (abs) node.textContent = str;
          else node.setAttribute('title', str);
          cl.add('set');
          if (abs || cl.contains('once')) cl.remove('timeago');
        }
        if (cl.contains('remaining')) {
          const diff = (timeago.timeagoDate.getTime() - now) / 1000;
          node.textContent = formatRemaining(diff);
        } else if (!abs) {
          const diff = (now - timeago.timeagoDate.getTime()) / 1000;
          node.textContent = formatAgo(diff);
          if (Math.abs(diff) > 9999) cl.remove('timeago'); // ~3h
        }
        if (site.blindMode) {
          node.removeAttribute('title');
          node.removeAttribute('datetime');
        }
      });
  });

export const updateTimeAgo = (interval: number): void => {
  renderTimeAgo();
  setTimeout(() => updateTimeAgo(interval * 1.1), interval);
};

const formatRemaining = (seconds: number): string =>
  seconds < 1
    ? 'Completed'
    : seconds < 3600
      ? `${Math.floor(seconds / 60)} minutes remaining`
      : `${Math.floor(seconds / 3600)} hours remaining`;
