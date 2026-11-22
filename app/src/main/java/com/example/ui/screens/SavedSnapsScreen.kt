package com.example.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PrivateMediaStorage
import com.example.data.local.SavedSnapEntity
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnapCoral
import com.example.ui.theme.SnapYellow
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SavedSnapsScreen(
    savedSnaps: List<SavedSnapEntity>,
    onBack: () -> Unit,
    onToggleFavorite: (SavedSnapEntity) -> Unit,
    onDeleteSnaps: (List<SavedSnapEntity>) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilterTab by remember { mutableStateOf("all") } // all, photos, videos, favorites, filtered, date
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var fullScreenSnap by remember { mutableStateOf<SavedSnapEntity?>(null) }

    val filteredSnaps = remember(savedSnaps, searchQuery, activeFilterTab) {
        val base = when (activeFilterTab) {
            "photos" -> savedSnaps.filter { it.mediaType == "photo" }
            "videos" -> savedSnaps.filter { it.mediaType == "video" }
            "favorites" -> savedSnaps.filter { it.favorite }
            "filtered" -> savedSnaps.filter { it.filtered }
            else -> savedSnaps
        }
        if (searchQuery.isBlank()) base
        else base.filter {
            it.caption.contains(searchQuery, ignoreCase = true) ||
                it.filterName.contains(searchQuery, ignoreCase = true) ||
                it.dateKey.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedByDate = remember(filteredSnaps) {
        filteredSnaps.groupBy { it.dateKey }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("saved_snaps_back")) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("MY SAVED SNAPS", style = MaterialTheme.typography.titleLarge)
                                    Icon(Icons.Filled.Lock, contentDescription = "Private vault", tint = SnapYellow, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "Private in-app gallery (${savedSnaps.size} items)",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    multiSelectMode = !multiSelectMode
                                    if (!multiSelectMode) selectedIds.clear()
                                },
                                modifier = Modifier.testTag("multi_select_toggle")
                            ) {
                                Icon(
                                    Icons.Filled.SelectAll,
                                    contentDescription = "Multiple selection",
                                    tint = if (multiSelectMode) SnapYellow else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (multiSelectMode && selectedIds.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        val toDelete = savedSnaps.filter { it.id in selectedIds }
                                        onDeleteSnaps(toDelete)
                                        selectedIds.clear()
                                        multiSelectMode = false
                                    }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = SnapCoral)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search saved snaps by caption, filter, date...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saved_snaps_search")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabs = listOf(
                            "all" to "All",
                            "photos" to "Saved Photos",
                            "videos" to "Saved Videos",
                            "favorites" to "Favourite",
                            "filtered" to "Filtered",
                            "date" to "Date-wise"
                        )
                        tabs.forEach { (key, label) ->
                            FilterChip(
                                selected = activeFilterTab == key,
                                onClick = { activeFilterTab = key },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnapYellow,
                                    selectedLabelColor = ObsidianBlack
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (filteredSnaps.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = SnapYellow,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Saved Snaps Yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Capture photos or videos in Camera and tap Save to store them privately inside NILESH SNAPCHAT.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedByDate.forEach { (dateLabel, itemsForDate) ->
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    items(itemsForDate, key = { it.id }) { snap ->
                        val isSelected = snap.id in selectedIds
                        SavedSnapGridTile(
                            snap = snap,
                            multiSelectMode = multiSelectMode,
                            isSelected = isSelected,
                            onClick = {
                                if (multiSelectMode) {
                                    if (isSelected) selectedIds.remove(snap.id) else selectedIds.add(snap.id)
                                } else {
                                    fullScreenSnap = snap
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(snap) }
                        )
                    }
                }
            }
        }
    }

    fullScreenSnap?.let { current ->
        Dialog(
            onDismissRequest = { fullScreenSnap = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack)
                    .statusBarsPadding()
            ) {
                val bmp = remember(current.filePath) {
                    if (current.mediaType == "photo") BitmapFactory.decodeFile(current.filePath) else null
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = current.caption.ifBlank { "Saved snap" },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = null, tint = SnapYellow, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Saved Video Snap", style = MaterialTheme.typography.titleLarge.copy(color = PureWhite))
                        Text(current.filterName, style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite.copy(alpha = 0.7f)))
                    }
                }

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { fullScreenSnap = null },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ObsidianBlack.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = PureWhite)
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = ObsidianBlack.copy(alpha = 0.65f)) {
                        Text(
                            text = "${current.dateKey} • ${current.filterName}",
                            style = MaterialTheme.typography.labelMedium.copy(color = SnapYellow),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Bottom actions: Favorite | Download to Phone | Share | Delete
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(ObsidianBlack.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        onToggleFavorite(current)
                        fullScreenSnap = current.copy(favorite = !current.favorite)
                    }) {
                        Icon(
                            if (current.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (current.favorite) SnapCoral else PureWhite
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val ok = PrivateMediaStorage.exportToDeviceGallery(
                                context,
                                File(current.filePath),
                                current.mediaType == "video"
                            )
                            onShowMessage(if (ok) "Downloaded to Phone Gallery!" else "Download failed.")
                        }
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download to Phone", tint = SnapYellow)
                    }
                    IconButton(onClick = {
                        PrivateMediaStorage.sharePrivateFile(
                            context,
                            File(current.filePath),
                            current.mediaType == "video"
                        )
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Photo", tint = PureWhite)
                    }
                    IconButton(onClick = {
                        onDeleteSnaps(listOf(current))
                        fullScreenSnap = null
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Photo", tint = SnapCoral)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedSnapGridTile(
    snap: SavedSnapEntity,
    multiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val bmp = remember(snap.filePath) {
        if (snap.mediaType == "photo" && File(snap.filePath).exists()) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(snap.filePath, opts)
        } else null
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianBlack)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) SnapYellow else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = snap.caption.ifBlank { "Saved snap" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Videocam, contentDescription = "Video", tint = SnapYellow, modifier = Modifier.size(34.dp))
            }
        }

        if (multiSelectMode) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isSelected) SnapYellow else PureWhite.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
            )
        } else {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
            ) {
                Icon(
                    imageVector = if (snap.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (snap.favorite) SnapCoral else PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ObsidianBlack.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = snap.filterName,
                style = MaterialTheme.typography.labelSmall.copy(color = PureWhite),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
