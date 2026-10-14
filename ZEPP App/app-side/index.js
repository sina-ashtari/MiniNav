import { BaseSideService } from '@zeppos/zml/base-side'
import { bridgeCode, validateState } from '../utils/protocol'

AppSideService(BaseSideService({
  onInit() {
    this.stopped = false; this.busy = false; this.lastSent = ''; this.lastAckAt = 0
    this.timer = setInterval(() => this.poll(), 1000)
    this.poll()
  },
  onCall(message) { if (message.method === 'hello') { this.lastSent = ''; this.poll() } },
  async poll() {
    if (this.busy || this.stopped) return
    const code = settings.settingsStorage.getItem('bridgeCode') || ''
    const credentials = bridgeCode(code)
    if (!credentials) return
    this.busy = true
    try {
      const response = await fetch({ url: `http://127.0.0.1:${credentials.port}/v1/state`, method: 'GET',
        headers: { Authorization: `Bearer ${credentials.token}` } })
      if (this.stopped || response.status !== 200 || settings.settingsStorage.getItem('bridgeCode') !== code) return
      if (JSON.stringify(response.body).length > 16384) return
      const state = validateState(typeof response.body === 'string' ? JSON.parse(response.body) : response.body)
      if (!state) return
      const signature = `${state.sessionId}:${state.sequence}`
      if (signature !== this.lastSent || Date.now() - this.lastAckAt >= 5000) {
        // MessageBuilder request/response is an acknowledgement; retry only until accepted.
        await this.request({ method: 'state', params: state }, { timeout: 3000 })
        this.lastSent = signature; this.lastAckAt = Date.now()
      }
    } catch (_) { /* No URLs, tokens or navigation payloads in logs. Watch retains a stale indication. */ }
    finally { this.busy = false }
  },
  onDestroy() { this.stopped = true; clearInterval(this.timer) }
}))
