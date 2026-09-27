package com.rec.termuxsbs

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Canvas

import com.rec.termuxsbs.projection.MediaProjectionService
import com.rec.termuxsbs.ui.theme.TermuxsbsTheme

import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.asImageBitmap
import android.view.WindowManager



class MainActivity : ComponentActivity() {

    init {
        println("TermuxSbs: MainActivity CLASS CARREGADA")
    }

    private var lastDisplayState: String? = null

    private lateinit var mediaProjectionManager:
        MediaProjectionManager

    private val captureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                println(
                    "TermuxSbs: MediaProjection autorizado"
                )

                val serviceIntent =
                    Intent(
                        this,
                        MediaProjectionService::class.java
                    ).apply {

                        action =
                            MediaProjectionService.ACTION_START

                        putExtra(
                            MediaProjectionService
                                .EXTRA_RESULT_CODE,
                            result.resultCode
                        )

                        putExtra(
                            MediaProjectionService
                                .EXTRA_RESULT_DATA,
                            result.data
                        )
                    }

                startForegroundService(
                    serviceIntent
                )

            } else {

                println(
                    "TermuxSbs: MediaProjection cancelado"
                )
            }
        }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        println("TermuxSbs: onCreate")

        mediaProjectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        println("TermuxSbs: mediaProjectionManager pronto")

        setContent {

            println("TermuxSbs: setContent executando")

            TermuxsbsTheme {

                var bitmap by remember {
                    mutableStateOf<Bitmap?>(null)
                }

                LaunchedEffect(Unit) {

                    println("TermuxSbs: LaunchedEffect iniciou")

                    while (true) {

                        val captured =
                            MediaProjectionService
                                .getLatestBitmap()

                        if (captured != null) {

                            val p1 = captured.getPixel(0, 0)

                            val p2 = captured.getPixel(
                                captured.width / 2,
                                captured.height / 2
                            )

                            val p3 = captured.getPixel(
                                captured.width - 1,
                                captured.height - 1
                            )

                            val state =
                                if (p1 == 0 && p2 == 0 && p3 == 0) {
                                    "PRETO"
                                } else {
                                    "CONTEUDO"
                                }

                            if (state != lastDisplayState) {

                                lastDisplayState = state

                                println(
                                    "TermuxSbs: DISPLAY=$state " +
                                        "${captured.width}x${captured.height} " +
                                        "p1=${Integer.toHexString(p1)} " +
                                        "p2=${Integer.toHexString(p2)} " +
                                        "p3=${Integer.toHexString(p3)}"
                                )
                            }

                            bitmap = captured
                        }

                        delay(50)
                    }
                }

            val testBitmap =
                remember {

                    Bitmap.createBitmap(
                        600,
                        600,
                        Bitmap.Config.ARGB_8888
                    ).also { bitmap ->

                        val canvas = Canvas(bitmap)

                        val paint = Paint()

                        paint.color = Color.RED

                        canvas.drawRect(
                            0f,
                            0f,
                            300f,
                            600f,
                            paint
                        )

                        paint.color = Color.GREEN

                        canvas.drawRect(
                            300f,
                            0f,
                            600f,
                            600f,
                            paint
                        )

                        paint.color = Color.BLUE

                        canvas.drawCircle(
                            300f,
                            300f,
                            120f,
                            paint
                        )
                    }
                }


                if (bitmap != null) {

                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                } else {

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {

                            Image(
                                bitmap = testBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            Image(
                                bitmap = testBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Button(
                            onClick = {

                                val intent =
                                    mediaProjectionManager
                                        .createScreenCaptureIntent()

                                captureLauncher.launch(intent)
                            },
                            modifier = Modifier.align(
                                Alignment.BottomCenter
                            )
                        ) {
                            Text("Iniciar captura")
                        }
                    }
                }
            }
        }
    }
}

