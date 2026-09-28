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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.WindowManager

import android.content.ComponentName
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitPairFilter
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.RuleController
import androidx.window.embedding.EmbeddingAspectRatio


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

        val embeddedComponent = ComponentName(
            "com.testfiles",
            "com.testfiles.MainActivity"
        )

        val splitPairFilter = SplitPairFilter(
            ComponentName(this, MainActivity::class.java),
            embeddedComponent,
            null
        )

        println(
            "TermuxSbs: filtro primary = " +
                splitPairFilter.primaryActivityName
        )

        println(
            "TermuxSbs: filtro secondary = " +
                splitPairFilter.secondaryActivityName
        )

        println(
            "TermuxSbs: filtro matches = " +
                splitPairFilter.matchesActivityIntentPair(
                    this,
                    Intent(this, EmbeddedActivity::class.java)
                )
        )

        val splitRule = SplitPairRule.Builder(
            setOf(splitPairFilter)
        )
            .setMinWidthDp(0)
            .setMinHeightDp(0)
            .setMinSmallestWidthDp(0)
            .setMaxAspectRatioInPortrait(
                EmbeddingAspectRatio.ALWAYS_ALLOW
            )
            .setDefaultSplitAttributes(
                SplitAttributes.Builder()
                    .setSplitType(
                        SplitAttributes.SplitType.ratio(0.5f)
                    )
                    .build()
            )
            .build()

        println("TermuxSbs: splitRule default = ${splitRule.defaultSplitAttributes}")

        RuleController.getInstance(this).addRule(splitRule)

        println("TermuxSbs: SplitPairRule registrada")

        val registeredRules =
            RuleController.getInstance(this).getRules()

        println(
            "TermuxSbs: regras registradas = $registeredRules"
        )

        println(
            "TermuxSbs: quantidade de regras = ${registeredRules.size}"
        )

        val embeddingBackend =
            androidx.window.embedding.EmbeddingBackend
                .getInstance(this)

        println(
            "TermuxSbs: backend rules = " +
                embeddingBackend.getRules()
        )

        println(
            "TermuxSbs: backend splitSupportStatus = " +
                embeddingBackend.splitSupportStatus
        )

        println(
            "TermuxSbs: SplitSupportStatus = " +
                embeddingBackend.splitSupportStatus
        )

        val embeddingController =
            androidx.window.embedding.ActivityEmbeddingController
                .getInstance(this)

        println(
            "TermuxSbs: MainActivity embedded=" +
                embeddingController.isActivityEmbedded(this)
        )

        println("TermuxSbs: SplitPairRule registrada")

        embeddingBackend.addSplitListenerForActivity(
            this,
            androidx.core.content.ContextCompat.getMainExecutor(this),
            androidx.core.util.Consumer { splits ->

                for (split in splits) {
                    println(
                        "TermuxSbs: Split primary = " +
                            split.primaryActivityStack
                    )

                    println(
                        "TermuxSbs: Split secondary = " +
                            split.secondaryActivityStack
                    )

                    println(
                        "TermuxSbs: Split attributes = " +
                            split.splitAttributes
                    )
                }
            }
        )

        startActivity(
            Intent().setComponent(
                ComponentName(
                    "com.testfiles",
                    "com.testfiles.MainActivity"
                )
            )
        )

        println("TermuxSbs: EmbeddedActivity iniciada")

        android.os.Handler(mainLooper).postDelayed({

            val stack =
                androidx.window.embedding.ActivityEmbeddingController
                    .getInstance(this)
                    .getActivityStack(this)

            println("TermuxSbs: ActivityStack = $stack")
            println("TermuxSbs: stack vazia = ${stack?.isEmpty}")
            println("TermuxSbs: MainActivity na stack = ${stack?.contains(this)}")

        }, 3000)

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

                            var nonBlack = 0

                            for (y in 200 until captured.height - 200 step 200) {
                                for (x in 100 until captured.width - 100 step 100) {

                                    val pixel = captured.getPixel(x, y)

                                    val r = android.graphics.Color.red(pixel)
                                    val g = android.graphics.Color.green(pixel)
                                    val b = android.graphics.Color.blue(pixel)

                                    if (r != 0 || g != 0 || b != 0) {
                                        nonBlack++
                                    }
                                }
                            }

                            val state =
                                if (nonBlack == 0) {
                                    "PRETO"
                                } else {
                                    "CONTEUDO"
                                }

                            if (state != lastDisplayState) {

                                lastDisplayState = state

                                println(
                                    "TermuxSbs: DISPLAY=$state " +
                                        "${captured.width}x${captured.height} " +
                                        "nonBlack=$nonBlack"
                                )
                            }

                            if (nonBlack > 0) {
                                bitmap = captured
                            }
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

