import { isBrandV3ShellEnabled, readShellSelectors } from './shell';

/* Based on: */
/*!
 * hoverIntent v1.10.0 // 2019.02.25 // jQuery v1.7.0+
 * http://briancherne.github.io/jquery-hoverIntent/
 *
 * You may use hoverIntent under the terms of the MIT license. Basically that
 * means you are free to use hoverIntent as long as this header is left intact.
 * Copyright 2007-2019 Brian Cherne
 */

type State = {
  timeoutId?: Timeout | void;
  isActive?: boolean;
  pX?: number;
  pY?: number;
};

export default function (): void {
  if ('ontouchstart' in window) return;
  if (!isBrandV3ShellEnabled()) return;
  const { navId: shellNavId } = readShellSelectors();

  const interval = 200,
    sensitivity = 8;

  // current X and Y position of mouse, updated during mousemove tracking (shared across instances)
  let cX: number, cY: number;

  const track = (ev: MouseEvent) => {
    cX = ev.pageX;
    cY = ev.pageY;
  };

  let state: State = {};

  $(`#${shellNavId}.hover`).each(function (this: HTMLElement) {
    const $el = $(this).removeClass('hover'),
      handler = () => $el.toggleClass('hover');

    // compares current and previous mouse positions
    const compare = () => {
      if (
        Math.sqrt((state.pX! - cX) * (state.pX! - cX) + (state.pY! - cY) * (state.pY! - cY)) < sensitivity
      ) {
        $el.off('mousemove', track);
        delete state.timeoutId;
        state.isActive = true;
        handler();
      } else {
        state.pX = cX;
        state.pY = cY;
        state.timeoutId = setTimeout(compare, interval);
      }
    };

    const handleHover = function (ev: MouseEvent) {
      if (state.timeoutId) state.timeoutId = clearTimeout(state.timeoutId);

      if (ev.type === 'mouseover') {
        if (state.isActive || ev.buttons) return;
        state.pX = ev.pageX;
        state.pY = ev.pageY;
        $el.off('mousemove', track).on('mousemove', track);
        state.timeoutId = setTimeout(compare, interval);
      } else {
        if (!state.isActive) return;
        $el.off('mousemove', track);
        state = {};
        handler();
      }
    };

    $el.on('mouseover', handleHover).on('mouseleave', handleHover);
  });
}
