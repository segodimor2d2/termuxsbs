package com.rec.termuxsbs.projection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder

class MediaProjectionService : Service() {

    companion object {

        const val ACTION_START =
            "com.rec.termuxsbs.START_CAPTURE"

        const val EXTRA_RESULT_CODE =
            "result_code"

        const val EXTRA_RESULT_DATA =
            "result_data"

        private const val CHANNEL_ID =
            "media_projection"

        private const val NOTIFICATION_ID =
            1001

        private var latestBitmap: Bitmap? = null

        fun getLatestBitmap(): Bitmap? {
            return latestBitmap
        }
    }

    private var mediaProjection: MediaProjection? = null

    private var mediaProjectionCallback:
        MediaProjection.Callback? = null

    private var virtualDisplay: VirtualDisplay? = null

    private var imageReader: ImageReader? = null

    private fun imageToBitmap(
        image: android.media.Image
    ): Bitmap {

        val width = image.width
        val height = image.height

        val plane = image.planes[0]

        val buffer = plane.buffer

        val pixelStride = plane.pixelStride

        val rowStride = plane.rowStride

        val rowPadding =
            rowStride - pixelStride * width

        val bitmapWidth =
            width + rowPadding / pixelStride

        val bitmap = Bitmap.createBitmap(
            bitmapWidth,
            height,
            Bitmap.Config.ARGB_8888
        )

        bitmap.copyPixelsFromBuffer(buffer)

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            width,
            height
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_START) {

            val resultCode =
                intent.getIntExtra(
                    EXTRA_RESULT_CODE,
                    -1
                )

            val resultData =
                intent.getParcelableExtra<Intent>(
                    EXTRA_RESULT_DATA
                )

            println(
                "MediaProjectionService: " +
                "iniciado, resultCode=$resultCode, " +
                "data=${resultData != null}"
            )

            startForeground(
                NOTIFICATION_ID,
                createNotification()
            )

            if (
                resultCode == -1 &&
                resultData != null
            ) {

                val manager =
                    getSystemService(
                        MEDIA_PROJECTION_SERVICE
                    ) as MediaProjectionManager

                mediaProjection =
                    manager.getMediaProjection(
                        resultCode,
                        resultData
                    )

                mediaProjectionCallback =
                    object : MediaProjection.Callback() {

                        override fun onStop() {

                            println(
                                "MediaProjectionService: " +
                                "MediaProjection parada"
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

                imageReader =
                    ImageReader.newInstance(
                        1272,
                        2772,
                        android.graphics.PixelFormat.RGBA_8888,
                        2
                    )

                println(
                    "MediaProjectionService: " +
                    "ImageReader criado = " +
                    "${imageReader != null}"
                )

                imageReader?.setOnImageAvailableListener(
                    { reader ->

                        val image =
                            reader.acquireLatestImage()

                        if (image != null) {

                            val bitmap =
                                imageToBitmap(image)

                            if (latestBitmap == null) {

                                latestBitmap = bitmap

                                println(
                                    "MediaProjectionService: " +
                                    "primeiro bitmap=" +
                                    "${bitmap.width}x${bitmap.height}"
                                )

                            } else {

                                bitmap.recycle()
                            }

                            image.close()
                        }
                    },
                    null
                )

                virtualDisplay =
                    mediaProjection?.createVirtualDisplay(
                        "TermuxSbsCapture",
                        1272,
                        2772,
                        382,
                        DisplayManager
                            .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader?.surface,
                        null,
                        null
                    )

                println(
                    "MediaProjectionService: " +
                    "MediaProjection obtido = " +
                    "${mediaProjection != null}"
                )
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Captura de tela",
                NotificationManager.IMPORTANCE_LOW
            )

        manager.createNotificationChannel(channel)

        return Notification.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("TermuxSBS")
            .setContentText(
                "Captura de tela ativa"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_view
            )
            .build()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}
