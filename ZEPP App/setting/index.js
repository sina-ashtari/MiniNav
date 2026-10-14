import { bridgeCode } from '../utils/protocol'
AppSettingsPage({
  build(props) {
    return View({ style: { padding: '20px' } }, [
      Text({ style: { fontSize: '26px', fontWeight: 'bold' } }, ['MiniNav']),
      Text({}, ['Start the Android bridge, copy its bridge code, and paste it here once. Never share the code. This app only contacts your phone’s localhost address.']),
      TextInput({ label: 'Paste bridge code', placeholder: 'MN1:port:token',
        onChange: value => {
          const code = String(value).trim()
          if (bridgeCode(code)) {
            props.settingsStorage.setItem('bridgeCode', code)
            props.settingsStorage.setItem('setupStatus', 'Bridge saved. Open MiniNav on your watch.')
          } else props.settingsStorage.setItem('setupStatus', 'Invalid code. Copy it again from MiniNav on Android.')
        } }),
      Text({}, [props.settingsStorage.getItem('setupStatus') || 'No bridge configured.']),
      Button({ label: 'Forget bridge', onClick: () => {
        props.settingsStorage.removeItem('bridgeCode')
        props.settingsStorage.setItem('setupStatus', 'Bridge removed.')
      } })
    ])
  }
})
