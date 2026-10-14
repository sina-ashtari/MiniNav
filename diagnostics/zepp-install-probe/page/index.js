import * as ui from '@zos/ui'

Page({
  build() {
    ui.createWidget(ui.widget.TEXT, {
      x: 83, y: 183, w: 300, h: 100,
      text: 'Install OK', text_size: 36, color: 0xffffff,
      align_h: ui.align.CENTER_H, align_v: ui.align.CENTER_V
    })
  }
})
