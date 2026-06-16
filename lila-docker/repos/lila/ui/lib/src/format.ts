// for many users, using the islamic calendar is not practical on the internet
// due to international context, so we make sure it's displayed using the gregorian calendar
export const displayLocale: string = (document.documentElement.lang && document.documentElement.lang.startsWith('ar-'))
  ? 'ar-ly'
  : (document.documentElement.lang || 'en-US');

const commonDateFormatter = new Intl.DateTimeFormat(displayLocale, {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
});

export const commonDateFormat: (d?: Date | number) => string = commonDateFormatter.format;

// format Date / string / timestamp to Date instance.
export const toDate = (input: DateLike): Date =>
  input instanceof Date ? input : new Date(isNaN(input as any) ? input : parseInt(input as any));

const timeagoStrings = {
  rightNow: 'now',
  justNow: 'now',
  nbYearsAgo: (n: number) => `${n}y ago`,
  inNbYears: (n: number) => `in ${n}y`,
  nbMonthsAgo: (n: number) => `${n}mo ago`,
  inNbMonths: (n: number) => `in ${n}mo`,
  nbWeeksAgo: (n: number) => `${n}w ago`,
  inNbWeeks: (n: number) => `in ${n}w`,
  nbDaysAgo: (n: number) => `${n}d ago`,
  inNbDays: (n: number) => `in ${n}d`,
  nbHoursAgo: (n: number) => `${n}h ago`,
  inNbHours: (n: number) => `in ${n}h`,
  nbMinutesAgo: (n: number) => `${n}m ago`,
  inNbMinutes: (n: number) => `in ${n}m`,
  inNbSeconds: (n: number) => `in ${n}s`,
};

// format the diff second to *** time ago
export const formatAgo = (seconds: number): string => {
  const absSeconds = Math.abs(seconds);
  const strIndex = seconds < 0 ? 1 : 0;
  const unit = agoUnits.find(unit => absSeconds >= unit[2] * unit[3] && unit[strIndex])!;
  const key = unit[strIndex]!;
  const fmt = timeagoStrings[key as keyof typeof timeagoStrings];
  return typeof fmt === 'string' ? fmt : fmt(Math.floor(absSeconds / unit[2]));
};

type DateLike = Date | number | string;

type TimeagoKey = keyof typeof timeagoStrings;

// past, future, divisor, at least
const agoUnits: [TimeagoKey | undefined, TimeagoKey, number, number][] = [
  ['nbYearsAgo', 'inNbYears', 60 * 60 * 24 * 365, 1],
  ['nbMonthsAgo', 'inNbMonths', (60 * 60 * 24 * 365) / 12, 1],
  ['nbWeeksAgo', 'inNbWeeks', 60 * 60 * 24 * 7, 1],
  ['nbDaysAgo', 'inNbDays', 60 * 60 * 24, 2],
  ['nbHoursAgo', 'inNbHours', 60 * 60, 1],
  ['nbMinutesAgo', 'inNbMinutes', 60, 1],
  [undefined, 'inNbSeconds', 1, 9],
  ['rightNow', 'justNow', 1, 0],
];

let numberFormatter: false | Intl.NumberFormat | null = false;

const getNumberFormatter = (): Intl.NumberFormat | null => {
  if (numberFormatter === false)
    numberFormatter = window.Intl && Intl.NumberFormat ? new Intl.NumberFormat(displayLocale) : null;
  return numberFormatter;
};

export const numberFormat = (n: number): string => {
  const nf = getNumberFormatter();
  return nf ? nf.format(n) : '' + n;
};
