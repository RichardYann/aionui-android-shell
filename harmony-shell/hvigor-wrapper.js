const { spawnSync } = require('child_process');
const path = require('path');

const root = __dirname;
const args = process.argv.slice(2);

const bin = process.platform === 'win32'
  ? path.join(root, 'node_modules', '.bin', 'hvigor.cmd')
  : path.join(root, 'node_modules', '.bin', 'hvigor');

const result = spawnSync(bin, args, {
  cwd: root,
  stdio: 'inherit',
  env: process.env
});

process.exit(result.status ?? 1);

