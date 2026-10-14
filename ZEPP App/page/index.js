import * as ui from '@zos/ui'
import { getDeviceInfo } from '@zos/device'
import { queryPermission, requestPermission } from '@zos/app'
import { start } from '@zos/app-service'
import { Battery } from '@zos/sensor'
import { setPageBrightTime, resetPageBrightTime, pauseDropWristScreenOff, resetDropWristScreenOff, setWakeUpRelaunch } from '@zos/display'
import { DEFAULT_THEME } from '../utils/protocol'
import { arrowLines } from '../utils/arrow'

const BRIGHT_TIME = 2147483000
const LOW_BATTERY = 15
const color = value => parseInt(value.slice(1), 16)

Page({
  build() {
    ui.setStatusBarVisible(false)
    const { width, height } = getDeviceInfo()
    this.width = width; this.height = height; this.d = Math.min(width, height)
    this.widgets = []; this.signature = ''; this.lit = false; this.starting = false
    this.battery = new Battery(); this.lowBattery = this.battery.getCurrent() < LOW_BATTERY
    this.onBattery = () => { if (this.battery.getCurrent() < LOW_BATTERY) this.lowBattery = true; this.render() }
    this.battery.onChange(this.onBattery)
    this.boot = () => {
      if (getApp()._options.globalData.serviceReady || this.starting) return
      this.starting = true
      start({ file: 'app-service/index', complete_func: result => {
        this.starting = false
        if (!result.result) getApp()._options.globalData.serviceError = 'Service unavailable'
      } })
    }
    if (queryPermission({ permissions: ['device:os.bg_service'] })[0] === 2) this.boot()
    else requestPermission({ permissions: ['device:os.bg_service'], callback: result => {
      if (result[0] === 2) this.boot()
      else getApp()._options.globalData.serviceError = 'Allow background service'
    } })
    this.render(); this.timer = setInterval(() => this.render(), 500)
  },
  add(type, props) { const item = ui.createWidget(type, props); this.widgets.push(item); return item },
  chord(y, h, requested) {
    const r = this.d / 2 - Math.max(4, this.d * 0.01); const cy = this.height / 2
    const dy = Math.max(Math.abs(y - cy), Math.abs(y + h - cy))
    return Math.max(0, Math.min(requested, 2 * Math.sqrt(Math.max(0, r * r - dy * dy))))
  },
  text(value, y, h, size, key, style = ui.text_style.ELLIPSIS) {
    const w = this.chord(y, h, this.d * 0.92); const x = (this.width - w) / 2
    this.add(ui.widget.TEXT, { x, y, w, h, text: value || '', text_size: size, color: color(this.theme[key]),
      align_h: /[\u0600-\u06ff]/.test(value || '') ? ui.align.RIGHT : ui.align.CENTER_H,
      align_v: ui.align.CENTER_V, text_style: style, line_space: Math.max(2, this.d * 0.012) })
  },
  setDisplayLock(shouldLock) {
    if (shouldLock === this.lit) return
    this.lit = shouldLock
    if (shouldLock) {
      setPageBrightTime({ brightTime: BRIGHT_TIME })
      pauseDropWristScreenOff({ duration: 0 })
    } else { resetPageBrightTime(); resetDropWristScreenOff() }
  },
  render() {
    const data = getApp()._options.globalData; const nav = data.nav; const s = nav.latest
    if (!data.serviceReady && queryPermission({ permissions: ['device:os.bg_service'] })[0] === 2) this.boot()
    const stale = nav.disconnected(Date.now())
    const active = !!(s && ['navigating', 'rerouting'].includes(s.state))
    setWakeUpRelaunch({ relaunch: active })
    this.setDisplayLock(active && !stale && !this.lowBattery)
    const signature = `${s?.sessionId}:${s?.sequence}:${stale}:${active}:${this.lowBattery}:${data.serviceError}:${data.serviceReady}`
    if (signature === this.signature) return
    this.signature = signature; this.widgets.forEach(w => ui.deleteWidget(w)); this.widgets = []
    this.theme = s?.theme || DEFAULT_THEME
    this.add(ui.widget.FILL_RECT, { x: 0, y: 0, w: this.width, h: this.height, color: color(this.theme.background) })
    const D = this.d; const idle = !s || s.state === 'idle'
    if (idle) {
      this.text('MiniNav', D * 0.27, D * 0.12, Math.max(24, D * 0.08), 'destination')
      this.text('Start navigation on your phone', D * 0.48, D * 0.11, Math.max(20, D * 0.055), 'street')
      this.text(data.serviceError || (!data.serviceReady ? 'Reconnecting' : 'Ready'), D * 0.66, D * 0.07, Math.max(18, D * 0.045), 'duration')
      return
    }
    const warning = stale ? 'PHONE DISCONNECTED' : this.lowBattery ? 'LOW BATTERY — SCREEN TIMEOUT ON' : ''
    if (warning) this.text(warning, D * 0.04, D * 0.07, Math.max(18, D * 0.048), 'destination')
    const distanceY = warning ? D * 0.12 : D * 0.07
    this.text(s.turnDistanceText || '', distanceY, D * 0.13, Math.max(30, D * 0.12), 'distance')
    let arrowW = D * 0.64; const arrowY = D * 0.23; let arrowH = arrowW * 0.8
    arrowW = Math.min(arrowW, this.chord(arrowY, arrowH, arrowW)); arrowW = Math.max(D * 0.56, arrowW); arrowH = arrowW * 0.8
    const lines = arrowLines(s.maneuver)
    if (lines.length) {
      const x = (this.width - arrowW) / 2; const canvas = this.add(ui.widget.CANVAS, { x, y: arrowY, w: arrowW, h: arrowH })
      canvas.setPaint({ color: color(this.theme.arrow), line_width: D * 0.045 })
      lines.forEach(points => points.slice(1).forEach(([x2, y2], i) => canvas.drawLine({
        x1: points[i][0] * arrowW / 200, y1: points[i][1] * arrowH / 160,
        x2: x2 * arrowW / 200, y2: y2 * arrowH / 160
      })))
    } else this.text(s.maneuver === 'ARRIVAL' ? 'Arrived' : s.maneuver === 'REROUTING' ? 'Rerouting' : '?', arrowY, arrowH, D * 0.14, 'arrow')
    this.text(s.street || '', D * 0.72, D * 0.14, Math.max(24, D * 0.06), 'street', ui.text_style.WRAP)
    if (this.chord(D * 0.88, D * 0.07, D * 0.7) >= D * 0.38) this.text(s.remainingText || '', D * 0.88, D * 0.07, Math.max(20, D * 0.05), 'duration')
  },
  onDestroy() {
    clearInterval(this.timer); this.battery.offChange(this.onBattery)
    resetPageBrightTime(); resetDropWristScreenOff()
  }
})
