import * as licon from '../licon';

const perfIcons: Partial<Record<Exclude<Perf, 'fromPosition'>, string>> = {
  ultraBullet: licon.UltraBullet,
  bullet: licon.Bullet,
  blitz: licon.FlameBlitz,
  rapid: licon.Rabbit,
  classical: licon.Turtle,
  correspondence: licon.PaperAirplane,
  chess960: licon.DieSix,
};

export default perfIcons;
