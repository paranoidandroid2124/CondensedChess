import { env } from '../ui/.build/src/env.ts';
import { parsePackages } from '../ui/.build/src/parse.ts';
import { sync } from '../ui/.build/src/sync.ts';
import { hash } from '../ui/.build/src/hash.ts';
import { sass } from '../ui/.build/src/sass.ts';
import { esbuild } from '../ui/.build/src/esbuild.ts';
import { definedUnique } from '../ui/.build/src/algo.ts';

const requested = ['site', 'serviceWorker', 'editor', 'analyse'];

env.prod = true;
env.install = false;
env.startTime = Date.now();

await parsePackages();

requested
  .filter(name => !env.packages.has(name))
  .forEach(name => env.exit(`Unknown package '${name}'`, 'build'));

env.building = definedUnique(requested.flatMap(name => env.deps(name)));

await sync();
await hash();
await Promise.all([sass(), esbuild()]);

env.begin('tsc', true);
env.done('tsc', 0);
