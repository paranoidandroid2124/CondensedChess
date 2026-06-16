import { boot } from './boot';
import Mousetrap from './mousetrap';
import { randomToken } from 'lib/algo';
import * as assets from './asset';
import { unload, redirect, reload } from './reload';
import './announce';
import sound from './sound';
import { api, type Api } from 'lib/api';
import { loadPolyfills } from './polyfill';
import { addWindowHandlers } from './domHandlers';

const site = window.site;
// site.load is initialized in site.inline.ts (body script)
// site.manifest is fetched
// The public API is exposed on window.chesstory only.
site.sri = randomToken();
site.blindMode = document.body.classList.contains('blind-mode');
site.mousetrap = new Mousetrap(document);
site.asset = assets;
site.unload = unload;
site.redirect = redirect;
site.reload = reload;
site.sound = sound;
const globals = window as Window & { chesstory?: Api };
globals.chesstory = api;
loadPolyfills();
addWindowHandlers();
if (site.load) site.load.then(boot);
else console.warn('site.load is undefined, boot skipped');
