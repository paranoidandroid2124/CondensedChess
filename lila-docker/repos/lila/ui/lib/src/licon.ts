/*
 * Licon constants map to semantic SVG icon identifiers.
 * Legacy [data-icon] paths still require glyph code points and are translated by toDataIcon().
 */

export const CautionTriangle = 'cautionTriangle';
export const Gear = 'gear';
export const Microscope = 'microscope';
export const ChasingArrows = 'chasingArrows';
export const Target = 'target';
export const Checkmark = 'checkmark';
export const InternalArrow = 'internalArrow';
export const PlayTriangle = 'playTriangle';
export const GreaterThan = 'greaterThan';
export const LessThan = 'lessThan';
export const X = 'x';
export const PlusButton = 'plusButton';
export const MinusButton = 'minusButton';
export const UpTriangle = 'upTriangle';
export const JumpLast = 'jumpLast';
export const JumpFirst = 'jumpFirst';
export const Hamburger = 'hamburger';
export const Book = 'book';
export const BubbleSpeech = 'bubbleSpeech';
export const Back = 'back';
export const CautionCircle = 'cautionCircle';
export const Cogs = 'cogs';
export const Trash = 'trash';
export const InfoCircle = 'infoCircle';
export const Clipboard = 'clipboard';
export const Reload = 'reload';

const glyphByName: Record<string, string> = {
  cautionTriangle: '\uE000',
  gear: '\uE005',
  microscope: '\uE01F',
  chasingArrows: '\uE020',
  target: '\uE015',
  checkmark: '\uE023',
  internalArrow: '\uE024',
  playTriangle: '\uE025',
  greaterThan: '\uE026',
  lessThan: '\uE027',
  x: '\uE02A',
  plusButton: '\uE02D',
  minusButton: '\uE02E',
  upTriangle: '\uE031',
  jumpLast: '\uE034',
  jumpFirst: '\uE035',
  hamburger: '\uE039',
  book: '\uE03B',
  bubbleSpeech: '\uE041',
  back: '\uE047',
  cautionCircle: '\uE048',
  cogs: '\uE04C',
  trash: '\uE04F',
  infoCircle: '\uE060',
  clipboard: '\uE070',
  reload: '\uE078',
};

export const toDataIcon = (icon: string): string => glyphByName[icon] ?? icon;
