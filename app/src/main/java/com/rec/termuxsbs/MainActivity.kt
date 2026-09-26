package com.rec.termuxsbs

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rec.termuxsbs.ui.theme.TermuxsbsTheme
import com.rec.termuxsbs.projection.MediaProjectionService

class MainActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val captureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                Log.d("TermuxSbs", "MediaProjection autorizado")

                val serviceIntent = Intent(
                    this,
                    MediaProjectionService::class.java
                ).apply {
                    action = MediaProjectionService.ACTION_START

                    putExtra(
                        MediaProjectionService.EXTRA_RESULT_CODE,
                        result.resultCode
                    )

                    putExtra(
                        MediaProjectionService.EXTRA_RESULT_DATA,
                        result.data
                    )
                }

                startForegroundService(serviceIntent)

            } else {

                Log.d("TermuxSbs", "MediaProjection cancelado")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            TermuxsbsTheme {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Button(
                        onClick = {

                            val intent =
                                mediaProjectionManager.createScreenCaptureIntent()

                            captureLauncher.launch(intent)
                        }
                    ) {
                        Text("Iniciar captura")
                    }
                }
            }
        }
    }
}
