const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

test('shared package stays within the audited API 3.5 surface', () => {
  const root = path.resolve(__dirname, '..')
  const app = JSON.parse(fs.readFileSync(path.join(root, 'app.json')))
  assert.deepEqual(app.runtime.apiVersion, { compatible: '3.5', target: '3.5', minVersion: '3.5' })
  assert.deepEqual(app.targets.activeedge.platforms.map(p => p.deviceSource), [8388864, 8388865])
  const source = ['page/index.js', 'app-service/index.js', 'app-side/index.js'].map(file => fs.readFileSync(path.join(root, file), 'utf8')).join('\n')
  for (const unsupported of ['@zos/timer', 'createSysTimer', 'stopTimer', 'reload:']) assert.equal(source.includes(unsupported), false, unsupported)
  assert.match(source, /Time/)
})
