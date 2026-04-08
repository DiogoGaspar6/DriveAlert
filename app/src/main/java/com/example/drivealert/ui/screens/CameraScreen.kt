package com.example.drivealert.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.drivealert.ml.FaceDetectorHelper
import com.example.drivealert.sensores.MotionSensorManager
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay

private val faceDetectorHelper = FaceDetectorHelper()
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen(navController: NavController) {
    var isTired by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val motionSensor = remember { MotionSensorManager(context) }
    var isMoving by remember { mutableStateOf(false) }

    val mediaPlayer = remember {
        MediaPlayer.create(
            context,
            Settings.System.DEFAULT_ALARM_ALERT_URI
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->

                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {

                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )

                                            faceDetectorHelper.processImage(
                                                image,
                                                onResult = { tired ->
                                                    isTired = tired
                                                },
                                                onComplete = {
                                                    imageProxy.close()
                                                }
                                            )
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer
                            )

                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                Surface(
                    color = (if (isTired) Color.Red else Color.Green).copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isTired) "FADIGA DETETADA" else "OK",
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isMoving) Icons.Default.DirectionsCar else Icons.Default.PauseCircle,
                        contentDescription = null,
                        tint = if (isMoving) Color.Cyan else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMoving) "VEÍCULO EM MOVIMENTO" else "VEÍCULO PARADO",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = if (isTired) "ENCOSTE O VEÍCULO IMEDIATAMENTE!" else "Conduza com cuidado. Olhos na estrada.",
                    textAlign = TextAlign.Center,
                    color = if (isTired) Color.Red else Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge
                )

                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Parar", color = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }

    LaunchedEffect(isTired) {
        if (isTired) {

            while (isTired) {

                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                }

                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(
                    0,
                    500,
                    200,
                    500,
                    200,
                    800
                )

                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )

                delay(3000)
            }
        } else {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                mediaPlayer.seekTo(0)
            }
        }
    }

    LaunchedEffect(Unit) {
        motionSensor.start()
        while (true) {
            isMoving = motionSensor.isMoving
            delay(100)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            motionSensor.stop()
            mediaPlayer.release()
        }
    }
}