package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.PrivateMediaStorage
import com.example.data.model.SimulatedFriendProfile
import com.example.data.model.UiState
import com.example.data.model.UserStory
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnapCoral
import com.example.ui.theme.SnapYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private sealed interface ActiveViewerStory {
    data class MyStory(val story: UserStory) : ActiveViewerStory
    data class FriendStory(val friend: SimulatedFriendProfile) : ActiveViewerStory
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesScreen(
    storiesState: UiState<List<UserStory>>,
    friends: List<SimulatedFriendProfile>,
    onCreateStory: (String, String, String, String, String, String, String?) -> Unit,
    onIncrementStoryView: (String) -> Unit,
    onDeleteStory: (String) -> Unit,
    onReplyToFriendStory: (SimulatedFriendProfile, String) -> Unit,
    onOpenCameraTab: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCreateSheet by remember { mutableStateOf(false) }
    var activeViewer by remember { mutableStateOf<ActiveViewerStory?>(null) }

    val galleryStoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val mime = context.contentResolver.getType(uri).orEmpty()
                val isVid = mime.startsWith("video")
                val file = PrivateMediaStorage.copyUriToPrivateFile(context, uri, if (isVid) "mp4" else "jpg")
                if (file != null) {
                    onCreateStory(
                        if (isVid) "video" else "photo",
                        "Shared from Gallery",
                        "Original",
                        "",
                        "friends",
                        "#14141B",
                        file.absolutePath
                    )
                }
            }
        }
    }

    val myStories = (storiesState as? UiState.Success)?.data.orEmpty()
    val featuredFriendStories = remember(friends) { friends.take(30) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Stories • 24 Hours", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Stories automatically expire after 24 hours",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Button(
                onClick = { showCreateSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("create_story_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Story", fontWeight = FontWeight.Bold)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // My Stories Horizontal Strip spanning 2 columns
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column {
                    Text("My 24h Stories (${myStories.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(150.dp)
                                    .clickable { showCreateSheet = true }
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxSize().padding(10.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(SnapYellow)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Create Story", tint = ObsidianBlack)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Create Story", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        items(myStories, key = { it.id }) { story ->
                            val bgColor = runCatching {
                                Color(android.graphics.Color.parseColor(story.bgColorHex))
                            }.getOrElse { ObsidianBlack }
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(150.dp)
                                    .border(2.dp, SnapYellow, RoundedCornerShape(18.dp))
                                    .clickable {
                                        onIncrementStoryView(story.id)
                                        activeViewer = ActiveViewerStory.MyStory(story)
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                                    Text(
                                        text = story.caption.ifBlank { story.storyType.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite, fontWeight = FontWeight.Bold),
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    ) {
                                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = SnapYellow, modifier = Modifier.size(14.dp))
                                        Text("${story.viewCount}", style = MaterialTheme.typography.labelSmall.copy(color = PureWhite))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Friends Stories (AI Sim)", style = MaterialTheme.typography.titleMedium)
                }
            }

            items(featuredFriendStories, key = { it.id }) { friend ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clickable { activeViewer = ActiveViewerStory.FriendStory(friend) }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_story_backdrop),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(friend.storyBgHex).copy(alpha = 0.55f),
                                            ObsidianBlack.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(friend.avatarColorArgb))
                                    .border(2.dp, SnapYellow, CircleShape)
                            ) {
                                Text(friend.name.take(1), fontWeight = FontWeight.Bold, color = ObsidianBlack)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            AiSimPill()
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(friend.storyThemeEmoji, fontSize = 24.sp)
                            Text(friend.name, style = MaterialTheme.typography.titleMedium.copy(color = PureWhite))
                            Text(
                                text = friend.storyCaption,
                                style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.8f)),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(onDismissRequest = { showCreateSheet = false }, containerColor = CharcoalSurface) {
            CreateStorySheet(
                onCreateTextStory = { caption, music, privacy, bgHex ->
                    onCreateStory("text", caption, "Original", music, privacy, bgHex, null)
                    showCreateSheet = false
                },
                onPickFromGallery = {
                    showCreateSheet = false
                    galleryStoryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onOpenCamera = {
                    showCreateSheet = false
                    onOpenCameraTab()
                }
            )
        }
    }

    activeViewer?.let { current ->
        StoryViewerDialog(
            story = current,
            onDismiss = { activeViewer = null },
            onDeleteMyStory = { id ->
                onDeleteStory(id)
                activeViewer = null
            },
            onReply = { friend, reply ->
                onReplyToFriendStory(friend, reply)
                activeViewer = null
            }
        )
    }
}

@Composable
private fun CreateStorySheet(
    onCreateTextStory: (String, String, String, String) -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenCamera: () -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var music by remember { mutableStateOf("Kesariya Lo-Fi") }
    var privacy by remember { mutableStateOf("friends") }
    var bgHex by remember { mutableStateOf("#14141B") }

    Column(modifier = Modifier.padding(18.dp)) {
        Text("Create 24h Story", style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenCamera, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Camera")
            }
            OutlinedButton(onClick = onPickFromGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gallery")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("Or Post a Text Story", style = MaterialTheme.typography.titleMedium.copy(color = SnapYellow))
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it.take(280) },
            placeholder = { Text("Share what's happening right now...") },
            leadingIcon = { Icon(Icons.Filled.TextFields, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("friends" to "Friends Only", "close_friends" to "Close Friends", "private" to "Private").forEach { (k, label) ->
                FilterChip(selected = privacy == k, onClick = { privacy = k }, label = { Text(label) })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("#14141B" to "Obsidian", "#D81B60" to "Festive", "#1565C0" to "Ocean", "#E65100" to "Sunset").forEach { (hex, label) ->
                FilterChip(selected = bgHex == hex, onClick = { bgHex = hex }, label = { Text(label) })
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                if (caption.isNotBlank()) onCreateTextStory(caption, music, privacy, bgHex)
            },
            enabled = caption.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Publish 24h Story", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StoryViewerDialog(
    story: ActiveViewerStory,
    onDismiss: () -> Unit,
    onDeleteMyStory: (String) -> Unit,
    onReply: (SimulatedFriendProfile, String) -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var replyText by remember { mutableStateOf("") }
    var selectedReaction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(story) {
        while (progress < 1f) {
            delay(60L)
            progress += 0.012f
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
                .statusBarsPadding()
        ) {
            when (story) {
                is ActiveViewerStory.MyStory -> {
                    val path = story.story.localMediaPath
                    val bmp = remember(path) {
                        if (!path.isNullOrBlank() && File(path).exists()) BitmapFactory.decodeFile(path) else null
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Story photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val bg = runCatching { Color(android.graphics.Color.parseColor(story.story.bgColorHex)) }
                            .getOrElse { CharcoalSurface }
                        Box(modifier = Modifier.fillMaxSize().background(bg))
                    }
                }
                is ActiveViewerStory.FriendStory -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color(story.friend.storyBgHex), ObsidianBlack)))
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = SnapYellow,
                    trackColor = PureWhite.copy(alpha = 0.28f),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (story) {
                        is ActiveViewerStory.MyStory -> {
                            Column {
                                Text(story.story.authorName, style = MaterialTheme.typography.titleMedium.copy(color = PureWhite))
                                Text(
                                    "${story.story.viewCount} views • ${story.story.privacy}",
                                    style = MaterialTheme.typography.labelMedium.copy(color = SnapYellow)
                                )
                            }
                            Row {
                                IconButton(onClick = { onDeleteMyStory(story.story.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete story", tint = SnapCoral)
                                }
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = PureWhite)
                                }
                            }
                        }
                        is ActiveViewerStory.FriendStory -> {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(story.friend.name, style = MaterialTheme.typography.titleMedium.copy(color = PureWhite))
                                    AiSimPill()
                                }
                                Text("${story.friend.city} • 24h Story", style = MaterialTheme.typography.labelMedium.copy(color = PureWhite.copy(alpha = 0.7f)))
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = PureWhite)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                when (story) {
                    is ActiveViewerStory.MyStory -> {
                        Surface(shape = RoundedCornerShape(16.dp), color = ObsidianBlack.copy(alpha = 0.65f)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(story.story.caption, style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
                                if (story.story.musicTrack.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = SnapYellow, modifier = Modifier.size(16.dp))
                                        Text(story.story.musicTrack, style = MaterialTheme.typography.bodyMedium.copy(color = SnapYellow))
                                    }
                                }
                            }
                        }
                    }
                    is ActiveViewerStory.FriendStory -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(story.friend.storyThemeEmoji, fontSize = 72.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(story.friend.storyCaption, style = MaterialTheme.typography.headlineMedium.copy(color = PureWhite))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        selectedReaction?.let {
                            Text("Reacted $it", style = MaterialTheme.typography.labelLarge.copy(color = SnapYellow))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                            listOf("❤️", "🔥", "😂", "👏").forEach { emoji ->
                                Surface(
                                    shape = CircleShape,
                                    color = CharcoalElevated,
                                    modifier = Modifier.clickable {
                                        selectedReaction = emoji
                                        onReply(story.friend, "Reacted $emoji to your story")
                                    }
                                ) {
                                    Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(10.dp))
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text("Reply to ${story.friend.name}'s story...") },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                if (replyText.isNotBlank()) {
                                    onReply(story.friend, "Story reply: $replyText")
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply", tint = SnapYellow)
                            }
                        }
                    }
                }
            }
        }
    }
}
