import * as licon from 'lib/licon';
import {
  initMiniBoards,
  initMiniGames,
  updateMiniGame,
  finishMiniGame,
  toggleBoxInit,
  alert,
} from 'lib/view';
import { text as xhrText } from 'lib/xhr';
import powertip from './powertip';
import serviceWorker from './serviceWorker';
import { watchers } from 'lib/view/watchers';
import { isIos, isWebkit, prefersLightThemeQuery } from 'lib/device';
import { scrollToInnerSelector, requestIdleCallback } from 'lib';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import { addDomHandlers } from './domHandlers';
import { updateTimeAgo, renderTimeAgo } from './renderTimeAgo';
import { pubsub } from 'lib/pubsub';
import { once } from 'lib/storage';
import { addExceptionListeners } from './unhandledError';

export function boot() {
  addExceptionListeners();
  $('#user_tag').removeAttr('href');
  const setBlind = location.hash === '#blind';
  const showDebug = location.hash.startsWith('#debug');

  requestAnimationFrame(() => {
    initMiniBoards();
    initMiniGames();
    pubsub.on('content-loaded', initMiniBoards);
    pubsub.on('content-loaded', initMiniGames);
    updateTimeAgo(1000);
    pubsub.on('content-loaded', renderTimeAgo);
    pubsub.on('content-loaded', toggleBoxInit);
  });
  requestIdleCallback(() => {
    const chatMembers = document.querySelector('.chat__members') as HTMLElement | null;
    if (chatMembers) watchers(chatMembers);

    $('.subnav__inner').each(function (this: HTMLElement) {
      scrollToInnerSelector(this, '.active', true);
    });

    powertip.watchMouse();

    addDomHandlers();

    // prevent zoom when keyboard shows on iOS
    if (isIos() && !('MSStream' in window)) {
      const el = document.querySelector('meta[name=viewport]') as HTMLElement;
      el.setAttribute('content', el.getAttribute('content') + ',maximum-scale=1.0');
    }

    toggleBoxInit();

    window.addEventListener('resize', dispatchChessgroundResize);

    if (setBlind && !site.blindMode) setTimeout(() => $('#blind-mode button').trigger('click'), 1500);

    if (site.debug) site.asset.loadEsm('bits.devMode');
    if (showDebug) site.asset.loadEsm('bits.diagnosticDialog');

    serviceWorker();

    console.info('Chesstory is open source.');

    if (isUnsupportedBrowser() && once('upgrade.nag', { days: 14 })) {
      pubsub
        .after('polyfill.dialog')
        .then(() => alert('Your browser is out of date.\nChesstory may not work properly.'));
    }

    // socket default receive handlers
    pubsub.on('socket.in.redirect', (d: RedirectTo) => {
      site.unload.expected = true;
      site.redirect(d);
    });
    pubsub.on('socket.in.fen', e =>
      document.querySelectorAll('.mini-game-' + e.id).forEach((el: HTMLElement) => updateMiniGame(el, e)),
    );
    pubsub.on('socket.in.finish', e =>
      document.querySelectorAll('.mini-game-' + e.id).forEach((el: HTMLElement) => finishMiniGame(el, e.win)),
    );
    // pubsub.on('socket.in.announce', announceDisplay); // Chesstory: removed
    pubsub.on('socket.in.tournamentReminder', (data: { id: string; name: string }) => {
      if ($('#announce').length || document.body.dataset.tournamentId === data.id) return;
      const url = '/tournament/' + data.id;
      $('body').append(
        $('<div id="announce">')
          .append($(`<a data-icon="${licon.Trophy}" class="text">`).attr('href', url).text(data.name))
          .append(
            $('<div class="actions">')
              .append(
                $(`<a class="withdraw text" data-icon="${licon.Pause}">`)
                  .attr('href', url + '/withdraw')
                  .text('Pause')
                  .on('click', function (this: HTMLAnchorElement) {
                    xhrText(this.href, { method: 'post' });
                    $('#announce').remove();
                    return false;
                  }),
              )
              .append(
                $(`<a class="text" data-icon="${licon.PlayTriangle}">`)
                  .attr('href', url)
                  .text('Resume'),
              ),
          ),
      );
    });
    const mql = prefersLightThemeQuery();
    if (typeof mql.addEventListener === 'function')
      mql.addEventListener('change', e => {
        if (document.body.dataset.theme === 'system')
          document.documentElement.className = e.matches ? 'light' : 'dark';
      });

  }, 800);
}

const isUnsupportedBrowser = () => isWebkit({ below: '15.4' });
