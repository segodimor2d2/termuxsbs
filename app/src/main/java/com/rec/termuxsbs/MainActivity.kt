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

import com.rec.termuxsbs.projection.MediaProjectionService
import com.rec.termuxsbs.ui.theme.TermuxsbsTheme

import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

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

        mediaProjectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager


        setContent {

            TermuxsbsTheme {

                var bitmap by remember {
                    mutableStateOf<Bitmap?>(null)
                }


                LaunchedEffect(Unit) {

                    while (bitmap == null) {

                        val captured =
                            MediaProjectionService
                                .getLatestBitmap()

                        if (captured != null) {

                            bitmap = captured

                        } else {

                            delay(100)
                        }
                    }
                }


                Box(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    if (bitmap != null) {

                        Image(
                            bitmap =
                                bitmap!!.asImageBitmap(),

                            contentDescription =
                                "Tela capturada",

                            modifier =
                                Modifier.fillMaxSize(),

                            contentScale =
                                ContentScale.Fit
                        )

                    } else {

                        Column(
                            modifier =
                                Modifier.fillMaxSize(),

                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            verticalArrangement =
                                Arrangement.Center
                        ) {

                            Button(
                                onClick = {

                                    val intent =
                                        mediaProjectionManager
                                            .createScreenCaptureIntent()

                                    captureLauncher
                                        .launch(intent)
                                }
                            ) {

                                Text(
                                    "Iniciar captura"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
