package app.mininav

import android.app.*
import android.content.Intent
import android.os.*

class BridgeService : Service() {
    private var server: BridgeHttp? = null
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            AppState.tick(System.currentTimeMillis())
            if(AppState.idleSince > 0 && System.currentTimeMillis()-AppState.idleSince > 120000) stopSelf()
            else handler.postDelayed(this,1000)
        }
    }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppState.init(this)
        if(intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if(server != null) return START_NOT_STICKY
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("bridge","Navigation connection",NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this,1,Intent(this,BridgeService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        startForeground(1,Notification.Builder(this,"bridge").setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("MiniNav is ready").setContentText("Navigation stays on your devices").setContentIntent(open)
            .setOngoing(true).addAction(Notification.Action.Builder(null,"Stop",stop).build()).build())
        try {
            server = BridgeHttp(AppState.token, { AppState.json() }, { handler.post { AppState.lastRead = System.currentTimeMillis() } })
            try { server!!.start(AppState.port) } catch (_: java.net.BindException) { server!!.start(0); AppState.port = server!!.port }
            AppState.running = true; AppState.status = "Bridge running"; AppState.idleSince = System.currentTimeMillis()
            NavigationListener.instance?.refresh(); handler.post(tick)
        } catch (_: Exception) { AppState.status = "Could not start local bridge"; stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); server?.close(); server = null
        AppState.running = false; AppState.clear(); AppState.status = "Stopped"; AppState.lastRead = 0
        super.onDestroy()
    }
}
