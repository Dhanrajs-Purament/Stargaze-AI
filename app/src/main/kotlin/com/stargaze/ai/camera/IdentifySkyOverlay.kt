package com.stargaze.ai.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.stargaze.ai.astronomy.DetectedStar
import com.stargaze.ai.ui.onboarding.PrimaryButton
import com.stargaze.ai.ui.sky.IdentifyState
import com.stargaze.ai.ui.theme.StarColors
import java.util.concurrent.Executors

/**
 * Full-screen camera overlay that performs real-time visual sky identification (plate solving).
 *
 * Binds a CameraX preview + image analysis. When the user starts scanning, each frame's detected
 * stars are pushed to [onDetections] (the ViewModel runs the solver). Requires CAMERA permission;
 * if denied, explains and lets the user dismiss. The camera is released automatically when this
 * leaves composition.
 */
@Composable
fun IdentifySkyOverlay(
    identifyState: IdentifyState,
    onDetections: (List<DetectedStar>, Int, Int) -> Unit,
    onBeginScan: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val analyzer = remember {
        SkyFrameAnalyzer { detection -> onDetections(detection.stars, detection.gridWidth, detection.gridHeight) }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    // Keep the analyzer's gate in sync with scan state.
    analyzer.enabled = identifyState is IdentifyState.Scanning

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(cameraExecutor, analyzer) }
                        provider.unbindAll()
                        runCatching {
                            provider.bindToLifecycle(
                                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis,
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("\uD83D\uDCF7", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Camera needed to identify the sky", color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Point your phone at the stars and StarGaze will detect them and identify what you're looking at — all on your device.",
                    color = StarColors.Muted, fontSize = 14.sp,
                )
                Spacer(Modifier.height(18.dp))
                PrimaryButton("Grant camera access") { permissionLauncher.launch(Manifest.permission.CAMERA) }
            }
        }

        // Bottom control panel.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = identifyState) {
                is IdentifyState.Solved -> ResultCard(s)
                is IdentifyState.Scanning -> InfoText("Scanning the sky… hold steady")
                is IdentifyState.Failed -> InfoText(s.reason)
                IdentifyState.Idle -> Unit
            }
            Spacer(Modifier.height(12.dp))
            if (hasPermission && identifyState !is IdentifyState.Scanning) {
                PrimaryButton(
                    if (identifyState is IdentifyState.Solved) "Scan again" else "Identify the sky",
                    modifier = Modifier.fillMaxWidth(),
                ) { onBeginScan() }
            }
            Spacer(Modifier.height(10.dp))
            Text("Close", color = StarColors.Muted, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onDismiss))
        }
    }
}

@Composable
private fun ResultCard(s: IdentifyState.Solved) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0A0D1C))
            .border(1.dp, StarColors.Line, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("\u2705 Sky identified", color = StarColors.Green, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        val cons = if (s.constellations.isEmpty()) "Matched stars in view" else "In view: ${s.constellations.joinToString(", ")}"
        Text(cons, color = StarColors.Ink, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            "Compass corrected by ${"%.1f".format(s.driftCorrectedDeg)}° · confidence ${(s.confidence * 100).toInt()}%",
            color = StarColors.Muted, fontSize = 12.sp,
        )
    }
}

@Composable
private fun InfoText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC0A0D1C))
            .border(1.dp, StarColors.Line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = StarColors.Ink, fontSize = 14.sp) }
}
