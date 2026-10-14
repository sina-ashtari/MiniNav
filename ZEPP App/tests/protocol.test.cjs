const { test } = require('node:test')
const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const modulePromise = import('data:text/javascript;base64,' + readFileSync('utils/protocol.js').toString('base64'))
async function fixture() {
  const p = await modulePromise
  return { p, state: { protocolVersion: 1, sequence: 1, servedAt: 1000, sessionId: 'a', instructionId: 'a:1',
    state: 'navigating', source: 'maps', destination: 'Home', street: 'Main', turnDistanceText: '100 m', remainingText: '10 min',
    maneuver: 'LEFT', theme: {...p.DEFAULT_THEME}, screenMode: 'always_on', vibrationEnabled: true,
    vibrationMap: Object.fromEntries(p.MANEUVERS.map(m => [m,p.defaultPreset(m)])), vibrationPreview: null } }
}
test('strict local-only bridge code', async () => {
  const {bridgeCode}=await modulePromise; const token='a'.repeat(32)
  assert.deepEqual(bridgeCode(`MN1:37843:${token}`),{port:37843,token})
  for(const x of [`MN1:80:${token}`,`MN1:65536:${token}`,`MN1:37843:short`,`http://host/${token}`,`MN1:37843:${token}\r\nInjected`]) assert.equal(bridgeCode(x),null)
})
test('all maneuvers have valid defaults; invalid payloads rejected', async () => {
  const {p,state}=await fixture(); assert.equal(p.MANEUVERS.length,19)
  assert.equal(p.validateState(state),state)
  p.MANEUVERS.forEach(m => assert.ok(p.PRESETS.includes(p.defaultPreset(m))))
  assert.equal(p.validateState({...state,protocolVersion:2}),null)
  assert.equal(p.validateState({...state,maneuver:'MAYBE'}),null)
  assert.equal(p.validateState({...state,street:'x'.repeat(257)}),null)
  assert.equal(p.validateState({...state,theme:{}}),null)
})
test('first instruction vibrates; time, distance, theme, heartbeat and reconnect do not', async () => {
  const {p,state}=await fixture(); const model=new p.WatchState(); const plays=[]
  model.accept(state,1000,x=>plays.push(x)); assert.deepEqual(plays,['LONG'])
  model.accept({...state,sequence:2,remainingText:'9 min',turnDistanceText:'80 m'},2000,x=>plays.push(x))
  model.accept({...state,sequence:3,theme:{...state.theme,arrow:'#FFFFFF'}},3000,x=>plays.push(x))
  model.accept({...state,sequence:3},30000,x=>plays.push(x)); assert.equal(plays.length,1)
  model.accept({...state,sequence:4,instructionId:'a:2'},31000,x=>plays.push(x)); assert.equal(plays.length,2)
  model.accept({...state,sessionId:'b',instructionId:'b:1',sequence:0},32000,x=>plays.push(x)); assert.equal(plays.length,3)
})
test('mute acknowledges instructions; preview nonce is one-shot and expires', async () => {
  const {p,state}=await fixture(); const model=new p.WatchState(); const plays=[]; const play=x=>plays.push(x)
  model.accept({...state,vibrationEnabled:false},1000,play)
  model.accept({...state,sequence:2},2000,play); assert.equal(plays.length,0)
  const preview={nonce:'n1',preset:'TWO_SHORT',expiresAt:10000}
  model.accept({...state,sequence:3,vibrationPreview:preview},3000,play)
  model.accept({...state,sequence:4,vibrationPreview:preview},4000,play); assert.deepEqual(plays,['TWO_SHORT'])
  model.accept({...state,sequence:5,vibrationPreview:{...preview,nonce:'n2',expiresAt:500}},5000,play)
  assert.equal(plays.length,1); assert.equal(model.lastInstruction,'a:1')
})
test('stale state remains cached; old sequence ignored', async () => {
  const {p,state}=await fixture(); const model=new p.WatchState()
  model.accept(state,1000,()=>{}); assert.equal(model.disconnected(14000),true)
  assert.equal(model.latest.street,'Main')
  assert.equal(model.accept({...state,sequence:0},14000,()=>{}),false)
})
test('every maneuver can use every preset without changing instruction on configuration updates', async () => {
  const {p,state}=await fixture()
  for(const maneuver of p.MANEUVERS) for(const preset of p.PRESETS) {
    const model=new p.WatchState();const plays=[]
    const next={...state,maneuver,vibrationMap:{...state.vibrationMap,[maneuver]:preset}}
    model.accept(next,1000,x=>plays.push(x));assert.deepEqual(plays,[preset])
    model.accept({...next,sequence:2,vibrationMap:state.vibrationMap},2000,x=>plays.push(x));assert.equal(plays.length,1)
  }
})
