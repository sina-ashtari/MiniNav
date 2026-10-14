import { Vibrator, VIBRATOR_SCENE_SHORT_LIGHT, VIBRATOR_SCENE_SHORT_STRONG,
  VIBRATOR_SCENE_NOTIFICATION, VIBRATOR_SCENE_DURATION, VIBRATOR_SCENE_DURATION_LONG,
  VIBRATOR_SCENE_STRONG_REMINDER, Time } from '@zos/sensor'
import { exit } from '@zos/app-service'

const scenes = { SHORT_LIGHT: VIBRATOR_SCENE_SHORT_LIGHT, SHORT_STRONG: VIBRATOR_SCENE_SHORT_STRONG,
  TWO_SHORT: VIBRATOR_SCENE_NOTIFICATION, LONG: VIBRATOR_SCENE_DURATION,
  EXTRA_LONG: VIBRATOR_SCENE_DURATION_LONG, STRONG_REMINDER: VIBRATOR_SCENE_STRONG_REMINDER }
AppService({
  onInit() {
    const app = getApp()
    const data = app._options.globalData
    const call = message => app._options.call.call(app, message)
    this.vibrator = new Vibrator()
    this.time = new Time()
    data.serviceReady = true; data.serviceError = ''
    this.startedAt = Date.now()
    this.maintenance = now => {
      call({ method: 'hello' }).catch(() => {})
      const state = data.nav.latest
      if (state?.state === 'idle' || state?.state === 'arrived') {
        if (!this.idleSince) this.idleSince = now
        if (now - this.idleSince > 120000) exit()
      } else this.idleSince = 0
      // A dead phone bridge must not leave a background service running forever.
      if (now - (data.nav.receivedAt || this.startedAt) > 300000) exit()
    }
    data.receive = s => {
      const accepted = data.nav.accept(s, Date.now(), preset => {
      if (scenes[preset] !== undefined) this.vibrator.start({ mode: scenes[preset] })
    })
      this.maintenance(Date.now())
      return accepted
    }
    // Time is available at API 2.1 and continues to run in an App Service.
    // The phone-side heartbeat provides the five-second connected maintenance path.
    this.time.onPerMinute(() => this.maintenance(Date.now()))
    this.maintenance(Date.now())
  },
  onDestroy() {
    this.vibrator.stop()
    getApp()._options.globalData.receive = null; getApp()._options.globalData.serviceReady = false
  }
})
