package app.mininav

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.graphics.Bitmap
import android.graphics.Canvas

class NavigationListener : NotificationListenerService() {
    override fun onCreate() { super.onCreate(); AppState.init(this) }
    override fun onListenerConnected() { instance = this; if(AppState.running) refresh() }
    override fun onListenerDisconnected() { instance = null; AppState.clear(); AppState.status = "Notification access disconnected" }
    fun refresh() { activeNotifications?.forEach(::onNotificationPosted) }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!AppState.running) return
        val source = when(sbn.packageName) { "com.google.android.apps.maps" -> "maps"; "com.waze" -> "waze"; else -> return }
        val n = sbn.notification
        if (n.flags and Notification.FLAG_ONGOING_EVENT == 0) { AppState.removed(sbn.key); return }
        val e = n.extras
        fun text(key: String) = e.getCharSequence(key)?.toString().orEmpty().take(2048)
        fun classify(candidate: android.graphics.drawable.Icon?) = runCatching {
            val d = candidate?.loadDrawable(this) ?: return@runCatching Maneuver.UNKNOWN
            val b = Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
            d.setBounds(0,0,32,32); d.draw(Canvas(b))
            val pixels = IntArray(1024); b.getPixels(pixels,0,32,0,0,32,32); b.recycle()
            IconClassifier.classify(pixels,32)
        }.getOrDefault(Maneuver.UNKNOWN)
        val large = classify(n.getLargeIcon())
        val icon = if (large != Maneuver.UNKNOWN) large else classify(n.smallIcon)
        AppState.accept(Notice(sbn.key,source,text(Notification.EXTRA_TITLE),text(Notification.EXTRA_TEXT),
            text(Notification.EXTRA_SUB_TEXT),text(Notification.EXTRA_BIG_TEXT),
            e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.take(12)?.map { it.toString().take(2048) }.orEmpty(),icon))
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) { AppState.removed(sbn.key) }
    companion object { var instance: NavigationListener? = null }
}
