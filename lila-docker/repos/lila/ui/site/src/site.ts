import { boot } from './boot';
import Mousetrap from './mousetrap';
import { randomToken } from 'lib/algo';
import powertip from './powertip';
import * as assets from './asset';
import { unload, redirect, reload } from './reload';
import { display as announceDisplay } from './announce';
import { displayLocale } from 'lib/format';
import sound from './sound';
import { api } from 'lib/api';
import { loadPolyfills } from './polyfill';
import { addWindowHandlers } from './domHandlers';

const site = window.site;
// site.load is initialized in site.inline.ts (body script)
// site.manifest is fetched
// site.info, site.debug are populated by ui/build
// site.quietMode is set elsewhere
// The public API is exposed on window.chesstory.
site.sri = randomToken();
site.displayLocale = displayLocale;
site.blindMode = document.body.classList.contains('blind-mode');
site.mousetrap = new Mousetrap(document);
site.powertip = powertip;
site.asset = assets;
site.unload = unload;
site.redirect = redirect;
site.reload = reload;
site.announce = announceDisplay;
site.sound = sound;
const globals = window as any;
globals.chesstory = api;
const legacyAliasEnabled = document.body.dataset.brandLegacyLichessAlias !== '0';
if (legacyAliasEnabled && !globals.lichess) globals.lichess = api;
loadPolyfills();
addWindowHandlers();
if (site.load) site.load.then(boot);
else console.warn('site.load is undefined, boot skipped');
