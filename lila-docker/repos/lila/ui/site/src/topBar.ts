import { loadEsm } from './asset';

import { clamp } from 'lib/algo';


import { isTouchDevice } from 'lib/device';

export default function () {
  const top = document.getElementById('cs-top')!;



  // On touchscreens, clicking the top menu element expands it. There's no top link.
  // Only for mq-topnav-visible in ui/lib/css/abstract/_media-queries.scss
  if ('ontouchstart' in window && window.matchMedia('(min-width: 1020px)').matches)
    $('#cs-nav section > a').removeAttr('href');

  const blockBodyScroll = (e: Event) => {
    // on iOS, overflow: hidden isn't sufficient
    if (!document.getElementById('cs-nav')!.contains(e.target as HTMLElement)) e.preventDefault();
  };

  $('#cs-nav-toggle').on('change', e => {
    const menuOpen = (e.target as HTMLInputElement).checked;
    if (menuOpen) {
      document.body.addEventListener('touchmove', blockBodyScroll, { passive: false });
      $(e.target).addClass('opened');
    } else {
      document.body.removeEventListener('touchmove', blockBodyScroll);
      setTimeout(() => $(e.target).removeClass('opened'), 200);
    }
    document.body.classList.toggle('masked', menuOpen);
  });

  $(top).on('click', '.toggle', function (this: HTMLElement) {
    const $p = $(this).parent().toggleClass('shown');
    $p.siblings('.shown').removeClass('shown');
    setTimeout(() => {
      const handler = (e: Event) => {
        const target = e.target as HTMLElement;
        if (!target.isConnected || $p[0]?.contains(target)) return;
        $p.removeClass('shown');
        $('html').off('click', handler);
      };
      $('html').on('click', handler);
    }, 10);
    return false;
  });



  {
    // cli
    const $wrap = $('#clinput');
    if (!$wrap.length) return;
    const $input = $wrap.find('input');
    let booted = false;
    const boot = () => {
      if (booted) return;
      booted = true;
      loadEsm('cli', { init: { input: $input[0] } }).catch(() => (booted = false));
    };
    $input.on({
      blur() {
        $input.val('');
        $('body').removeClass('clinput');
      },
      focus() {
        boot();
        $('body').addClass('clinput');
      },
    });
    $wrap.find('a').on({
      mouseover: boot,
      click() {
        $('body').hasClass('clinput') ? $input[0]!.blur() : $input[0]!.focus();
      },
    });
    $wrap.on('mouseenter', () => {
      if ($input[0] !== document.activeElement) $input[0]!.focus();
    });
    $wrap.on('mouseleave', () => {
      if (!$input.val()) $input[0]!.blur();
    });
    site.mousetrap
      .bind('/', () => {
        $input.val('/');
        $input[0]!.focus();
        top.classList.remove('hide');
      })
      .bind('s', () => {
        $input[0]!.focus();
        top.classList.remove('hide');
      });
  }

  {
    // stick top bar
    let lastY = window.scrollY;
    if (lastY > 0) top.classList.add('scrolled');

    window.addEventListener(
      'scroll',
      () => {
        const y = window.scrollY;
        top.classList.toggle('scrolled', y > 0);
        if (y > lastY + 10) top.classList.add('hide');
        else if (y <= clamp(lastY - 20, { min: 0, max: document.body.scrollHeight - window.innerHeight }))
          top.classList.remove('hide');
        else return;

        lastY = Math.max(0, y);
      },
      { passive: true },
    );

    if (!isTouchDevice() || site.blindMode || !document.querySelector('main.analyse')) return;

    // double tap to align top of board with viewport
    document.querySelector<HTMLElement>('.main-board')?.addEventListener(
      'dblclick',
      e => {
        lastY = -9999;
        window.scrollTo({
          top: parseInt(window.getComputedStyle(document.body).getPropertyValue('---site-header-height')),
          behavior: 'instant',
        });
        e.preventDefault();
      },
      { passive: true },
    );
  }
}
