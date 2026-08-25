package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.BharatElectricCyan
import com.example.ui.theme.BharatGreenLight
import com.example.ui.theme.BharatSaffron
import com.example.ui.theme.BharatWhite
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Modern Jetpack Compose CameraPreview component using the CameraView (CameraX PreviewView) library.
 *
 * It seamlessly requests and checks camera permissions using Google Accompanist Permissions,
 * displays the live video feed once granted, supports front/back camera lens switching,
 * pinch-to-zoom, and provides lifecycle-aware binding to the hosting Activity/Fragment.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = true,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    implementationMode: PreviewView.ImplementationMode = PreviewView.ImplementationMode.COMPATIBLE,
    enablePinchToZoom: Boolean = true,
    onCameraInitialized: ((Camera) -> Unit)? = null,
    onPermissionDeniedContent: (@Composable () -> Unit)? = null
) {
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    if (cameraPermissionState.status.isGranted) {
        CameraPreviewFeed(
            isFrontCamera = isFrontCamera,
            scaleType = scaleType,
            implementationMode = implementationMode,
            enablePinchToZoom = enablePinchToZoom,
            onCameraInitialized = onCameraInitialized,
            modifier = modifier
        )
    } else {
        if (onPermissionDeniedContent != null) {
            onPermissionDeniedContent()
        } else {
            CameraPermissionRationale(
                permissionState = cameraPermissionState,
                modifier = modifier
            )
        }
    }
}

/**
 * Underlying CameraX PreviewView host that connects to ProcessCameraProvider
 * and renders the camera stream once permission has been validated.
 */
@Composable
fun CameraPreviewFeed(
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    implementationMode: PreviewView.ImplementationMode = PreviewView.ImplementationMode.COMPATIBLE,
    enablePinchToZoom: Boolean = true,
    onCameraInitialized: ((Camera) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }

    val zoomModifier = if (enablePinchToZoom && activeCamera != null) {
        Modifier.pointerInput(activeCamera) {
            detectTransformGestures { _, _, zoom, _ ->
                val camera = activeCamera ?: return@detectTransformGestures
                val zoomState = camera.cameraInfo.zoomState.value ?: return@detectTransformGestures
                val newRatio = (currentZoomRatio * zoom).coerceIn(
                    zoomState.minZoomRatio,
                    zoomState.maxZoomRatio
                )
                currentZoomRatio = newRatio
                camera.cameraControl.setZoomRatio(newRatio)
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(zoomModifier)
            .testTag("camera_preview_view")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.scaleType = scaleType
                    this.implementationMode = implementationMode
                }
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val lensFacing = if (isFrontCamera) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        cameraProvider.unbindAll()
                        val boundCamera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                        activeCamera = boundCamera
                        onCameraInitialized?.invoke(boundCamera)
                    } catch (exc: Exception) {
                        // Fallback to default cameras if specific lensFacing throws
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            cameraProvider.unbindAll()
                            val fallbackCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                                preview
                            )
                            activeCamera = fallbackCamera
                            onCameraInitialized?.invoke(fallbackCamera)
                        } catch (e: Exception) {
                            // Safe fallback
                        }
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )
    }
}

/**
 * Fallback and Rationale UI shown before camera permissions are granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionRationale(
    permissionState: com.google.accompanist.permissions.PermissionState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B192C))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BharatSaffron.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Permission",
                    tint = BharatSaffron,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (permissionState.status.shouldShowRationale) {
                    "Camera Access Required"
                } else {
                    "Enable Camera Access"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BharatWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Allow camera access to view live front and rear camera video feed during calls and capture photos.",
                fontSize = 12.sp,
                color = BharatWhite.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { permissionState.launchPermissionRequest() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BharatSaffron,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.testTag("request_camera_permission_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Grant Permission",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
