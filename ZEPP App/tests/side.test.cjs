const {test}=require('node:test')
const assert=require('node:assert/strict')
const vm=require('node:vm')
const esbuild=require('esbuild')
const {readFileSync}=require('node:fs')
const protocol=import('data:text/javascript;base64,'+readFileSync('utils/protocol.js').toString('base64'))
async function setup() {
  const p=await protocol
  const state={protocolVersion:1,sequence:1,servedAt:1000,sessionId:'s',instructionId:'i',state:'navigating',source:'waze',
    destination:'Home',street:'Test',turnDistanceText:'100 m',remainingText:'10 min',maneuver:'LEFT',theme:{...p.DEFAULT_THEME},
    screenMode:'always_on',vibrationEnabled:true,vibrationMap:Object.fromEntries(p.MANEUVERS.map(m=>[m,p.defaultPreset(m)]))}
  let service; const clock={now:1000}; const requests=[]; const http=[]
  const settings={code:'MN1:37843:'+'a'.repeat(32)}
  const network={fetch:async()=>({status:200,body:state})}
  const js=esbuild.buildSync({entryPoints:['app-side/index.js'],bundle:true,write:false,format:'cjs',platform:'node',external:['@zeppos/*']}).outputFiles[0].text
  vm.runInNewContext(js, {require:()=>({BaseSideService:s=>s}),AppSideService:s=>service=s,
    settings:{settingsStorage:{getItem:()=>settings.code}},Date:{now:()=>clock.now},setInterval:()=>1,clearInterval:()=>{},
    fetch:async opts=>{http.push(opts);return network.fetch()},exports:{},module:{exports:{}}})
  Object.assign(service,{stopped:false,busy:false,lastSent:'',lastAckAt:0,request:async message=>{requests.push(message);return {instructionId:'i'}}})
  return {service,state,clock,settings,network,requests,http}
}
test('phone side forwards changes and five-second heartbeat with authenticated localhost only',async()=>{
  const s=await setup(); await s.service.poll()
  assert.equal(s.requests.length,1)
  assert.equal(s.http[0].url,'http://127.0.0.1:37843/v1/state')
  assert.equal(s.http[0].headers.Authorization,'Bearer '+'a'.repeat(32))
  s.clock.now=2000;await s.service.poll();assert.equal(s.requests.length,1)
  s.clock.now=6000;await s.service.poll();assert.equal(s.requests.length,2)
  s.state.sequence++;await s.service.poll();assert.equal(s.requests.length,3)
  s.settings.code='https://example.com';await s.service.poll();assert.equal(s.http.length,4)
})
test('phone side caps concurrent polls, discards response after key change, retries failed ACK',async()=>{
  const s=await setup();let resolve
  s.network.fetch=()=>new Promise(r=>resolve=r)
  const pending=s.service.poll();await s.service.poll();assert.equal(s.http.length,1)
  s.settings.code='MN1:37843:'+'b'.repeat(32);resolve({status:200,body:s.state});await pending
  assert.equal(s.requests.length,0);assert.equal(s.service.busy,false)
  s.network.fetch=async()=>({status:200,body:JSON.stringify(s.state)})
  let attempts=0;s.service.request=async()=>{if(++attempts===1)throw Error('disconnected');return {}}
  await s.service.poll();await s.service.poll();assert.equal(attempts,2)
})
test('phone side rejects oversized and malformed responses and stays stopped after destruction',async()=>{
  const s=await setup()
  s.network.fetch=async()=>({status:200,body:{...s.state,extra:'x'.repeat(17000)}})
  await s.service.poll();assert.equal(s.requests.length,0)
  s.network.fetch=async()=>({status:200,body:'not JSON'});await s.service.poll();assert.equal(s.service.busy,false)
  s.service.onDestroy();await s.service.poll();assert.equal(s.http.length,2)
})
