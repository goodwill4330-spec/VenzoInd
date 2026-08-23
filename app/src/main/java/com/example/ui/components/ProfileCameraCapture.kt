package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Reusable CameraX Profile Picture Capture Component.
 *
 * Features:
 * - Live CameraX Preview with Front/Back lens switching.
 * - Circular profile avatar viewfinder framing mask.
 * - Flash control (Auto, On, Off).
 * - Instant capture to Bitmap / local cache Uri.
 * - Review / Retake / Confirm screen with circular crop preview.
 * - Automatic runtime camera permission handling.
 */
@Composable
fun ProfileCameraCaptureDialog(
    onDismiss: () -> Unit,
    onPhotoConfirmed: (Uri, Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = Color.Black
        ) {
            ProfileCameraCaptureContent(
                onDismiss = onDismiss,
                onPhotoConfirmed = onPhotoConfirmed
            )
        }
    }
}

@Composable
fun ProfileCameraCaptureContent(
    onDismiss: () -> Unit,
    onPhotoConfirmed: (Uri, Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to capture profile picture", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Configuration State
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Captured review state
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    if (!hasCameraPermission) {
        // Permission Request UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(BharatGreenLight.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = BharatGreenLight,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "Camera Access Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BharatWhite
                )

                Text(
                    text = "Allow camera access to capture and set your WhatsApp-style profile avatar.",
                    fontSize = 14.sp,
                    color = TextSecondaryDark,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreenNeon),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Permission", color = Color(0xFF022613), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x3364748B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    } else if (capturedBitmap != null && capturedUri != null) {
        // Photo Review & Crop Confirmation Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        capturedBitmap = null
                        capturedUri = null
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retake", tint = BharatWhite)
                    }

                    Text(
                        text = "Profile Photo Preview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatWhite
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                    }
                }

                // Circular Framed Preview
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(CircleShape)
                            .border(3.dp, CyberGreenNeon, CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "This will be visible to your contacts",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                }

                // Action Buttons (Retake & Use Photo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            capturedBitmap = null
                            capturedUri = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("retake_photo_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x4464748B)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BharatWhite)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retake", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onPhotoConfirmed(capturedUri!!, capturedBitmap!!)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("confirm_profile_photo_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreenNeon)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF022613), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set As Profile", color = Color(0xFF022613), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Live Camera Viewfinder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // AndroidView for CameraX PreviewView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setFlashMode(flashMode)
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            imageCapture = capture
                        } catch (exc: Exception) {
                            Log.e("CameraCapture", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setFlashMode(flashMode)
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            imageCapture = capture
                        } catch (exc: Exception) {
                            Log.e("CameraCapture", "Use case update failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            // Circular Overlay Mask for Profile Picture Framing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val circleRadius = (canvasWidth * 0.38f).coerceAtMost(canvasHeight * 0.28f)
                val centerOffset = Offset(canvasWidth / 2f, canvasHeight * 0.42f)

                // Draw translucent mask with circular transparent cutout
                val maskPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                    addOval(
                        Rect(
                            center = centerOffset,
                            radius = circleRadius
                        )
                    )
                }
                drawPath(maskPath, color = Color.Black.copy(alpha = 0.65f))

                // Glowing circular ring boundary
                drawCircle(
                    color = CyberGreenNeon,
                    radius = circleRadius,
                    center = centerOffset,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Subtle guide crosshairs/marks
                val markLen = 14.dp.toPx()
                drawLine(
                    color = BharatSaffron,
                    start = Offset(centerOffset.x - markLen, centerOffset.y),
                    end = Offset(centerOffset.x + markLen, centerOffset.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = BharatSaffron,
                    start = Offset(centerOffset.x, centerOffset.y - markLen),
                    end = Offset(centerOffset.x, centerOffset.y + markLen),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Top Bar Controls (Close, Flash, Switch Camera)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = BharatWhite)
                }

                // Flash Toggle
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash Toggle",
                        tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) CyberGreenNeon else BharatWhite
                    )
                }

                // Lens Flip (Front / Back)
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                            CameraSelector.LENS_FACING_BACK
                        } else {
                            CameraSelector.LENS_FACING_FRONT
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("switch_camera_lens_button")
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Camera", tint = BharatWhite)
                }
            }

            // Bottom Shutter & Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Position your face within the circle",
                    fontSize = 13.sp,
                    color = BharatWhite.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, CyberGreenNeon, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(CyberGreenNeon, CyberGreenPrimary)
                            )
                        )
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (!isCapturing && imageCapture != null) {
                                isCapturing = true
                                capturePhoto(
                                    context = context,
                                    imageCapture = imageCapture!!,
                                    executor = cameraExecutor,
                                    isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT,
                                    onSuccess = { uri, bitmap ->
                                        isCapturing = false
                                        capturedUri = uri
                                        capturedBitmap = bitmap
                                    },
                                    onError = { exc ->
                                        isCapturing = false
                                        Toast.makeText(context, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        enabled = !isCapturing,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = Color(0xFF022613),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Photo",
                                tint = Color(0xFF022613),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Captures photo from ImageCapture, processes rotation/mirroring for front camera,
 * saves to local cache and returns cropped square profile bitmap and Uri.
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    isFrontCamera: Boolean,
    onSuccess: (Uri, Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = imageProxyToBitmap(image, isFrontCamera)
                    image.close()

                    // Crop to square for profile
                    val squareBitmap = cropToSquare(bitmap)

                    // Save to local cache
                    val cacheFile = File(context.cacheDir, "profile_camera_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(cacheFile).use { out ->
                        squareBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }

                    val uri = Uri.fromFile(cacheFile)

                    ContextCompat.getMainExecutor(context).execute {
                        onSuccess(uri, squareBitmap)
                    }
                } catch (e: Exception) {
                    image.close()
                    ContextCompat.getMainExecutor(context).execute {
                        onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "Processing error: ${e.message}", e))
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                ContextCompat.getMainExecutor(context).execute {
                    onError(exception)
                }
            }
        }
    )
}

/**
 * Converts ImageProxy to a correctly oriented Bitmap with front camera mirror adjustment.
 */
private fun imageProxyToBitmap(image: ImageProxy, isFrontCamera: Boolean): Bitmap {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    if (isFrontCamera) {
        matrix.postScale(-1f, 1f) // Mirror horizontal for selfie preview natural feel
    }

    return Bitmap.createBitmap(
        rawBitmap,
        0,
        0,
        rawBitmap.width,
        rawBitmap.height,
        matrix,
        true
    )
}

/**
 * Crops a bitmap into a centered 1:1 square for profile avatars.
 */
private fun cropToSquare(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val newDimension = width.coerceAtMost(height)

    val cropX = (width - newDimension) / 2
    val cropY = (height - newDimension) / 2

    return Bitmap.createBitmap(bitmap, cropX, cropY, newDimension, newDimension)
}
