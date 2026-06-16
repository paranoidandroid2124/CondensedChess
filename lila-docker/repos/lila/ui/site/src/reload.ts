import { wsDestroy } from 'lib/socket';

let redirectInProgress = false;

export const redirect = (opts: RedirectTo) => {
  let url: string;
  if (typeof opts === 'string') url = opts;
  else {
    url = opts.url;
    if (opts.cookie) {
      const cookie = [
        encodeURIComponent(opts.cookie.name) + '=' + opts.cookie.value,
        '; max-age=' + opts.cookie.maxAge,
        '; path=/',
        '; domain=' + location.hostname,
      ].join('');
      document.cookie = cookie;
    }
  }
  const href = '//' + location.host + '/' + url.replace(/^\//, '');
  redirectInProgress = true;
  location.href = href;
};

export const unload = {
  expected: false,
};

export const reload = (err?: any) => {
  if (err) console.warn(err);
  if (redirectInProgress) return;
  unload.expected = true;
  wsDestroy();
  if (location.hash) location.reload();
  else location.assign(location.href);
};
