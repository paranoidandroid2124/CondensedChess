import fg from 'fast-glob';
import { join } from 'node:path';

async function test() {
    const rootDir = process.cwd();
    const uiDir = join(rootDir, 'ui');
    const pattern = '*/css/**/*.scss';

    console.log('rootDir:', rootDir);
    console.log('uiDir:', uiDir);
    console.log('pattern:', pattern);

    const files = await fg(pattern, { cwd: uiDir, absolute: true });
    console.log('Found files with backslashes in cwd:', files.length);

    const uiDirForward = uiDir.replace(/\\/g, '/');
    console.log('uiDirForward:', uiDirForward);
    const files2 = await fg(pattern, { cwd: uiDirForward, absolute: true });
    console.log('Found files with forward slashes in cwd:', files2.length);

    if (files2.length > 0) {
        console.log('Sample:', files2.slice(0, 5));
    }
}

test();
