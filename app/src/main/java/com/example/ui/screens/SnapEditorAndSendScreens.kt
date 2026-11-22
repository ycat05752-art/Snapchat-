package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SimulatedFriendProfile
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.MintOnline
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnapCoral
import com.example.ui.theme.SnapGold
import com.example.ui.theme.SnapYellow
import com.example.ui.viewmodel.CapturedSnapDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private data class DrawStroke(val points: List<Offset>, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapEditorScreen(
    draft: CapturedSnapDraft,
    friends: List<SimulatedFriendProfile>,
    onBack: () -> Unit,
    onSaveToMySavedSnaps: (Bitmap?, String, String, String, String, Boolean) -> Unit,
    onPostToStory: (String, String, String, String, String, String?) -> Unit,
    onSendToFriends: (List<SimulatedFriendProfile>, String, String, String, Boolean, Boolean, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val baseBitmap = remember(draft.filePath, draft.mediaType) {
        if (draft.mediaType == "photo") {
            BitmapFactory.decodeFile(draft.filePath)
        } else null
    }

    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var cropMode by remember { mutableStateOf("Full") } // Full, 1:1, 4:5, 9:16
    var overlayText by remember { mutableStateOf("") }
    var selectedSticker by remember { mutableStateOf<String?>(null) }
    var selectedMusic by remember { mutableStateOf<String?>(null) }
    var drawMode by remember { mutableStateOf(false) }
    var brushColor by remember { mutableStateOf(SnapYellow) }
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var showTextSheet by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }
    var showSendSheet by remember { mutableStateOf(false) }

    val transformedBitmap = remember(baseBitmap, rotationAngle, cropMode) {
        baseBitmap?.let { src ->
            val rotMat = Matrix().apply { if (rotationAngle != 0f) postRotate(rotationAngle) }
            val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, rotMat, true)
            when (cropMode) {
                "1:1" -> {
                    val side = minOf(rotated.width, rotated.height)
                    Bitmap.createBitmap(rotated, (rotated.width - side) / 2, (rotated.height - side) / 2, side, side)
                }
                "4:5" -> {
                    val w = rotated.width
                    val h = (w * 1.25f).toInt().coerceAtMost(rotated.height)
                    Bitmap.createBitmap(rotated, 0, (rotated.height - h) / 2, w, h)
                }
                else -> rotated
            }
        }
    }

    fun renderFinalBitmap(): Bitmap? {
        val src = transformedBitmap ?: return null
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(out)
        if (overlayText.isNotBlank()) {
            val boxPaint = Paint().apply { color = android.graphics.Color.argb(170, 0, 0, 0) }
            val top = out.height * 0.44f
            canvas.drawRect(0f, top, out.width.toFloat(), top + out.height * 0.1f, boxPaint)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = (out.width * 0.055f).coerceAtLeast(32f)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(overlayText, out.width / 2f, top + out.height * 0.068f, textPaint)
        }
        selectedSticker?.let { sticker ->
            val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = (out.width * 0.14f).coerceAtLeast(64f)
            }
            canvas.drawText(sticker, out.width * 0.72f, out.height * 0.25f, stickerPaint)
        }
        selectedMusic?.let { track ->
            val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SnapYellow.toArgb()
                textSize = (out.width * 0.036f).coerceAtLeast(24f)
            }
            canvas.drawText("🎵 $track", out.width * 0.06f, out.height * 0.90f, mPaint)
        }
        return out
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Canvas / Preview Area
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 86.dp)
        ) {
            if (transformedBitmap != null) {
                Image(
                    bitmap = transformedBitmap.asImageBitmap(),
                    contentDescription = "Captured snap preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .let {
                            if (cropMode == "1:1") it.aspectRatio(1f)
                            else if (cropMode == "4:5") it.aspectRatio(0.8f)
                            else it.fillMaxSize()
                        }
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CharcoalSurface,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(340.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🎥 Video Snap Ready", style = MaterialTheme.typography.headlineMedium.copy(color = SnapYellow))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Filter: ${draft.filter.name}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite)
                        )
                    }
                }
            }

            // Finger Drawing Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (drawMode) {
                            Modifier.pointerInput(brushColor) {
                                detectDragGestures(
                                    onDragStart = { offset -> currentStroke = listOf(offset) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentStroke = currentStroke + change.position
                                    },
                                    onDragEnd = {
                                        if (currentStroke.isNotEmpty()) {
                                            strokes.add(DrawStroke(currentStroke, brushColor))
                                            currentStroke = emptyList()
                                        }
                                    }
                                )
                            }
                        } else Modifier
                    )
            ) {
                (strokes + DrawStroke(currentStroke, brushColor)).forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke.points.first().x, stroke.points.first().y)
                            stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = path,
                            color = stroke.color,
                            style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            // Text Banner Overlay
            if (overlayText.isNotBlank()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianBlack.copy(alpha = 0.65f))
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = overlayText,
                        style = MaterialTheme.typography.titleLarge.copy(color = PureWhite)
                    )
                }
            }

            // Sticker Overlay
            selectedSticker?.let { sticker ->
                Text(
                    text = sticker,
                    fontSize = 54.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 84.dp, end = 28.dp)
                )
            }

            // Music Pill Overlay
            selectedMusic?.let { track ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianBlack.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = SnapYellow, modifier = Modifier.size(16.dp))
                        Text(track, style = MaterialTheme.typography.labelLarge.copy(color = PureWhite))
                    }
                }
            }
        }

        // Top Editor Bar (Back + Editing Tools)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ObsidianBlack.copy(alpha = 0.6f))
                    .testTag("editor_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to camera", tint = PureWhite)
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianBlack.copy(alpha = 0.65f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    IconButton(onClick = { showTextSheet = true }) {
                        Icon(Icons.Filled.TextFields, contentDescription = "Add Text", tint = PureWhite)
                    }
                    IconButton(onClick = { showStickerSheet = true }) {
                        Icon(Icons.Filled.EmojiEmotions, contentDescription = "Stickers & Emoji", tint = PureWhite)
                    }
                    IconButton(onClick = { drawMode = !drawMode }) {
                        Icon(Icons.Filled.Brush, contentDescription = "Draw", tint = if (drawMode) SnapYellow else PureWhite)
                    }
                    if (drawMode && strokes.isNotEmpty()) {
                        IconButton(onClick = { strokes.removeAt(strokes.lastIndex) }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo stroke", tint = PureWhite)
                        }
                    }
                    IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90 degrees", tint = PureWhite)
                    }
                    IconButton(onClick = {
                        cropMode = when (cropMode) {
                            "Full" -> "1:1"
                            "1:1" -> "4:5"
                            else -> "Full"
                        }
                    }) {
                        Icon(Icons.Filled.Crop, contentDescription = "Crop ratio", tint = if (cropMode != "Full") SnapYellow else PureWhite)
                    }
                    IconButton(onClick = { showMusicSheet = true }) {
                        Icon(Icons.Filled.MusicNote, contentDescription = "Add Music", tint = if (selectedMusic != null) SnapYellow else PureWhite)
                    }
                }
            }
        }

        // Brush Palette if Drawing
        if (drawMode) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 92.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ObsidianBlack.copy(alpha = 0.78f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                listOf(SnapYellow, PureWhite, NeonCyan, SnapCoral, MintOnline, Color(0xFF7C4DFF)).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(if (brushColor == c) 2.5.dp else 0.dp, PureWhite, CircleShape)
                            .clickable { brushColor = c }
                    )
                }
            }
        }

        // Bottom Editor Actions: Save | Post Story | Send To ->
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ObsidianBlack.copy(alpha = 0.85f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CharcoalElevated,
                    modifier = Modifier
                        .clickable {
                            onSaveToMySavedSnaps(
                                renderFinalBitmap(),
                                draft.mediaType,
                                draft.filePath,
                                overlayText,
                                draft.filter.name,
                                draft.filter.id != "original"
                            )
                        }
                        .testTag("save_to_gallery_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = SnapYellow, modifier = Modifier.size(18.dp))
                        Text("Save", style = MaterialTheme.typography.labelLarge.copy(color = PureWhite))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CharcoalElevated,
                    modifier = Modifier
                        .clickable {
                            onPostToStory(
                                draft.mediaType,
                                overlayText.ifBlank { "Captured with ${draft.filter.name}" },
                                draft.filter.name,
                                selectedMusic ?: "",
                                "friends",
                                draft.filePath
                            )
                        }
                        .testTag("post_to_story_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.AutoStories, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Text("Story", style = MaterialTheme.typography.labelLarge.copy(color = PureWhite))
                    }
                }
            }

            Button(
                onClick = { showSendSheet = true },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
                modifier = Modifier.testTag("editor_send_to_button")
            ) {
                Text("Send To", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showTextSheet) {
        ModalBottomSheet(onDismissRequest = { showTextSheet = false }, containerColor = CharcoalSurface) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Add Snap Caption", style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = overlayText,
                    onValueChange = { overlayText = it.take(120) },
                    placeholder = { Text("Type caption or Hinglish vibe...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showTextSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Text", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showStickerSheet) {
        ModalBottomSheet(onDismissRequest = { showStickerSheet = false }, containerColor = CharcoalSurface) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Emojis & Indian Stickers", style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
                Spacer(modifier = Modifier.height(12.dp))
                val stickers = listOf("🔥", "😂", "❤️", "🪔", "☕", "🏏", "👑", "😎", "🇮🇳", "🎉", "💯", "🙏", "✨", "🥳")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    stickers.forEach { st ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CharcoalElevated,
                            modifier = Modifier.clickable {
                                selectedSticker = st
                                showStickerSheet = false
                            }
                        ) {
                            Text(st, fontSize = 32.sp, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showMusicSheet) {
        ModalBottomSheet(onDismissRequest = { showMusicSheet = false }, containerColor = CharcoalSurface) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Add Music Vibe", style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
                Spacer(modifier = Modifier.height(10.dp))
                val tracks = listOf(
                    "Kesariya Lo-Fi Chill" to 440.0,
                    "Monsoon Chai Acoustic" to 523.25,
                    "Mumbai Midnight Synth" to 587.33,
                    "Desi Dhol Celebration" to 659.25,
                    "Golden Hour Guitar" to 493.88
                )
                tracks.forEach { (title, freq) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CharcoalElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedMusic = title
                                scope.launch(Dispatchers.Default) { playShortMusicChime(freq) }
                                showMusicSheet = false
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = SnapYellow)
                            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = PureWhite))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSendSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSendSheet = false },
            sheetState = sheetState,
            containerColor = CharcoalSurface
        ) {
            SendSnapToFriendsSheet(
                friends = friends,
                initialCaption = overlayText,
                filterName = draft.filter.name,
                mediaType = draft.mediaType,
                onConfirmSend = { selectedFriends, caption, viewOnce, replayAllowed, timer ->
                    onSendToFriends(
                        selectedFriends,
                        caption,
                        draft.filter.name,
                        draft.mediaType,
                        viewOnce,
                        replayAllowed,
                        timer
                    )
                    showSendSheet = false
                    onBack()
                }
            )
        }
    }
}

@Composable
fun SendSnapToFriendsSheet(
    friends: List<SimulatedFriendProfile>,
    initialCaption: String,
    filterName: String,
    mediaType: String,
    onConfirmSend: (List<SimulatedFriendProfile>, String, Boolean, Boolean, Int) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf(initialCaption) }
    var viewOnce by remember { mutableStateOf(true) }
    var replayAllowed by remember { mutableStateOf(true) }
    var timerSec by remember { mutableIntStateOf(5) }
    val selectedIds = remember { mutableStateListOf<String>() }

    val filteredFriends = remember(friends, search) {
        if (search.isBlank()) friends.take(80)
        else friends.filter {
            it.name.contains(search, ignoreCase = true) ||
                it.username.contains(search, ignoreCase = true)
        }.take(80)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(640.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Send Snap To", style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
                Text(
                    text = "+1000 PN Points per Snap Sent! (${selectedIds.size * 1000} PN)",
                    style = MaterialTheme.typography.labelLarge.copy(color = SnapGold)
                )
            }
            Surface(shape = RoundedCornerShape(12.dp), color = SnapYellow) {
                Text(
                    text = mediaType.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(color = ObsidianBlack, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it.take(180) },
            placeholder = { Text("Add snap caption or emoji...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = viewOnce,
                onClick = { viewOnce = !viewOnce },
                label = { Text(if (viewOnce) "View Once: ON" else "View Once: OFF") }
            )
            FilterChip(
                selected = replayAllowed,
                onClick = { replayAllowed = !replayAllowed },
                label = { Text(if (replayAllowed) "Replay: ON" else "Replay: OFF") }
            )
            listOf(3, 5, 10).forEach { sec ->
                FilterChip(
                    selected = timerSec == sec,
                    onClick = { timerSec = sec },
                    label = { Text("${sec}s Timer") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search 560+ Indian friends...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(filteredFriends, key = { it.id }) { friend ->
                val checked = friend.id in selectedIds
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (checked) SnapYellow.copy(alpha = 0.14f) else CharcoalElevated
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (checked) selectedIds.remove(friend.id) else selectedIds.add(friend.id)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(friend.avatarColorArgb))
                        ) {
                            Text(
                                text = friend.name.take(1),
                                style = MaterialTheme.typography.titleMedium.copy(color = ObsidianBlack, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(friend.name, style = MaterialTheme.typography.titleMedium.copy(color = PureWhite))
                            Text("${friend.city} • AI Sim", style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.6f)))
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                if (checked) selectedIds.remove(friend.id) else selectedIds.add(friend.id)
                            }
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val chosen = friends.filter { it.id in selectedIds }
                onConfirmSend(chosen, caption, viewOnce, replayAllowed, timerSec)
            },
            enabled = selectedIds.isNotEmpty(),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(52.dp)
                .testTag("confirm_send_snap_button")
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedIds.isEmpty()) "Select Friends to Send"
                else "Send Snap (${selectedIds.size}) • +${selectedIds.size * 1000} PN",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun playShortMusicChime(freqHz: Double) {
    try {
        val sampleRate = 22050
        val count = sampleRate / 2
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val env = 1f - (i.toFloat() / count)
            val s = sin(2.0 * PI * i * freqHz / sampleRate) * env
            samples[i] = (s * 8000).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        Thread.sleep(550)
        track.release()
    } catch (_: Exception) {
    }
}
