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
import android.os.Handler
import android.os.HandlerThread
import android.graphics.Color


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
    private var mediaProjectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var imageReaderThread: HandlerThread? = null
    private var imageReaderHandler: Handler? = null

    private fun imageToBitmap(
        image: android.media.Image
    ): Bitmap {

        val width = image.width
        val height = image.height

        val plane = image.planes[0]

        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride

        val bitmap =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

        val pixels =
            IntArray(width * height)

        for (y in 0 until height) {

            for (x in 0 until width) {

                val offset =
                    y * rowStride +
                    x * pixelStride

                val r =
                    buffer.get(offset)
                        .toInt() and 0xFF

                val g =
                    buffer.get(offset + 1)
                        .toInt() and 0xFF

                val b =
                    buffer.get(offset + 2)
                        .toInt() and 0xFF

                val a =
                    buffer.get(offset + 3)
                        .toInt() and 0xFF

                pixels[
                    y * width + x
                ] =
                    (a shl 24) or
                    (r shl 16) or
                    (g shl 8) or
                    b
            }
        }

        bitmap.setPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        return bitmap
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

                            imageReaderThread?.quitSafely()
                            imageReaderThread = null
                            imageReaderHandler = null
                        }
                    }

                mediaProjection?.registerCallback(
                    mediaProjectionCallback!!,
                    null
                )

                val metrics =
                    resources.displayMetrics

                println(
                    "MediaProjectionService: " +
                    "displayMetrics width=${metrics.widthPixels} " +
                    "height=${metrics.heightPixels} " +
                    "density=${metrics.densityDpi}"
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

                imageReaderThread =
                    HandlerThread("MediaProjectionImageReader").apply {
                        start()
                    }

                imageReaderHandler =
                    Handler(imageReaderThread!!.looper)

                imageReader?.setOnImageAvailableListener(
                    { reader ->

                        val image =
                            reader.acquireLatestImage()

                        val plane = image.planes[0]

                        val buffer = plane.buffer
                        val pixelStride = plane.pixelStride
                        val rowStride = plane.rowStride

                        println(
                            "MediaProjectionService: " +
                            "RAW pixelStride=$pixelStride " +
                            "rowStride=$rowStride " +
                            "bufferRemaining=${buffer.remaining()}"
                        )

                        println(
                            "MediaProjectionService: " +
                            "RAW bytes=" +
                            (0 until 16).joinToString(" ") { i ->
                                "%02x".format(buffer.get(i).toInt() and 0xFF)
                            }
                        )

                        if (image != null) {

                            val bitmap = imageToBitmap(image)

                            val step = 100

                            for (y in 0 until bitmap.height step step) {

                                var sum = 0L
                                var count = 0

                                for (x in 0 until bitmap.width step step) {

                                    val pixel = bitmap.getPixel(x, y)

                                    val r = Color.red(pixel)
                                    val g = Color.green(pixel)
                                    val b = Color.blue(pixel)

                                    sum += r + g + b
                                    count++
                                }

                                val average = sum.toDouble() / count

                                println(
                                    "MediaProjectionService: ROW y=$y avg=$average"
                                )
                            }

                            val sampleSize = 100

                            var min = 255
                            var max = 0
                            var sum = 0L
                            var count = 0

                            for (y in 0 until sampleSize) {
                                for (x in 0 until sampleSize) {

                                    val color =
                                        bitmap.getPixel(x, y)

                                    val r =
                                        android.graphics.Color.red(color)

                                    val g =
                                        android.graphics.Color.green(color)

                                    val b =
                                        android.graphics.Color.blue(color)

                                    min = minOf(min, r, g, b)
                                    max = maxOf(max, r, g, b)

                                    sum += r + g + b
                                    count += 3
                                }
                            }

                            val average =
                                sum.toDouble() / count

                            println(
                                "MediaProjectionService: " +
                                "RGB sample min=$min " +
                                "max=$max " +
                                "avg=$average"
                            )


                            val p1 = bitmap.getPixel(100, 100)
                            val p2 = bitmap.getPixel(636, 1386)
                            val p3 = bitmap.getPixel(1100, 500)
                            val p4 = bitmap.getPixel(600, 2200)

                            println(
                                "MediaProjectionService: " +
                                "pixels=" +
                                "p1=${Integer.toHexString(p1)} " +
                                "p2=${Integer.toHexString(p2)} " +
                                "p3=${Integer.toHexString(p3)} " +
                                "p4=${Integer.toHexString(p4)}"
                            )

                            val signature =
                                p1.toLong() +
                                p2.toLong() +
                                p3.toLong() +
                                p4.toLong()

                            latestBitmap = bitmap

                            println(
                                "MediaProjectionService: " +
                                "bitmap=${bitmap.width}x${bitmap.height} " +
                                "signature=$signature"
                            )

                            image.close()
                        }
                    },
                    imageReaderHandler
                )

                virtualDisplay =
                    mediaProjection?.createVirtualDisplay(
                        "TermuxSbsCapture",
                        1272,
                        2772,
                        382,
                        0,
                        imageReader?.surface,
                        null,
                        null
                    )

                println(
                    "MediaProjectionService: " +
                    "VirtualDisplay criado = ${virtualDisplay != null}"
                )

                println(
                    "MediaProjectionService: " +
                    "VirtualDisplay display=${virtualDisplay?.display}"
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
