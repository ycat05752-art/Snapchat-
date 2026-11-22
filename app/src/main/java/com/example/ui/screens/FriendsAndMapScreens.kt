package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.FriendState
import com.example.data.model.SentSnap
import com.example.data.model.SimulatedFriendProfile
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.theme.MintOnline
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnapCoral
import com.example.ui.theme.SnapGold
import com.example.ui.theme.SnapYellow
import com.google.android.gms.location.LocationServices

@Composable
fun FriendsScreen(
    allSimulatedFriends: List<SimulatedFriendProfile>,
    friendStates: List<FriendState>,
    sentSnapsState: UiState<List<SentSnap>>,
    userProfile: UserProfile?,
    userApproxCity: String,
    userLatLng: Pair<Double, Double>?,
    onOpenChat: (SimulatedFriendProfile) -> Unit,
    onSetFriendRelation: (String, String, Boolean?) -> Unit,
    onToggleLocationSharing: (Boolean) -> Unit,
    onToggleGhostMode: (Boolean) -> Unit,
    onUpdateUserLocation: (String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("my_friends") } // my_friends, discover, requests, sent_snaps, map
    var searchQuery by remember { mutableStateOf("") }

    val stateMap = remember(friendStates) { friendStates.associateBy { it.friendId } }

    val myFriendsList = remember(allSimulatedFriends, stateMap, searchQuery) {
        allSimulatedFriends.filter { friend ->
            val st = stateMap[friend.id]?.relationStatus
            val isFriend = st == "friend" || (st == null && friend.isDefaultFriend)
            isFriend && (searchQuery.isBlank() ||
                friend.name.contains(searchQuery, ignoreCase = true) ||
                friend.username.contains(searchQuery, ignoreCase = true) ||
                friend.city.contains(searchQuery, ignoreCase = true))
        }
    }

    val pendingRequests = remember(allSimulatedFriends, stateMap) {
        allSimulatedFriends.filter { stateMap[it.id]?.relationStatus == "pending_incoming" }
    }

    val discoverList = remember(allSimulatedFriends, stateMap, searchQuery) {
        allSimulatedFriends.filter { friend ->
            val st = stateMap[friend.id]?.relationStatus
            st != "blocked" && (searchQuery.isBlank() ||
                friend.name.contains(searchQuery, ignoreCase = true) ||
                friend.username.contains(searchQuery, ignoreCase = true) ||
                friend.city.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Friends & Snap Map", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${allSimulatedFriends.size} AI Simulated Indian Profiles • ${myFriendsList.size} Friends",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = SnapYellow) {
                    Text(
                        text = "${userProfile?.pnPoints ?: 0L} PN",
                        style = MaterialTheme.typography.labelLarge.copy(color = ObsidianBlack),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 560+ Indian friends by name or city...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("friends_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    "my_friends" to "Friends (${myFriendsList.size})",
                    "discover" to "All ${allSimulatedFriends.size} AI Sim",
                    "requests" to "Requests (${pendingRequests.size})",
                    "sent_snaps" to "Sent Snaps",
                    "map" to "Snap Map"
                )
                tabs.forEach { (key, label) ->
                    FilterChip(
                        selected = activeTab == key,
                        onClick = { activeTab = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SnapYellow,
                            selectedLabelColor = ObsidianBlack
                        )
                    )
                }
            }
        }

        when (activeTab) {
            "my_friends", "discover" -> {
                val list = if (activeTab == "my_friends") myFriendsList else discoverList
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(list, key = { it.id }) { friend ->
                        val rel = stateMap[friend.id]?.relationStatus
                            ?: if (friend.isDefaultFriend) "friend" else "none"
                        FriendProfileCard(
                            friend = friend,
                            isAdded = rel == "friend",
                            onOpenChat = { onOpenChat(friend) },
                            onAddOrRemove = {
                                val next = if (rel == "friend") "none" else "friend"
                                onSetFriendRelation(friend.id, next, null)
                            },
                            onBlock = { onSetFriendRelation(friend.id, "blocked", null) },
                            onReport = { onSetFriendRelation(friend.id, "blocked", true) }
                        )
                    }
                }
            }

            "requests" -> {
                if (pendingRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No pending friend requests right now.",
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pendingRequests, key = { it.id }) { friend ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        Text("${friend.city} • ${friend.username}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    }
                                    IconButton(onClick = { onSetFriendRelation(friend.id, "friend", null) }) {
                                        Icon(Icons.Filled.Check, contentDescription = "Accept request", tint = MintOnline)
                                    }
                                    IconButton(onClick = { onSetFriendRelation(friend.id, "none", null) }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Decline request", tint = SnapCoral)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "sent_snaps" -> {
                SentSnapsStatusList(sentSnapsState = sentSnapsState)
            }

            "map" -> {
                SnapMapPanel(
                    userProfile = userProfile,
                    friends = myFriendsList.take(20),
                    userApproxCity = userApproxCity,
                    userLatLng = userLatLng,
                    onToggleLocationSharing = onToggleLocationSharing,
                    onToggleGhostMode = onToggleGhostMode,
                    onRefreshLocation = { lat, lng ->
                        onUpdateUserLocation("Live Approximate Location", lat, lng)
                    }
                )
            }
        }
    }
}

@Composable
private fun FriendProfileCard(
    friend: SimulatedFriendProfile,
    isAdded: Boolean,
    onOpenChat: () -> Unit,
    onAddOrRemove: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(friend.avatarColorArgb))
                ) {
                    Text(
                        text = friend.name.take(1),
                        style = MaterialTheme.typography.titleLarge.copy(color = ObsidianBlack, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = friend.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        AiSimPill()
                    }
                    Text(
                        text = "${friend.username} • ${friend.city} • ${friend.onlineStatus}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(shape = RoundedCornerShape(10.dp), color = SnapGold.copy(alpha = 0.16f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = SnapGold, modifier = Modifier.size(14.dp))
                        Text("${friend.pnPoints}", style = MaterialTheme.typography.labelMedium.copy(color = SnapGold))
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = friend.bio,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenChat,
                    colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chat", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onAddOrRemove,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        if (isAdded) Icons.Filled.PersonRemove else Icons.Filled.PersonAdd,
                        contentDescription = if (isAdded) "Remove friend" else "Add friend",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAdded) "Added" else "Add")
                }
                IconButton(onClick = onBlock) {
                    Icon(Icons.Filled.Block, contentDescription = "Block user", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onReport) {
                    Icon(Icons.Filled.Report, contentDescription = "Report user", tint = SnapCoral)
                }
            }
        }
    }
}

@Composable
fun AiSimPill() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = NeonCyan.copy(alpha = 0.16f)
    ) {
        Text(
            text = "AI Sim",
            style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SentSnapsStatusList(sentSnapsState: UiState<List<SentSnap>>) {
    when (sentSnapsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading sent snaps...")
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(sentSnapsState.message, color = SnapCoral)
        }
        is UiState.Success -> {
            val snaps = sentSnapsState.data
            if (snaps.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Send your first snap from Camera to earn +1000 PN Points and track live Opened status!",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(snaps, key = { it.id }) { snap ->
                        val statusColor = when (snap.status) {
                            "OPENED" -> MintOnline
                            "DELIVERED" -> NeonCyan
                            "EXPIRED" -> SnapCoral
                            else -> SnapYellow
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("To: ${snap.recipientName}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${snap.mediaType.uppercase()} • ${snap.filterName}" +
                                            if (snap.caption.isNotBlank()) " • \"${snap.caption}\"" else "",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(shape = RoundedCornerShape(10.dp), color = statusColor.copy(alpha = 0.16f)) {
                                    Text(
                                        text = snap.status,
                                        style = MaterialTheme.typography.labelLarge.copy(color = statusColor),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun SnapMapPanel(
    userProfile: UserProfile?,
    friends: List<SimulatedFriendProfile>,
    userApproxCity: String,
    userLatLng: Pair<Double, Double>?,
    onToggleLocationSharing: (Boolean) -> Unit,
    onToggleGhostMode: (Boolean) -> Unit,
    onRefreshLocation: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val sharingEnabled = userProfile?.locationSharingEnabled ?: false
    val ghostMode = userProfile?.ghostMode ?: true

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    fun fetchCoarseLocation() {
        if (!hasLocationPermission) return
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                onRefreshLocation(loc.latitude, loc.longitude)
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            onToggleLocationSharing(true)
            fetchCoarseLocation()
        } else {
            onToggleLocationSharing(false)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (sharingEnabled && !ghostMode) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                                contentDescription = null,
                                tint = if (sharingEnabled && !ghostMode) MintOnline else SnapCoral
                            )
                            Column {
                                Text("Location Sharing", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (sharingEnabled) "Active ($userApproxCity)" else "OFF by default (Private)",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                        Switch(
                            checked = sharingEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (hasLocationPermission) {
                                        onToggleLocationSharing(true)
                                        fetchCoarseLocation()
                                    } else {
                                        locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                } else {
                                    onToggleLocationSharing(false)
                                }
                            },
                            modifier = Modifier.testTag("location_sharing_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ghost Mode", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Hide your exact map pin from friends",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = ghostMode,
                            onCheckedChange = { onToggleGhostMode(it) }
                        )
                    }

                    if (userLatLng != null && sharingEnabled && !ghostMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last updated: ${"%.2f".format(userLatLng.first)}, ${"%.2f".format(userLatLng.second)}",
                            style = MaterialTheme.typography.labelMedium.copy(color = MintOnline)
                        )
                    }
                }
            }
        }

        // Stylized India Snap Map Canvas
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF1A2A3A), Color(0xFF0E141E))
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Subtle map grid lines
                    val cols = 6
                    val rows = 5
                    for (i in 1 until cols) {
                        val x = size.width * i / cols
                        drawLine(PureWhite.copy(alpha = 0.06f), Offset(x, 0f), Offset(x, size.height), 1.5f)
                    }
                    for (j in 1 until rows) {
                        val y = size.height * j / rows
                        drawLine(PureWhite.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y), 1.5f)
                    }

                    // Plot Indian simulated friends across normalized India bounds (lat 8..32, lng 68..90)
                    friends.forEach { friend ->
                        val nx = ((friend.longitude - 68.0) / 22.0).toFloat().coerceIn(0.08f, 0.92f)
                        val ny = (1f - ((friend.latitude - 8.0) / 24.0).toFloat()).coerceIn(0.10f, 0.90f)
                        val pt = Offset(nx * size.width, ny * size.height)
                        drawCircle(Color(friend.avatarColorArgb).copy(alpha = 0.3f), 20f, pt)
                        drawCircle(Color(friend.avatarColorArgb), 10f, pt)
                    }

                    if (sharingEnabled && !ghostMode && userLatLng != null) {
                        val ux = ((userLatLng.second - 68.0) / 22.0).toFloat().coerceIn(0.1f, 0.9f)
                        val uy = (1f - ((userLatLng.first - 8.0) / 24.0).toFloat()).coerceIn(0.1f, 0.9f)
                        val uPt = Offset(ux * size.width, uy * size.height)
                        drawCircle(SnapYellow.copy(alpha = 0.35f), 28f, uPt)
                        drawCircle(SnapYellow, 13f, uPt)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianBlack.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "India Snap Map • AI Sim Pins",
                        style = MaterialTheme.typography.labelMedium.copy(color = SnapYellow),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        items(friends.take(8), key = { "map_${it.id}" }) { friend ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(friend.avatarColorArgb))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${friend.name} • ${friend.city}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("Updated just now", style = MaterialTheme.typography.labelSmall.copy(color = MintOnline))
                }
            }
        }
    }
}
