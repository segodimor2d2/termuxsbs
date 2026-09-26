package com.rec.termuxsbs.projection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay


class MediaProjectionService : Service() {

    companion object {
        const val ACTION_START = "com.rec.termuxsbs.START_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private const val CHANNEL_ID = "media_projection"
        private const val NOTIFICATION_ID = 1001
    }

    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_START) {

            val resultCode =
                intent.getIntExtra(EXTRA_RESULT_CODE, -1)

            val resultData =
                intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

            println(
                "MediaProjectionService: iniciado, resultCode=$resultCode, data=${resultData != null}"
            )

            startForeground(
                NOTIFICATION_ID,
                createNotification()
            )

            if (resultCode == -1 && resultData != null) {

                val manager =
                    getSystemService(MEDIA_PROJECTION_SERVICE)
                        as MediaProjectionManager

                mediaProjection =
                    manager.getMediaProjection(
                        resultCode,
                        resultData
                    )

                mediaProjectionCallback = object : MediaProjection.Callback() {

                    override fun onStop() {
                        println(
                            "MediaProjectionService: MediaProjection parada"
                        )

                        virtualDisplay?.release()
                        virtualDisplay = null

                        mediaProjection = null
                    }
                }

                mediaProjection?.registerCallback(
                    mediaProjectionCallback!!,
                    null
                )

                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "TermuxSbsCapture",
                    1272,
                    2772,
                    382,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    null,
                    null,
                    null
                )

                println(
                    "MediaProjectionService: MediaProjection obtido = ${mediaProjection != null}"
                )
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {

        val manager =
            getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Captura de tela",
            NotificationManager.IMPORTANCE_LOW
        )

        manager.createNotificationChannel(channel)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("TermuxSBS")
            .setContentText("Captura de tela ativa")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
