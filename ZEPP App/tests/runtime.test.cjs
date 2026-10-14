const { test } = require('node:test')
const assert = require('node:assert/strict')
const vm = require('node:vm')
const esbuild = require('esbuild')

function load(file, globals, modules) {
  const result=esbuild.buildSync({entryPoints:[file],bundle:true,write:false,platform:'node',format:'cjs',external:['@zos/*','@zeppos/*']})
  const context={...globals,require:name=>{ if(!(name in modules)) throw new Error(`Unexpected import ${name}`); return modules[name] },module:{exports:{}},exports:{}}
  vm.runInNewContext(result.outputFiles[0].text,context)
}
test('service owns vibration; page recreate and reconnect do not replay; cleanup stops motor', () => {
  let app,service,page,minute; const vibrations=[]; const stops=[]; let now=1000
  const time={now:()=>now}
  load('app.js',{App:a=>app=a,Date:time},{'@zeppos/zml/base-app':{BaseApp:a=>a}})
  let callContext
  app.call=function(){callContext=this;return Promise.resolve()}
  const runtimeApp={_options:app,messaging:{}}
  const globals={getApp:()=>runtimeApp,Date:time}
  load('app-service/index.js',{...globals,AppService:s=>service=s},{
    '@zos/sensor':{Vibrator:class {start(p){vibrations.push(p.mode)} stop(){stops.push('vibration')}}, Time:class {onPerMinute(cb){minute=cb}},
      VIBRATOR_SCENE_SHORT_LIGHT:1,VIBRATOR_SCENE_SHORT_STRONG:2,VIBRATOR_SCENE_NOTIFICATION:3,
      VIBRATOR_SCENE_DURATION:4,VIBRATOR_SCENE_DURATION_LONG:5,VIBRATOR_SCENE_STRONG_REMINDER:6},
    '@zos/app-service':{exit:()=>stops.push('exit')}
  })
  service.onInit()
  assert.equal(callContext,runtimeApp)
  assert.equal(app.globalData.serviceReady,true)
  const maneuvers=['STRAIGHT','SLIGHT_LEFT','LEFT','SHARP_LEFT','KEEP_LEFT','SLIGHT_RIGHT','RIGHT','SHARP_RIGHT','KEEP_RIGHT','UTURN_LEFT','UTURN_RIGHT','ROUNDABOUT_LEFT','ROUNDABOUT_RIGHT','EXIT_LEFT','EXIT_RIGHT','MERGE','ARRIVAL','REROUTING','UNKNOWN']
  const state={protocolVersion:1,sequence:1,servedAt:1000,sessionId:'a',instructionId:'a1',state:'navigating',source:'maps',
    destination:'Home',street:'خیابان نمونه',turnDistanceText:'100 m',remainingText:'10 min',maneuver:'LEFT',
    theme:Object.fromEntries(['background','destination','arrow','distance','street','duration'].map(k=>[k,'#FFFFFF'])),
    screenMode:'always_on',vibrationEnabled:true,vibrationMap:Object.fromEntries(maneuvers.map(m=>[m,'LONG']))}
  const reply=(error,value)=>{assert.equal(error,null);assert.equal(value.instructionId,'a1')}
  app.onRequest({method:'state',params:state},reply)
  assert.deepEqual(vibrations,[4])
  const screen=[]; const widgets=[]; let serviceStarts=0
  const modules={
    '@zos/device':{getDeviceInfo:()=>({width:466,height:466})},
    '@zos/ui':{widget:{TEXT:1,FILL_RECT:2,CANVAS:3},align:{RIGHT:1,CENTER_H:2,CENTER_V:3},text_style:{WRAP:1,ELLIPSIS:2},
      setStatusBarVisible:()=>{},deleteWidget:()=>{},createWidget:(type,props)=>{widgets.push(props);return {setPaint:()=>{},drawLine:()=>{}}}},
    '@zos/app':{queryPermission:()=>[2],requestPermission:()=>{}},
    '@zos/app-service':{start:()=>{serviceStarts++}},
    '@zos/display':{setPageBrightTime:()=>screen.push('on'),resetPageBrightTime:()=>screen.push('reset'),pauseDropWristScreenOff:()=>{},resetDropWristScreenOff:()=>{},setWakeUpRelaunch:()=>{}},
    '@zos/sensor':{Battery:class {getCurrent(){return 80} onChange(){} offChange(){}}}
  }
  const open=()=>{load('page/index.js',{...globals,Page:p=>page=p,setInterval:()=>1,clearInterval:()=>{}},modules);page.build()}
  open(); assert.ok(screen.includes('on')); page.onDestroy(); open()
  assert.ok(widgets.some(w=>w.w===466 && w.h===466 && w.x===0 && w.y===0))
  assert.ok(widgets.some(w=>w.text==='100 m'))
  page.onDestroy(); widgets.length=0
  modules['@zos/device'].getDeviceInfo=()=>({width:480,height:480})
  open()
  assert.ok(widgets.some(w=>w.w===480 && w.h===480 && w.x===0 && w.y===0))
  assert.ok(widgets.some(w=>w.text==='100 m'))
  app.onRequest({method:'state',params:state},reply); assert.equal(vibrations.length,1)
  now=14000; page.render(); assert.equal(screen.at(-1),'reset')
  assert.ok(widgets.some(w=>w.text==='PHONE DISCONNECTED'))
  now=301001; minute(); assert.ok(stops.includes('exit'))
  service.onDestroy(); assert.equal(app.globalData.serviceReady,false); page.render(); assert.equal(serviceStarts,1)
  assert.ok(widgets.some(w=>w.text==='PHONE DISCONNECTED')); assert.ok(stops.includes('vibration'))
})
test('left/right arrow geometries mirror physically, regardless of text language', async () => {
  const source=require('node:fs').readFileSync('utils/arrow.js')
  const {arrowLines}=await import('data:text/javascript;base64,'+source.toString('base64'))
  for(const family of ['','SLIGHT_','SHARP_','KEEP_','EXIT_','UTURN_','ROUNDABOUT_']) {
    const left=arrowLines(family+'LEFT'); const right=arrowLines(family+'RIGHT')
    assert.deepEqual(right,left.map(line=>line.map(([x,y])=>[200-x,y])))
  }
  assert.deepEqual(arrowLines('UNKNOWN'),[])
})
