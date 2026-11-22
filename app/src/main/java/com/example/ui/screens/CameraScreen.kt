package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.FilterPrefEntity
import com.example.data.local.PrivateMediaStorage
import com.example.data.model.DistortionType
import com.example.data.model.FilterCatalog
import com.example.data.model.SnapFilter
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnapCoral
import com.example.ui.theme.SnapYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    allFilters: List<SnapFilter>,
    filterPrefs: List<FilterPrefEntity>,
    onFilterUsed: (SnapFilter) -> Unit,
    onToggleFavoriteFilter: (SnapFilter) -> Unit,
    onCaptureBitmap: (Bitmap, SnapFilter, Float, Float) -> Unit,
    onVideoRecorded: (String, SnapFilter) -> Unit,
    onPickedGalleryMedia: (android.net.Uri, Boolean, SnapFilter) -> Unit,
    onOpenSavedSnaps: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            onPickedGalleryMedia(uri, mime.startsWith("video"), selectedFilterGlobal)
        }
    }

    var isFrontCamera by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var hdrEnabled by remember { mutableStateOf(true) }
    var isVideoMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var countdownRemaining by remember { mutableIntStateOf(0) }
    var beautyLevel by remember { mutableFloatStateOf(0.25f) }
    var brightnessLevel by remember { mutableFloatStateOf(0f) }
    var exposureIndex by remember { mutableIntStateOf(0) }
    var showAdjustPanel by remember { mutableStateOf(false) }
    var showAllFiltersSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(FilterCatalog.originalFilter) }
    selectedFilterGlobal = selectedFilter

    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
        }
    }

    var activeRecording by remember { mutableStateOf<androidx.camera.video.Recording?>(null) }

    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        if (hasCameraPermission) {
            cameraController.bindToLifecycle(lifecycleOwner)
        }
        onDispose {
            activeRecording?.stop()
        }
    }

    LaunchedEffect(isFrontCamera, flashOn, zoomRatio, exposureIndex, hasCameraPermission) {
        if (hasCameraPermission) {
            cameraController.cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            cameraController.imageCaptureFlashMode = if (flashOn) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }
            cameraController.setZoomRatio(zoomRatio)
            cameraController.cameraControl?.setExposureCompensationIndex(exposureIndex)
        }
    }

    fun capturePhotoNow() {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        cameraController.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    val rot = image.imageInfo.rotationDegrees.toFloat()
                    image.close()
                    val matrix = Matrix().apply {
                        if (rot != 0f) postRotate(rot)
                        if (isFrontCamera) postScale(-1f, 1f)
                    }
                    val corrected = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    onCaptureBitmap(corrected, selectedFilter, beautyLevel, brightnessLevel)
                }

                override fun onError(exception: ImageCaptureException) {
                    val fallback = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
                    val c = android.graphics.Canvas(fallback)
                    c.drawColor(android.graphics.Color.rgb(25, 25, 34))
                    onCaptureBitmap(fallback, selectedFilter, beautyLevel, brightnessLevel)
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun toggleVideoRecording() {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        if (isRecording) {
            activeRecording?.stop()
            activeRecording = null
            isRecording = false
        } else {
            val hasAudio = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasAudio) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            val videoFile = PrivateMediaStorage.createPrivateVideoFile(context)
            val outputOpts = androidx.camera.video.FileOutputOptions.Builder(videoFile).build()
            val audioCfg = if (hasAudio) AudioConfig.create(true) else AudioConfig.AUDIO_DISABLED
            isRecording = true
            activeRecording = cameraController.startRecording(
                outputOpts,
                audioCfg,
                ContextCompat.getMainExecutor(context)
            ) { event ->
                if (event is androidx.camera.video.VideoRecordEvent.Finalize) {
                    isRecording = false
                    if (!event.hasError()) {
                        onVideoRecorded(videoFile.absolutePath, selectedFilter)
                    }
                }
            }
        }
    }

    fun triggerShutter() {
        if (isVideoMode) {
            toggleVideoRecording()
            return
        }
        if (timerSeconds == 0) {
            capturePhotoNow()
        } else {
            scope.launch {
                for (sec in timerSeconds downTo 1) {
                    countdownRemaining = sec
                    delay(1000L)
                }
                countdownRemaining = 0
                capturePhotoNow()
            }
        }
    }

    val favoriteIds = remember(filterPrefs) {
        filterPrefs.filter { it.favorite }.map { it.filterId }.toSet()
    }
    val recentIds = remember(filterPrefs) {
        filterPrefs.sortedByDescending { it.lastUsedAtMillis }.take(20).map { it.filterId }
    }
    val quickStripFilters = remember(allFilters) {
        val list = mutableListOf<SnapFilter>()
        list.add(FilterCatalog.originalFilter)
        list.addAll(allFilters.filter { it.trending }.take(24))
        list.distinctBy { it.id }
    }

    val previewTintColor = remember(selectedFilter) {
        if (selectedFilter.overlayAlpha > 0.01f && selectedFilter.overlayColorArgb != 0L) {
            Color(selectedFilter.overlayColorArgb).copy(alpha = selectedFilter.overlayAlpha)
        } else {
            Color.Transparent
        }
    }

    val previewScale by animateFloatAsState(
        targetValue = when (selectedFilter.distortionType) {
            DistortionType.BIG_HEAD -> 1.14f
            DistortionType.SMALL_FACE -> 0.88f
            else -> 1f
        },
        label = "distortion_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // 1. Live CameraX Preview OR Permission Gate
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(previewScale)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            controller = cameraController
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(SnapYellow.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = SnapYellow,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Access Needed",
                    style = MaterialTheme.typography.headlineMedium.copy(color = PureWhite)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Allow Camera permission to capture photos & videos with 1,076+ live filters, or import directly from your gallery.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.72f))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
                    modifier = Modifier.testTag("grant_camera_button")
                ) {
                    Text("Enable Camera", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Live Filter Visual Overlay (Tint, Vignette, Distortion FX)
        LiveFilterVisualOverlay(
            filter = selectedFilter,
            tintColor = previewTintColor,
            brightnessOffset = brightnessLevel,
            beautyLevel = beautyLevel
        )

        // 3. Top Camera Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianBlack.copy(alpha = 0.58f),
                modifier = Modifier
                    .clickable { onOpenSavedSnaps() }
                    .testTag("open_saved_snaps_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "My Saved Snaps", tint = SnapYellow, modifier = Modifier.size(18.dp))
                    Text("MY SAVED SNAPS", style = MaterialTheme.typography.labelLarge.copy(color = PureWhite))
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ObsidianBlack.copy(alpha = 0.58f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            modifier = Modifier.testTag("switch_camera_button")
                        ) {
                            Icon(Icons.Filled.FlipCameraAndroid, contentDescription = "Switch camera", tint = PureWhite)
                        }
                        IconButton(
                            onClick = { flashOn = !flashOn },
                            modifier = Modifier.testTag("flash_toggle_button")
                        ) {
                            Icon(
                                if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = "Flash",
                                tint = if (flashOn) SnapYellow else PureWhite
                            )
                        }
                        IconButton(
                            onClick = {
                                timerSeconds = when (timerSeconds) {
                                    0 -> 3
                                    3 -> 5
                                    5 -> 10
                                    else -> 0
                                }
                            },
                            modifier = Modifier.testTag("timer_toggle_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = "Timer",
                                    tint = if (timerSeconds > 0) SnapYellow else PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (timerSeconds > 0) {
                                    Text("${timerSeconds}s", fontSize = 10.sp, color = SnapYellow, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        IconButton(
                            onClick = { hdrEnabled = !hdrEnabled },
                            modifier = Modifier.testTag("hdr_toggle_button")
                        ) {
                            Icon(
                                Icons.Filled.HdrOn,
                                contentDescription = "HDR mode",
                                tint = if (hdrEnabled) NeonCyan else PureWhite.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(
                            onClick = { showAdjustPanel = !showAdjustPanel },
                            modifier = Modifier.testTag("beauty_brightness_button")
                        ) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = "Beauty & Brightness",
                                tint = if (showAdjustPanel) SnapYellow else PureWhite
                            )
                        }
                    }
                }
            }
        }

        // Countdown Overlay
        if (countdownRemaining > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    shape = CircleShape,
                    color = ObsidianBlack.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "$countdownRemaining",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, color = SnapYellow),
                        modifier = Modifier.padding(36.dp)
                    )
                }
            }
        }

        // Beauty, Brightness & Exposure Slider Panel
        AnimatedVisibility(
            visible = showAdjustPanel,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, end = 76.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianBlack.copy(alpha = 0.78f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.FaceRetouchingNatural, contentDescription = null, tint = SnapYellow, modifier = Modifier.size(18.dp))
                        Text("Beauty & Soft Skin", style = MaterialTheme.typography.labelLarge.copy(color = PureWhite))
                    }
                    Slider(
                        value = beautyLevel,
                        onValueChange = { beautyLevel = it },
                        colors = SliderDefaults.colors(thumbColor = SnapYellow, activeTrackColor = SnapYellow)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.BrightnessHigh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Text("Brightness & Exposure", style = MaterialTheme.typography.labelLarge.copy(color = PureWhite))
                    }
                    Slider(
                        value = brightnessLevel,
                        onValueChange = {
                            brightnessLevel = it
                            exposureIndex = (it * 4f).toInt().coerceIn(-4, 4)
                        },
                        valueRange = -0.6f..0.6f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }
            }
        }

        // 4. Bottom Camera Controls: Zoom + Active Filter Pill + Shutter + 1000+ Filters Strip
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            // Zoom Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1f, 2f, 3f, 5f).forEach { z ->
                    val active = zoomRatio == z
                    Surface(
                        shape = CircleShape,
                        color = if (active) SnapYellow else ObsidianBlack.copy(alpha = 0.55f),
                        modifier = Modifier.clickable { zoomRatio = z }
                    ) {
                        Text(
                            text = "${z.toInt()}x",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (active) ObsidianBlack else PureWhite,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Filter Pill + All 1076 Filters Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianBlack.copy(alpha = 0.72f),
                    modifier = Modifier
                        .clickable { showAllFiltersSheet = true }
                        .testTag("open_all_filters_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SnapYellow, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${selectedFilter.name} • Browse ${allFilters.size} Filters",
                            style = MaterialTheme.typography.labelLarge.copy(color = PureWhite),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Filter Carousel
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickStripFilters, key = { it.id }) { filter ->
                    val isSelected = filter.id == selectedFilter.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clickable {
                                selectedFilter = filter
                                onFilterUsed(filter)
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(if (isSelected) 54.dp else 46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(filter.accentColorArgb),
                                            ObsidianBlack
                                        )
                                    )
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) SnapYellow else PureWhite.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = filter.name.take(2).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(color = PureWhite, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = filter.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) SnapYellow else PureWhite
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shutter Row: Gallery Import | Capture / Video Record | Photo/Video Mode Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(ObsidianBlack.copy(alpha = 0.62f))
                        .testTag("gallery_import_button")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Import from Gallery", tint = PureWhite)
                }

                // Primary Shutter Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) SnapCoral else Color.Transparent
                        )
                        .border(
                            width = 5.dp,
                            color = if (isVideoMode) SnapCoral else SnapYellow,
                            shape = CircleShape
                        )
                        .clickable { triggerShutter() }
                        .testTag("camera_shutter_button")
                ) {
                    if (isRecording) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop recording", tint = PureWhite, modifier = Modifier.size(34.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(if (isVideoMode) SnapCoral else PureWhite)
                        )
                    }
                }

                IconButton(
                    onClick = { isVideoMode = !isVideoMode },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isVideoMode) SnapCoral else ObsidianBlack.copy(alpha = 0.62f))
                        .testTag("photo_video_mode_button")
                ) {
                    Icon(
                        if (isVideoMode) Icons.Filled.Videocam else Icons.Filled.CameraAlt,
                        contentDescription = "Toggle Photo/Video mode",
                        tint = PureWhite
                    )
                }
            }
        }
    }

    if (showAllFiltersSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAllFiltersSheet = false },
            sheetState = sheetState,
            containerColor = CharcoalSurface
        ) {
            AllFiltersBrowserSheet(
                allFilters = allFilters,
                selectedFilter = selectedFilter,
                favoriteIds = favoriteIds,
                recentIds = recentIds,
                onSelectFilter = {
                    selectedFilter = it
                    onFilterUsed(it)
                    showAllFiltersSheet = false
                },
                onToggleFavorite = onToggleFavoriteFilter,
                onClose = { showAllFiltersSheet = false }
            )
        }
    }
}

private var selectedFilterGlobal: SnapFilter = FilterCatalog.originalFilter

@Composable
private fun LiveFilterVisualOverlay(
    filter: SnapFilter,
    tintColor: Color,
    brightnessOffset: Float,
    beautyLevel: Float
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (tintColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tintColor)
            )
        }
        if (brightnessOffset > 0.05f || beautyLevel > 0.3f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureWhite.copy(alpha = (brightnessOffset.coerceAtLeast(0f) * 0.25f) + (beautyLevel * 0.08f)))
            )
        } else if (brightnessOffset < -0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack.copy(alpha = (-brightnessOffset * 0.45f).coerceIn(0f, 0.55f)))
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (filter.vignetteStrength > 0.08f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            ObsidianBlack.copy(alpha = (filter.vignetteStrength * 0.7f).coerceIn(0f, 0.75f))
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.75f
                    )
                )
            }
            when (filter.distortionType) {
                DistortionType.NEON_GLOW -> {
                    drawRect(
                        color = Color(filter.accentColorArgb),
                        style = Stroke(width = 14f)
                    )
                }
                DistortionType.CHROMA_3D -> {
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.25f),
                        radius = size.minDimension * 0.36f,
                        center = Offset(size.width * 0.48f, size.height * 0.48f),
                        style = Stroke(width = 8f)
                    )
                    drawCircle(
                        color = SnapCoral.copy(alpha = 0.25f),
                        radius = size.minDimension * 0.36f,
                        center = Offset(size.width * 0.52f, size.height * 0.50f),
                        style = Stroke(width = 8f)
                    )
                }
                DistortionType.RAIN_DROPS -> {
                    for (i in 0..18) {
                        val x = ((i * 73) % 100) / 100f * size.width
                        val y = ((i * 137) % 100) / 100f * size.height
                        drawLine(
                            color = PureWhite.copy(alpha = 0.28f),
                            start = Offset(x, y),
                            end = Offset(x - 16f, y + 54f),
                            strokeWidth = 3f
                        )
                    }
                }
                DistortionType.BOKEH_LIGHT, DistortionType.FESTIVAL_DIWALI -> {
                    for (i in 0..10) {
                        val x = ((i * 97 + 15) % 100) / 100f * size.width
                        val y = ((i * 61 + 10) % 100) / 100f * size.height
                        drawCircle(
                            color = Color(filter.accentColorArgb).copy(alpha = 0.24f),
                            radius = 28f + (i % 4) * 18f,
                            center = Offset(x, y)
                        )
                    }
                }
                DistortionType.FOG_MIST -> {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                PureWhite.copy(alpha = 0.22f),
                                Color.Transparent,
                                PureWhite.copy(alpha = 0.28f)
                            )
                        )
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun AllFiltersBrowserSheet(
    allFilters: List<SnapFilter>,
    selectedFilter: SnapFilter,
    favoriteIds: Set<String>,
    recentIds: List<String>,
    onSelectFilter: (SnapFilter) -> Unit,
    onToggleFavorite: (SnapFilter) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("all") } // "all", "favorites", "recent"
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(allFilters, searchQuery, activeTab, selectedCategoryId, favoriteIds, recentIds) {
        val base = when (activeTab) {
            "favorites" -> allFilters.filter { it.id in favoriteIds }
            "recent" -> {
                val map = allFilters.associateBy { it.id }
                recentIds.mapNotNull { map[it] }
            }
            else -> {
                if (selectedCategoryId == null) allFilters
                else allFilters.filter { it.categoryId == selectedCategoryId }
            }
        }
        if (searchQuery.isBlank()) base
        else base.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.categoryTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Filters Studio (${allFilters.size} Effects)",
                    style = MaterialTheme.typography.titleLarge.copy(color = PureWhite)
                )
                Text(
                    text = "${FilterCatalog.categories.size} Categories • Search, Favorites & Recent",
                    style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.65f))
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = PureWhite)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search 1000+ filters (e.g. Golden Hour, Dog, iPhone, Neon)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("filter_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = activeTab == "all",
                onClick = { activeTab = "all" },
                label = { Text("All (${allFilters.size})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SnapYellow, selectedLabelColor = ObsidianBlack)
            )
            FilterChip(
                selected = activeTab == "favorites",
                onClick = { activeTab = "favorites" },
                leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Saved (${favoriteIds.size})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SnapYellow, selectedLabelColor = ObsidianBlack)
            )
            FilterChip(
                selected = activeTab == "recent",
                onClick = { activeTab = "recent" },
                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Recent (${recentIds.size})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SnapYellow, selectedLabelColor = ObsidianBlack)
            )
        }

        if (activeTab == "all") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text("All Categories") }
                )
                FilterCatalog.categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text("${cat.emoji} ${cat.title}") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No matching filters yet. Tap the heart on any filter to save it!",
                    style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.65f))
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { filter ->
                    val isSelected = filter.id == selectedFilter.id
                    val isFav = filter.id in favoriteIds
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CharcoalElevated,
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) SnapYellow else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectFilter(filter) }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(filter.accentColorArgb),
                                                if (filter.overlayColorArgb != 0L) Color(filter.overlayColorArgb) else ObsidianBlack
                                            )
                                        )
                                    )
                            ) {
                                Text(
                                    text = filter.categoryTitle.take(10),
                                    style = MaterialTheme.typography.labelSmall.copy(color = PureWhite, fontSize = 9.sp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(5.dp)
                                )
                                IconButton(
                                    onClick = { onToggleFavorite(filter) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Favorite filter",
                                        tint = if (isFav) SnapCoral else PureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = filter.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) SnapYellow else PureWhite,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
