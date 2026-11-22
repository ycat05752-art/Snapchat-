package com.example.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.PnHistoryEntry
import com.example.data.model.UiState
import com.example.data.model.UserNote
import com.example.data.model.UserProfile
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.MintOnline
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnapCoral
import com.example.ui.theme.SnapGold
import com.example.ui.theme.SnapYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndNotesScreen(
    userProfileState: UiState<UserProfile?>,
    pnHistoryState: UiState<List<PnHistoryEntry>>,
    notesState: UiState<List<UserNote>>,
    notifications: List<AppNotification>,
    friendsCount: Int,
    savedSnapsCount: Int,
    favoriteFiltersCount: Int,
    onOpenSavedSnaps: () -> Unit,
    onOpenSettings: () -> Unit,
    onUpdateProfile: (String, String, String) -> Unit,
    onSaveNote: (String?, String, String, String, Boolean) -> Unit,
    onDeleteNote: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit
) {
    val profile = (userProfileState as? UiState.Success)?.data
    val pnTarget = (profile?.pnPoints ?: 0L).toInt()
    val animatedPn by animateIntAsState(
        targetValue = pnTarget,
        animationSpec = tween(durationMillis = 800),
        label = "pn_counter"
    )

    var activeSection by remember { mutableStateOf("overview") } // overview, pn_history, notes, notifications
    var showEditProfile by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<UserNote?>(null) }
    var showCreateNote by remember { mutableStateOf(false) }
    var noteSearch by remember { mutableStateOf("") }

    val unreadNotifCount = remember(notifications) { notifications.count { !it.readFlag } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Profile & PN Points", style = MaterialTheme.typography.headlineMedium)
                Text("NILESH SNAPCHAT", style = MaterialTheme.typography.labelMedium.copy(color = SnapGold))
            }
            Row {
                IconButton(
                    onClick = { activeSection = "notifications" },
                    modifier = Modifier.testTag("open_notifications_button")
                ) {
                    Box {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        if (unreadNotifCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(SnapCoral)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("open_settings_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }

        // Sub-section pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "overview" to "Profile",
                "pn_history" to "PN History",
                "notes" to "Notes",
                "notifications" to "Alerts ($unreadNotifCount)"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = activeSection == key,
                    onClick = { activeSection = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SnapYellow,
                        selectedLabelColor = ObsidianBlack
                    )
                )
            }
        }

        when (activeSection) {
            "overview" -> {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Identity Card
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(SnapYellow)
                                    ) {
                                        Text(
                                            text = (profile?.displayName ?: "N").take(1).uppercase(),
                                            style = MaterialTheme.typography.headlineLarge.copy(color = ObsidianBlack)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(profile?.displayName ?: "Nilesh User", style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            "@${profile?.username ?: "nilesh_snap"}",
                                            style = MaterialTheme.typography.labelMedium.copy(color = SnapGold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            profile?.bio ?: "Snapping daily on NILESH SNAPCHAT",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                    IconButton(onClick = { showEditProfile = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                                    }
                                }
                            }
                        }
                    }

                    // Animated PN POINT Wallet Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(listOf(SnapYellow, SnapGold))
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = ObsidianBlack)
                                        Text(
                                            "PN POINT",
                                            style = MaterialTheme.typography.titleMedium.copy(color = ObsidianBlack, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(50), color = ObsidianBlack) {
                                        Text(
                                            "+1000 / Snap",
                                            style = MaterialTheme.typography.labelSmall.copy(color = SnapYellow),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$animatedPn",
                                    style = MaterialTheme.typography.displayLarge.copy(color = ObsidianBlack)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    Text(
                                        "Snaps Sent: ${profile?.totalSnapsSent ?: 0L}",
                                        style = MaterialTheme.typography.labelLarge.copy(color = ObsidianBlack)
                                    )
                                    Text(
                                        "Friends: $friendsCount",
                                        style = MaterialTheme.typography.labelLarge.copy(color = ObsidianBlack)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Tiles: My Saved Snaps, My Filters, Personal Notes
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            ProfileActionTile(
                                title = "MY SAVED SNAPS",
                                subtitle = "$savedSnapsCount Private",
                                icon = Icons.Filled.PhotoLibrary,
                                tint = SnapYellow,
                                onClick = onOpenSavedSnaps,
                                modifier = Modifier.weight(1f)
                            )
                            ProfileActionTile(
                                title = "My Filters",
                                subtitle = "$favoriteFiltersCount Saved",
                                icon = Icons.Filled.AutoAwesome,
                                tint = NeonCyan,
                                onClick = { activeSection = "pn_history" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeSection = "notes" }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Filled.NoteAlt, contentDescription = null, tint = SnapYellow, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Personal Notes", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Private quick notes, reminders & snap ideas",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Text("Open", style = MaterialTheme.typography.labelLarge.copy(color = SnapYellow))
                            }
                        }
                    }
                }
            }

            "pn_history" -> {
                val history = (pnHistoryState as? UiState.Success)?.data.orEmpty()
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Daily Activity & PN Points", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Total Earned: $animatedPn PN • Total Snaps Sent: ${profile?.totalSnapsSent ?: 0L}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = SnapGold)
                                )
                            }
                        }
                    }
                    if (history.isEmpty()) {
                        item {
                            Text(
                                "Send snaps to friends to see your +1000 PN Point history here!",
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(history, key = { it.id }) { entry ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(14.dp)
                                ) {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = SnapGold)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.reason, style = MaterialTheme.typography.titleMedium)
                                        val timeStr = entry.createdAt?.toDate()?.let {
                                            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
                                        } ?: "Just now"
                                        Text(timeStr, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    }
                                    Text("+${entry.delta} PN", style = MaterialTheme.typography.labelLarge.copy(color = MintOnline))
                                }
                            }
                        }
                    }
                }
            }

            "notes" -> {
                val allNotes = (notesState as? UiState.Success)?.data.orEmpty()
                val filteredNotes = remember(allNotes, noteSearch) {
                    if (noteSearch.isBlank()) allNotes
                    else allNotes.filter {
                        it.title.contains(noteSearch, ignoreCase = true) ||
                            it.content.contains(noteSearch, ignoreCase = true)
                    }
                }
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = noteSearch,
                            onValueChange = { noteSearch = it },
                            placeholder = { Text("Search personal notes...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showCreateNote = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("add_note_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "New Note")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingNote = note }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (note.pinned) {
                                                Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = SnapYellow, modifier = Modifier.size(16.dp))
                                            }
                                            Text(note.title, style = MaterialTheme.typography.titleMedium)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(note.content, style = MaterialTheme.typography.bodyMedium)
                                        val timeLabel = note.updatedAt?.toDate()?.let {
                                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
                                        } ?: "Saved"
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(timeLabel, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    }
                                    IconButton(onClick = { onDeleteNote(note.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = SnapCoral)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "notifications" -> {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (notifications.isEmpty()) {
                        item {
                            Text(
                                "No notifications yet.",
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(notifications, key = { it.id }) { notif ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!notif.readFlag) SnapYellow.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMarkNotificationRead(notif.id) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(notif.title, style = MaterialTheme.typography.titleMedium)
                                    Text(notif.body, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditProfile && profile != null) {
        var name by remember { mutableStateOf(profile.displayName) }
        var username by remember { mutableStateOf(profile.username) }
        var bio by remember { mutableStateOf(profile.bio) }
        ModalBottomSheet(onDismissRequest = { showEditProfile = false }, containerColor = CharcoalSurface) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Edit Profile", style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = name, onValueChange = { name = it.take(50) }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = username, onValueChange = { username = it.take(30) }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = bio, onValueChange = { bio = it.take(200) }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onUpdateProfile(name, username, bio)
                        showEditProfile = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showCreateNote || editingNote != null) {
        val current = editingNote
        var title by remember(current) { mutableStateOf(current?.title ?: "") }
        var content by remember(current) { mutableStateOf(current?.content ?: "") }
        var pinned by remember(current) { mutableStateOf(current?.pinned ?: false) }

        ModalBottomSheet(
            onDismissRequest = {
                showCreateNote = false
                editingNote = null
            },
            containerColor = CharcoalSurface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(if (current == null) "Create Note" else "Edit Note", style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = title, onValueChange = { title = it.take(100) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it.take(2000) },
                    label = { Text("Private Note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pin Note", color = PureWhite)
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onSaveNote(current?.id, title, content, "YELLOW", pinned)
                        showCreateNote = false
                        editingNote = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Note", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}
