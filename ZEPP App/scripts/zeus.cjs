// Zeus 1.9's module-alias/register resolves the consumer package when locally installed.
// Initialize its bundled aliases explicitly; no global CLI install is needed.
const path = require('node:path')
const root = path.dirname(require.resolve('@zeppos/zeus-cli/package.json'))
require('module-alias')({ base: root })
// Pinned compiler: do not block a build on an unrelated npm version check.
require(path.join(root, 'utils/pre-check.js')).checkVersion = async () => {}
require(path.join(root, 'private-modules/zeppos-app-utils/dist/hm-analytics.js')).HM_Analytics = () => {}
require(path.join(root, 'bin/main.js'))
