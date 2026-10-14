export const MANEUVERS = ['STRAIGHT', 'SLIGHT_LEFT', 'LEFT', 'SHARP_LEFT', 'KEEP_LEFT',
  'SLIGHT_RIGHT', 'RIGHT', 'SHARP_RIGHT', 'KEEP_RIGHT', 'UTURN_LEFT', 'UTURN_RIGHT',
  'ROUNDABOUT_LEFT', 'ROUNDABOUT_RIGHT', 'EXIT_LEFT', 'EXIT_RIGHT', 'MERGE', 'ARRIVAL', 'REROUTING', 'UNKNOWN']
export const PRESETS = ['NONE', 'SHORT_LIGHT', 'SHORT_STRONG', 'TWO_SHORT', 'LONG', 'EXTRA_LONG', 'STRONG_REMINDER']
export const DEFAULT_THEME = { background: '#000000', destination: '#B8C4D6', arrow: '#72E8B0', distance: '#FFFFFF', street: '#FFFFFF', duration: '#72E8B0' }
export function defaultPreset(m) {
  if (m.startsWith('UTURN') || m === 'ARRIVAL') return 'STRONG_REMINDER'
  if (m.endsWith('LEFT')) return 'LONG'
  if (m.endsWith('RIGHT')) return 'TWO_SHORT'
  if (m === 'STRAIGHT') return 'SHORT_LIGHT'
  return m === 'MERGE' || m === 'REROUTING' ? 'SHORT_STRONG' : 'NONE'
}
export function bridgeCode(text) {
  const match = /^MN1:([0-9]{1,5}):([0-9a-f]{32})$/.exec(String(text).trim())
  if (!match || +match[1] < 1024 || +match[1] > 65535) return null
  return { port: +match[1], token: match[2] }
}
// Treat even the authenticated local endpoint as untrusted input. Never accept a URL from settings.
export function validateState(s) {
  if (!s || s.protocolVersion !== 1 || !Number.isSafeInteger(s.sequence) || !Number.isFinite(s.servedAt)) return null
  if (!['idle', 'navigating', 'rerouting', 'arrived', 'error'].includes(s.state)) return null
  if (![null, 'maps', 'waze'].includes(s.source) || !MANEUVERS.includes(s.maneuver)) return null
  for (const field of ['sessionId', 'instructionId', 'destination', 'street', 'turnDistanceText', 'remainingText']) {
    if (typeof s[field] !== 'string' || s[field].length > 256) return null
  }
  if (!['always_on', 'battery_saver'].includes(s.screenMode) || typeof s.vibrationEnabled !== 'boolean') return null
  for (const key of Object.keys(DEFAULT_THEME)) if (!/^#[0-9a-fA-F]{6}$/.test(s.theme?.[key] || '')) return null
  for (const m of MANEUVERS) if (!PRESETS.includes(s.vibrationMap?.[m])) return null
  const p = s.vibrationPreview
  if (p && (typeof p.nonce !== 'string' || !p.nonce.length || p.nonce.length > 80 || !PRESETS.includes(p.preset) || !Number.isFinite(p.expiresAt))) return null
  return s
}
// RAM only: survives page closure/reopening and BLE reconnects while AppService lives.
export class WatchState {
  constructor() { this.latest = null; this.receivedAt = 0; this.lastInstruction = ''; this.previews = [] }
  accept(s, now, play) {
    if (!validateState(s)) return false
    // Ignore out-of-order messages in one Android process/session, including heartbeat duplicates.
    if (this.latest && s.sessionId === this.latest.sessionId && s.sequence < this.latest.sequence) return false
    this.latest = s; this.receivedAt = now
    if (s.instructionId && s.instructionId !== this.lastInstruction) {
      this.lastInstruction = s.instructionId // muted instructions are acknowledged too
      if (s.vibrationEnabled && ['navigating', 'rerouting', 'arrived'].includes(s.state)) play(s.vibrationMap[s.maneuver])
    }
    const p = s.vibrationPreview
    // Compare phone timestamps with one another: phone/watch clocks need not agree.
    if (p && p.expiresAt > s.servedAt && !this.previews.includes(p.nonce)) {
      this.previews.push(p.nonce); this.previews = this.previews.slice(-32)
      if (s.vibrationEnabled) play(p.preset)
    }
    return true
  }
  disconnected(now) { return !this.receivedAt || now - this.receivedAt > 12000 }
}
