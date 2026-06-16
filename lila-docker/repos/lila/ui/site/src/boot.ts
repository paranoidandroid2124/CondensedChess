import {
  initMiniBoards,
  toggleBoxInit,
  alert,
} from 'lib/view';
import { isIos, isWebkit, prefersLightThemeQuery } from 'lib/device';
import { scrollToInnerSelector, requestIdleCallback } from 'lib';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import { addDomHandlers } from './domHandlers';
import { updateTimeAgo, renderTimeAgo } from './renderTimeAgo';
import { pubsub } from 'lib/pubsub';
import { once } from 'lib/storage';
import { addExceptionListeners } from './unhandledError';

const retireDormantServiceWorkers = () => {
  if (!('serviceWorker' in navigator)) return;

  void navigator.serviceWorker.getRegistrations().then(async regs => {
    if (!regs.length) return;

    let unregistered = false;
    for (const reg of regs) {
      unregistered = (await reg.unregister()) || unregistered;
    }

    if (!unregistered) return;

    const reloadKey = 'chesstory-sw-retired';
    if (navigator.serviceWorker.controller && !sessionStorage.getItem(reloadKey)) {
      sessionStorage.setItem(reloadKey, '1');
      window.location.reload();
    } else if (!navigator.serviceWorker.controller) {
      sessionStorage.removeItem(reloadKey);
    }
  });
};

export function boot() {
  addExceptionListeners();
  $('#user_tag').removeAttr('href');
  const setBlind = location.hash === '#blind';
  const showDebug = location.hash.startsWith('#debug');

  requestAnimationFrame(() => {
    initMiniBoards();
    pubsub.on('content-loaded', initMiniBoards);
    updateTimeAgo(1000);
    pubsub.on('content-loaded', renderTimeAgo);
    pubsub.on('content-loaded', toggleBoxInit);
  });
  requestIdleCallback(() => {
    retireDormantServiceWorkers();

    $('.subnav__inner').each(function (this: HTMLElement) {
      scrollToInnerSelector(this, '.active', true);
    });

    addDomHandlers();

    // prevent zoom when keyboard shows on iOS
    if (isIos() && !('MSStream' in window)) {
      const el = document.querySelector('meta[name=viewport]') as HTMLElement;
      el.setAttribute('content', el.getAttribute('content') + ',maximum-scale=1.0');
    }

    toggleBoxInit();

    window.addEventListener('resize', dispatchChessgroundResize);

    if (setBlind && !site.blindMode) setTimeout(() => $('#blind-mode button').trigger('click'), 1500);

    if (showDebug) site.asset.loadEsm('bits.diagnosticDialog');

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
    const mql = prefersLightThemeQuery();
    if (typeof mql.addEventListener === 'function')
      mql.addEventListener('change', e => {
        if (document.body.dataset.theme === 'system')
          document.documentElement.className = e.matches ? 'light' : 'dark';
      });
  }, 800);
}

const isUnsupportedBrowser = () => isWebkit({ below: '15.4' });
