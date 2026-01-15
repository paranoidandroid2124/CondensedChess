import { spawnSync } from 'node:child_process';
import { readFileSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import ps from 'node:process';

const __dirname = dirname(fileURLToPath(import.meta.url));
const rootDir = join(__dirname, '..');
const dotBuildDir = join(__dirname, '.build');

// 1. Verify Node.js version
const minNodePath = join(rootDir, '.node-version');
if (existsSync(minNodePath)) {
    const minNode = readFileSync(minNodePath, 'utf8').trim().replace(/^v/, '');
    const curNode = ps.version.replace(/^v/, '');

    const compareVersions = (v1, v2) => {
        const parts1 = v1.split('.').map(Number);
        const parts2 = v2.split('.').map(Number);
        for (let i = 0; i < 3; i++) {
            if (parts1[i] > (parts2[i] || 0)) return 1;
            if (parts1[i] < (parts2[i] || 0)) return -1;
        }
        return 0;
    };

    if (compareVersions(minNode, curNode) > 0) {
        console.error(`Nodejs v${minNode} or later is required. Current: ${ps.version}`);
        ps.exit(1);
    }
}

// 2. Prepare arguments
const args = ps.argv.slice(2);
const isOneDash = arg => /^-([a-z]+)$/.test(arg);
const noInstall = args.some(arg => arg === '--no-install' || (isOneDash(arg) && arg.includes('n')));

// 3. Run pnpm install if needed
if (!noInstall) {
    console.log('Running pnpm install in ui/.build...');
    const installResult = spawnSync('pnpm', ['install', '--ignore-workspace'], {
        cwd: dotBuildDir,
        stdio: 'inherit',
        shell: true
    });

    if (installResult.status !== 0) {
        console.error('pnpm install failed');
        ps.exit(installResult.status ?? 1);
    }
}

// 4. Run the build script
console.log('Starting UI build...');
const buildResult = spawnSync('node', [
    '--experimental-strip-types',
    '--no-warnings',
    'src/main.ts',
    ...args
], {
    cwd: dotBuildDir,
    stdio: 'inherit',
    shell: true
});

ps.exit(buildResult.status ?? 0);
