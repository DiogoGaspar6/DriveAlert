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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    var isMoving by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val motionSensor = remember { MotionSensorManager(context) }

    val mediaPlayer = remember {
        MediaPlayer.create(context, Settings.System.DEFAULT_ALARM_ALERT_URI).apply {
            isLooping = true
        }
    }

    DisposableEffect(Unit) {
        motionSensor.start()
        onDispose {
            motionSensor.stop()
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isMoving = motionSensor.isMoving
            delay(500)
        }
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
            Box(modifier = Modifier.fillMaxSize()) {
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
                                                onResult = { tired -> isTired = tired },
                                                onComplete = { imageProxy.close() }
                                            )
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner, cameraSelector, preview, imageAnalyzer
                                )
                            } catch (e: Exception) {
                                Log.e("CameraScreen", "Erro ao ligar câmara: ${e.message}")
                            }
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
                        text = if (isTired) "FADIGA DETETADA" else "OK",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                .weight(0.7f),
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isTired) FontWeight.Bold else FontWeight.Normal
                )

                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                ) {
                    Text("Finalizar Viagem", color = Color.Red)
                }
            }
        }
    }

    LaunchedEffect(isTired) {
        if (isTired) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 500, 200, 500)

            while (isTired) {
                if (!mediaPlayer.isPlaying) mediaPlayer.start()
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                delay(2000)
            }
        } else {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                mediaPlayer.seekTo(0)
            }
        }
    }
}