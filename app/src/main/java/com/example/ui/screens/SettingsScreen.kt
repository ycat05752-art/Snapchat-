package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PrivateMediaStorage
import com.example.data.model.FriendState
import com.example.data.model.SimulatedFriendProfile
import com.example.data.model.UserProfile
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SnapCoral
import com.example.ui.theme.SnapYellow

@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    friends: List<SimulatedFriendProfile>,
    friendStates: List<FriendState>,
    onBack: () -> Unit,
    onUpdateSettings: (Map<String, Any?>) -> Unit,
    onUnblockFriend: (String) -> Unit,
    onTriggerSimulatedSnapNow: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var cacheBytes by remember { mutableLongStateOf(PrivateMediaStorage.calculateCacheSizeBytes(context)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogBody by remember { mutableStateOf("") }

    var emailInput by remember(userProfile?.email) { mutableStateOf(userProfile?.email ?: "") }
    var phoneInput by remember(userProfile?.phone) { mutableStateOf(userProfile?.phone ?: "") }

    val blockedFriendIds = remember(friendStates) {
        friendStates.filter { it.relationStatus == "blocked" }.map { it.friendId }.toSet()
    }
    val blockedFriends = remember(friends, blockedFriendIds) {
        friends.filter { it.id in blockedFriendIds }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                SettingsSectionCard("Account Settings") {
                    Text(
                        text = "Display Name: ${userProfile?.displayName ?: "Nilesh User"} • @${userProfile?.username ?: "nilesh_snap"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it.take(120) },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it.take(25) },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onUpdateSettings(mapOf("email" to emailInput, "phone" to phoneInput))
                            onShowMessage("Account contact info updated!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack)
                    ) {
                        Text("Save Account Info", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Password & Sign-In protected by Google Credential Manager.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            item {
                SettingsSectionCard("Appearance & Feedback") {
                    SettingsSwitchRow(
                        title = "Dark Mode",
                        subtitle = "Switch between Premium Dark and Clean Light theme",
                        checked = userProfile?.darkMode ?: true,
                        onCheckedChange = { onUpdateSettings(mapOf("darkMode" to it)) }
                    )
                    SettingsChoiceRow(
                        title = "App Language",
                        current = userProfile?.appLanguage ?: "english",
                        options = listOf("english" to "English", "hinglish" to "Hinglish"),
                        onSelect = { onUpdateSettings(mapOf("appLanguage" to it)) }
                    )
                    SettingsSwitchRow(
                        title = "Sounds",
                        subtitle = "Camera shutter, music preview & snap chimes",
                        checked = userProfile?.soundsEnabled ?: true,
                        onCheckedChange = { onUpdateSettings(mapOf("soundsEnabled" to it)) }
                    )
                    SettingsSwitchRow(
                        title = "Vibration",
                        subtitle = "Haptic feedback on snap capture and send",
                        checked = userProfile?.vibrationEnabled ?: true,
                        onCheckedChange = { onUpdateSettings(mapOf("vibrationEnabled" to it)) }
                    )
                }
            }

            item {
                SettingsSectionCard("Privacy & Location Settings") {
                    SettingsChoiceRow(
                        title = "Who Can Contact Me",
                        current = userProfile?.whoCanContact ?: "everyone",
                        options = listOf("everyone" to "Everyone", "friends" to "My Friends"),
                        onSelect = { onUpdateSettings(mapOf("whoCanContact" to it)) }
                    )
                    SettingsChoiceRow(
                        title = "Who Can View My Story",
                        current = userProfile?.whoCanViewStory ?: "friends",
                        options = listOf("friends" to "Friends Only", "close_friends" to "Close Friends", "private" to "Private"),
                        onSelect = { onUpdateSettings(mapOf("whoCanViewStory" to it)) }
                    )
                    SettingsSwitchRow(
                        title = "Location Sharing",
                        subtitle = "Default OFF. Requires explicit location permission.",
                        checked = userProfile?.locationSharingEnabled ?: false,
                        onCheckedChange = { onUpdateSettings(mapOf("locationSharingEnabled" to it)) }
                    )
                    SettingsSwitchRow(
                        title = "Ghost Mode",
                        subtitle = "Hide approximate pin on Snap Map",
                        checked = userProfile?.ghostMode ?: true,
                        onCheckedChange = { onUpdateSettings(mapOf("ghostMode" to it)) }
                    )
                }
            }

            item {
                SettingsSectionCard("PN Point & Simulated Friends Settings") {
                    Text(
                        text = "PN Points Balance: ${userProfile?.pnPoints ?: 0L} PN (+1000 PN per sent snap)",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSwitchRow(
                        title = "Auto Incoming Simulated Snaps",
                        subtitle = "Receive random Indian daily-life snaps every 2–3 hours",
                        checked = userProfile?.simAutoSnapEnabled ?: true,
                        onCheckedChange = { onUpdateSettings(mapOf("simAutoSnapEnabled" to it)) }
                    )
                    SettingsChoiceRow(
                        title = "Auto-Open Simulation Speed",
                        current = userProfile?.simOpenSpeed ?: "realistic",
                        options = listOf("realistic" to "5–10 Min", "fast" to "Fast Preview"),
                        onSelect = { onUpdateSettings(mapOf("simOpenSpeed" to it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onTriggerSimulatedSnapNow,
                        colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
                        modifier = Modifier.testTag("settings_trigger_sim_snap")
                    ) {
                        Text("Receive Simulated Friend Snap Now", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                SettingsSectionCard("Notifications, Camera, Saved Snaps & Story") {
                    SettingsSwitchRow(
                        title = "All Notifications",
                        subtitle = "New Snap, Chat, Friend Request, Story, PN Points & Memories",
                        checked = userProfile?.notificationsEnabled ?: true,
                        onCheckedChange = { onUpdateSettings(mapOf("notificationsEnabled" to it)) }
                    )
                    SettingsChoiceRow(
                        title = "Upload Quality",
                        current = userProfile?.uploadQuality ?: "high",
                        options = listOf("standard" to "Data Saver", "high" to "HD High"),
                        onSelect = { onUpdateSettings(mapOf("uploadQuality" to it)) }
                    )
                    SettingsChoiceRow(
                        title = "Download Quality",
                        current = userProfile?.downloadQuality ?: "high",
                        options = listOf("standard" to "Standard", "high" to "Original HD"),
                        onSelect = { onUpdateSettings(mapOf("downloadQuality" to it)) }
                    )
                }
            }

            item {
                SettingsSectionCard("Storage Management & Data Usage") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("App Cache", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${cacheBytes / 1024} KB used",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Button(
                            onClick = {
                                cacheBytes = PrivateMediaStorage.clearAppCache(context)
                                onShowMessage("Cache cleared!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack)
                        ) {
                            Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Clear Cache", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                SettingsSectionCard("Safety, Blocked Users & Support") {
                    if (blockedFriends.isEmpty()) {
                        Text(
                            "Blocked Users: None",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    } else {
                        blockedFriends.forEach { bf ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(bf.name, style = MaterialTheme.typography.bodyLarge)
                                TextButton(onClick = { onUnblockFriend(bf.id) }) { Text("Unblock") }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsLinkRow("Report a Problem") {
                        infoDialogTitle = "Report a Problem"
                        infoDialogBody = "Thank you for keeping NILESH SNAPCHAT safe. Use the Block/Report buttons on any profile or contact support."
                    }
                    SettingsLinkRow("Help & Support") {
                        infoDialogTitle = "Help & Support"
                        infoDialogBody = "NILESH SNAPCHAT combines CameraX, 1,076 filters, MY SAVED SNAPS private vault, +1000 PN Points per snap, 24h Stories, and AI-simulated Indian friends."
                    }
                    SettingsLinkRow("About NILESH SNAPCHAT") {
                        infoDialogTitle = "About NILESH SNAPCHAT"
                        infoDialogBody = "NILESH SNAPCHAT v1.0 • Original social camera experience with PN Points & AI Simulated Indian Friends."
                    }
                    SettingsLinkRow("Privacy Policy") {
                        infoDialogTitle = "Privacy Policy"
                        infoDialogBody = "Your photos in MY SAVED SNAPS are stored privately inside app storage. Location sharing is OFF by default and only accessed with your explicit permission."
                    }
                    SettingsLinkRow("Terms & Conditions") {
                        infoDialogTitle = "Terms & Conditions"
                        infoDialogBody = "Simulated Indian accounts are clearly labeled 'AI Sim' as virtual companions. PN Points are in-app social rewards."
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                    Button(
                        onClick = onLogOut,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Log Out", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SnapCoral, contentColor = MaterialTheme.colorScheme.onTertiary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("delete_account_button")
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Delete Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This will delete your NILESH SNAPCHAT cloud profile and sign you out. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAccount()
                }) {
                    Text("Delete", color = SnapCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    infoDialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            title = { Text(title) },
            text = { Text(infoDialogBody) },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = SnapYellow))
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    current: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, label) ->
                val active = current == key
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) SnapYellow else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onSelect(key) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (active) ObsidianBlack else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsLinkRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text("View", style = MaterialTheme.typography.labelLarge.copy(color = SnapYellow))
    }
}
