import { BaseApp } from '@zeppos/zml/base-app'
import { WatchState } from './utils/protocol'

App(BaseApp({
  globalData: { nav: new WatchState(), serviceReady: false, serviceError: '', receive: null },
  onRequest(message, reply) {
    if (message.method !== 'state' || !this.globalData.receive) { reply('not_ready'); return }
    const accepted = this.globalData.receive(message.params)
    reply(accepted ? null : 'invalid_state', accepted ? {
      instructionId: this.globalData.nav.lastInstruction,
      previewNonce: this.globalData.nav.previews.slice(-1)[0] || null
    } : null)
  }
}))
