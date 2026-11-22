package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.local.IncomingSimSnapEntity
import com.example.data.local.PrivateMediaStorage
import com.example.data.model.ChatMessage
import com.example.data.model.FriendState
import com.example.data.model.SimulatedFriendProfile
import com.example.data.model.UiState
import com.example.ui.theme.MintOnline
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnapCoral
import com.example.ui.theme.SnapYellow
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ChatScreen(
    friends: List<SimulatedFriendProfile>,
    friendStates: List<FriendState>,
    incomingSnaps: List<IncomingSimSnapEntity>,
    activeChatFriendId: String?,
    activeMessagesState: UiState<List<ChatMessage>>,
    friendTypingId: String?,
    aiChatError: String?,
    onOpenChat: (String?) -> Unit,
    onSendChatMessage: (SimulatedFriendProfile, String, String, String?) -> Unit,
    onReactToMessage: (String, String, String) -> Unit,
    onOpenIncomingSnap: (String) -> Unit,
    onReplayIncomingSnap: (String) -> Unit,
    onTriggerSimSnap: () -> Unit
) {
    val activeFriend = remember(activeChatFriendId, friends) {
        friends.firstOrNull { it.id == activeChatFriendId }
    }

    if (activeFriend != null) {
        BackHandler { onOpenChat(null) }
        ChatDetailScreen(
            friend = activeFriend,
            messagesState = activeMessagesState,
            isTyping = friendTypingId == activeFriend.id,
            aiError = aiChatError,
            onBack = { onOpenChat(null) },
            onSend = { type, text, mediaUri -> onSendChatMessage(activeFriend, type, text, mediaUri) },
            onReact = { msgId, emoji -> onReactToMessage(activeFriend.id, msgId, emoji) }
        )
    } else {
        ChatInboxScreen(
            friends = friends,
            friendStates = friendStates,
            incomingSnaps = incomingSnaps,
            onSelectFriend = { onOpenChat(it.id) },
            onOpenIncomingSnap = onOpenIncomingSnap,
            onReplayIncomingSnap = onReplayIncomingSnap,
            onTriggerSimSnap = onTriggerSimSnap
        )
    }
}

@Composable
private fun ChatInboxScreen(
    friends: List<SimulatedFriendProfile>,
    friendStates: List<FriendState>,
    incomingSnaps: List<IncomingSimSnapEntity>,
    onSelectFriend: (SimulatedFriendProfile) -> Unit,
    onOpenIncomingSnap: (String) -> Unit,
    onReplayIncomingSnap: (String) -> Unit,
    onTriggerSimSnap: () -> Unit
) {
    val stateMap = remember(friendStates) { friendStates.associateBy { it.friendId } }
    val chatFriends = remember(friends, stateMap) {
        friends.filter { f ->
            val st = stateMap[f.id]?.relationStatus
            st == "friend" || (st == null && f.isDefaultFriend)
        }
    }

    var viewingSnap by remember { mutableStateOf<IncomingSimSnapEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Chat & Incoming Snaps", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "AI Simulated Indian Friends & Live Replies",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            IconButton(
                onClick = onTriggerSimSnap,
                modifier = Modifier.testTag("trigger_incoming_snap_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Simulate incoming snap", tint = SnapYellow)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (incomingSnaps.isNotEmpty()) {
                item {
                    Text(
                        text = "Incoming Simulated Snaps (${incomingSnaps.count { !it.opened }} Unopened)",
                        style = MaterialTheme.typography.labelLarge.copy(color = SnapCoral)
                    )
                }
                items(incomingSnaps.take(6), key = { it.id }) { snap ->
                    val lockedExpired = snap.opened && snap.viewOnce && (!snap.replayAllowed || snap.replayed)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !lockedExpired) {
                                if (!snap.opened) {
                                    onOpenIncomingSnap(snap.id)
                                    viewingSnap = snap
                                } else if (snap.replayAllowed && !snap.replayed) {
                                    onReplayIncomingSnap(snap.id)
                                    viewingSnap = snap
                                } else if (!snap.viewOnce) {
                                    viewingSnap = snap
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!snap.opened) SnapCoral else Color.Transparent)
                                    .then(
                                        if (snap.opened) Modifier.background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                        else Modifier
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(snap.senderName, style = MaterialTheme.typography.titleMedium)
                                    AiSimPill()
                                }
                                Text(
                                    text = when {
                                        !snap.opened -> "Tap to View • ${snap.themeTitle} (${snap.timerSeconds}s)"
                                        lockedExpired -> "Opened • View Once Expired"
                                        snap.replayAllowed && !snap.replayed -> "Tap to Replay • ${snap.themeTitle}"
                                        else -> "Opened • ${snap.themeTitle}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (!snap.opened) SnapCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            if (snap.opened && snap.replayAllowed && !snap.replayed) {
                                Icon(Icons.Filled.Replay, contentDescription = "Replay snap", tint = NeonCyan)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Friends Chat",
                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            items(chatFriends, key = { it.id }) { friend ->
                val state = stateMap[friend.id]
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFriend(friend) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(friend.avatarColorArgb))
                        ) {
                            Text(friend.name.take(1), fontWeight = FontWeight.Bold, color = ObsidianBlack)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(friend.name, style = MaterialTheme.typography.titleMedium)
                                AiSimPill()
                            }
                            Text(
                                text = state?.lastMessage?.ifBlank { "Tap to chat with ${friend.name}" }
                                    ?: "${friend.city} • ${friend.onlineStatus}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if ((state?.unreadCount ?: 0L) > 0L) {
                            Surface(shape = CircleShape, color = SnapCoral) {
                                Text(
                                    text = "${state?.unreadCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = PureWhite),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    viewingSnap?.let { snap ->
        IncomingSnapViewerDialog(
            snap = snap,
            onDismiss = { viewingSnap = null }
        )
    }
}

@Composable
private fun IncomingSnapViewerDialog(
    snap: IncomingSimSnapEntity,
    onDismiss: () -> Unit
) {
    var secondsLeft by remember(snap.id) { mutableIntStateOf(snap.timerSeconds) }

    LaunchedEffect(snap.id) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
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
                .background(
                    Brush.verticalGradient(
                        listOf(Color(snap.paletteStartArgb), Color(snap.paletteEndArgb))
                    )
                )
                .clickable { onDismiss() }
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(snap.senderName, style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
                    Text("${snap.filterName} • AI Sim Snap", style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.8f)))
                }
                Surface(shape = CircleShape, color = ObsidianBlack.copy(alpha = 0.65f)) {
                    Text(
                        text = "${secondsLeft}s",
                        style = MaterialTheme.typography.titleMedium.copy(color = SnapYellow),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(snap.stickerEmoji, fontSize = 84.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(snap.themeTitle, style = MaterialTheme.typography.headlineLarge.copy(color = PureWhite))
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianBlack.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = snap.caption,
                        style = MaterialTheme.typography.titleMedium.copy(color = PureWhite),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Text(
                text = if (snap.viewOnce) "View Once • Tap anywhere to close" else "Tap anywhere to close",
                style = MaterialTheme.typography.labelLarge.copy(color = PureWhite.copy(alpha = 0.85f)),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ChatDetailScreen(
    friend: SimulatedFriendProfile,
    messagesState: UiState<List<ChatMessage>>,
    isTyping: Boolean,
    aiError: String?,
    onBack: () -> Unit,
    onSend: (String, String, String?) -> Unit,
    onReact: (String, String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentVoiceFile by remember { mutableStateOf<File?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try { recorder?.release() } catch (_: Exception) {}
            try { mediaPlayer?.release() } catch (_: Exception) {}
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val isVid = mime.startsWith("video")
            onSend(if (isVid) "video" else "photo", if (isVid) "Sent a video" else "Sent a photo", uri.toString())
        }
    }

    val starterPrompts = listOf(
        "Kya haal hai bhai?",
        "Chai pine chalein?",
        "Snap kaisa laga?",
        "Weekend plan kya hai?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to chats")
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(friend.avatarColorArgb))
                ) {
                    Text(friend.name.take(1), fontWeight = FontWeight.Bold, color = ObsidianBlack)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(friend.name, style = MaterialTheme.typography.titleMedium)
                        AiSimPill()
                    }
                    Text(
                        text = if (isTyping) "typing..." else "${friend.city} • ${friend.onlineStatus}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isTyping) MintOnline else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Conversation starter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            starterPrompts.forEach { starter ->
                FilterChip(
                    selected = false,
                    onClick = { onSend("text", starter, null) },
                    label = { Text(starter) }
                )
            }
        }

        // Messages List
        Box(modifier = Modifier.weight(1f)) {
            when (messagesState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading chat...")
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(messagesState.message, color = SnapCoral)
                }
                is UiState.Success -> {
                    val msgs = messagesState.data
                    if (msgs.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Say hi to ${friend.name}! AI-powered Indian friend replies will appear here.",
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(msgs, key = { it.id }) { msg ->
                                val isUser = msg.senderRole == "user"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isUser) SnapYellow else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .clickable {
                                                val nextReaction = if (msg.reaction == "❤️") "🔥" else "❤️"
                                                onReact(msg.id, nextReaction)
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            if (msg.messageType == "voice" && !msg.mediaUri.isNullOrBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.clickable {
                                                        try {
                                                            mediaPlayer?.release()
                                                            mediaPlayer = MediaPlayer().apply {
                                                                setDataSource(msg.mediaUri)
                                                                prepare()
                                                                start()
                                                            }
                                                        } catch (_: Exception) {
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play voice note", tint = if (isUser) ObsidianBlack else SnapYellow)
                                                    Text(
                                                        text = msg.text,
                                                        color = if (isUser) ObsidianBlack else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = msg.text,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        color = if (isUser) ObsidianBlack else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Text(
                                                    text = msg.deliveryStatus,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isUser) ObsidianBlack.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                if (!msg.reaction.isNullOrBlank()) {
                                                    Text(msg.reaction, fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!aiError.isNullOrBlank()) {
            Surface(color = SnapCoral.copy(alpha = 0.16f), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AI Simulation: $aiError",
                    style = MaterialTheme.typography.labelMedium.copy(color = SnapCoral),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Input Composer Bar
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = {
                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Attach photo or video")
                }

                IconButton(onClick = {
                    val hasAudio = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasAudio) {
                        audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return@IconButton
                    }
                    if (!isRecordingVoice) {
                        try {
                            val file = PrivateMediaStorage.createPrivateVoiceFile(context)
                            val rec = PrivateMediaStorage.createAudioRecorder(context, file)
                            rec.start()
                            recorder = rec
                            currentVoiceFile = file
                            isRecordingVoice = true
                        } catch (_: Exception) {
                        }
                    } else {
                        try {
                            recorder?.stop()
                            recorder?.release()
                        } catch (_: Exception) {
                        }
                        recorder = null
                        isRecordingVoice = false
                        currentVoiceFile?.let { f ->
                            onSend("voice", "Voice Note", f.absolutePath)
                        }
                    }
                }) {
                    Icon(
                        if (isRecordingVoice) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = "Voice note",
                        tint = if (isRecordingVoice) SnapCoral else MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(if (isRecordingVoice) "Recording voice note..." else "Send a chat...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSend("text", inputText, null)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.testTag("send_chat_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send chat", tint = SnapYellow)
                }
            }
        }
    }
}
