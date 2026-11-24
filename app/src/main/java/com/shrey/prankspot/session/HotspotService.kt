package com.shrey.prankspot.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.shrey.prankspot.PrankSpotApp
import com.shrey.prankspot.R
import com.shrey.prankspot.ui.MainActivity

/** Keeps the hotspot and the prank page alive while PrankSpot is in the background. */
class HotspotService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            (application as PrankSpotApp).container.session.stop()
            return START_NOT_STICKY
        }
        ensureChannel(this)
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, build(this), type)
        // If the process died, the hotspot died with it: don't come back pretending it's running.
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiped away from recents: keep running; the notification has a Stop button.
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        private const val CHANNEL = "session"
        private const val NOTIFICATION_ID = 7
        private const val ACTION_STOP = "com.shrey.prankspot.STOP"

        fun startIntent(context: Context) = Intent(context, HotspotService::class.java)

        fun stop(context: Context) {
            context.stopService(Intent(context, HotspotService::class.java))
        }

        fun refresh(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            runCatching { nm.notify(NOTIFICATION_ID, build(context)) }
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Active prank hotspot", NotificationManager.IMPORTANCE_LOW),
                )
            }
        }

        private fun build(context: Context): Notification {
            val s = (context.applicationContext as PrankSpotApp).container.session.state.value
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val stop = PendingIntent.getService(
                context, 1, Intent(context, HotspotService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val text = when (s.phase) {
                Phase.RUNNING -> "Live: ${s.info?.ssid ?: s.config?.ssid ?: "your hotspot"}"
                Phase.WAITING_FOR_SYSTEM_HOTSPOT -> "Waiting for your phone's hotspot…"
                else -> "Starting the prank hotspot…"
            }
            return NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("PrankSpot")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(0, "Stop", stop)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        }
    }
}
