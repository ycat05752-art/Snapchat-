package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.firebase.signOutUser
import com.example.data.local.AppLocalDatabase
import com.example.data.local.UserPreferencesStore
import com.example.data.model.FilterCatalog
import com.example.data.model.UiState
import com.example.data.repository.AiFriendChatRepository
import com.example.data.repository.SnapFirestoreRepository
import com.example.ui.theme.NileshSnapchatTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnapGold
import com.example.ui.theme.SnapYellow
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

enum class MainTab(val label: String) {
    FRIENDS("Friends"),
    CHAT("Chat"),
    CAMERA("Camera"),
    STORIES("Stories"),
    PROFILE("Profile")
}

private enum class SubOverlayScreen {
    NONE,
    SAVED_SNAPS,
    SETTINGS
}

@Composable
fun NileshSnapchatApp() {
    val context = LocalContext.current
    val firebaseInitialized = remember(context) {
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    }

    if (!firebaseInitialized) {
        NileshSnapchatTheme(darkTheme = true) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ObsidianBlack
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NILESH SNAPCHAT",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = SnapYellow,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Firebase config (app/google-services.json) was not included in this build. Add app/google-services.json to the repository and rebuild to enable Google Sign-In, Firestore, and AI Chat.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite)
                    )
                }
            }
        }
        return
    }

    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (showSplash) {
        NileshSnapchatTheme(darkTheme = true) {
            SplashScreen(onSplashFinished = { showSplash = false })
        }
    } else if (currentUser == null) {
        NileshSnapchatTheme(darkTheme = true) {
            AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
        }
    } else {
        val user = currentUser!!
        val uid = user.uid
        key(uid) {
            AuthenticatedNileshShell(
                currentUserId = uid,
                displayName = user.displayName ?: "Nilesh User",
                email = user.email
            )
        }
    }
}

@Composable
private fun AuthenticatedNileshShell(
    currentUserId: String,
    displayName: String,
    email: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    firestoreRepo = SnapFirestoreRepository(db),
                    aiChatRepo = AiFriendChatRepository(),
                    localDb = AppLocalDatabase.getInstance(app),
                    prefsStore = UserPreferencesStore(app),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email
                )
            }
        }
    )

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val notesState by viewModel.notesState.collectAsStateWithLifecycle()
    val sentSnapsState by viewModel.sentSnapsState.collectAsStateWithLifecycle()
    val pnHistoryState by viewModel.pnHistoryState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val friendStates by viewModel.friendStates.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val savedSnaps by viewModel.savedSnaps.collectAsStateWithLifecycle()
    val filterPrefs by viewModel.filterPrefs.collectAsStateWithLifecycle()
    val incomingSimSnaps by viewModel.incomingSimSnaps.collectAsStateWithLifecycle()
    val draftSnap by viewModel.draftSnap.collectAsStateWithLifecycle()
    val activeChatFriendId by viewModel.activeChatFriendId.collectAsStateWithLifecycle()
    val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val friendTypingId by viewModel.friendTypingId.collectAsStateWithLifecycle()
    val aiChatError by viewModel.aiChatError.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val pnCelebration by viewModel.pnCelebration.collectAsStateWithLifecycle()
    val userApproxCity by viewModel.userApproxCity.collectAsStateWithLifecycle()
    val userLatLng by viewModel.userLatLng.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val darkMode = userProfile?.darkMode ?: true

    var activeTab by remember { mutableStateOf(MainTab.CAMERA) }
    var subOverlay by remember { mutableStateOf(SubOverlayScreen.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Request POST_NOTIFICATIONS gracefully on Android 13+
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(bannerMessage) {
        bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    LaunchedEffect(pnCelebration) {
        if (pnCelebration != null) {
            delay(2200L)
            viewModel.dismissPnCelebration()
        }
    }

    val stateMap = remember(friendStates) { friendStates.associateBy { it.friendId } }
    val activeFriendsCount = remember(viewModel.allSimulatedFriends, stateMap) {
        viewModel.allSimulatedFriends.count { f ->
            val st = stateMap[f.id]?.relationStatus
            st == "friend" || (st == null && f.isDefaultFriend)
        }
    }

    NileshSnapchatTheme(darkTheme = darkMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            val currentDraft = draftSnap
            if (currentDraft != null) {
                BackHandler { viewModel.clearDraft() }
                SnapEditorScreen(
                    draft = currentDraft,
                    friends = viewModel.allSimulatedFriends,
                    onBack = { viewModel.clearDraft() },
                    onSaveToMySavedSnaps = { bmp, mediaType, path, caption, filterName, isFiltered ->
                        viewModel.saveDraftToMySavedSnaps(context, bmp, mediaType, path, caption, filterName, isFiltered)
                    },
                    onPostToStory = { type, caption, filterName, music, privacy, path ->
                        viewModel.createStory(type, caption, filterName, music, privacy, "#14141B", path)
                        viewModel.clearDraft()
                        activeTab = MainTab.STORIES
                    },
                    onSendToFriends = { recipients, caption, filterName, mediaType, viewOnce, replayAllowed, timer ->
                        viewModel.sendSnapToFriends(
                            context = context,
                            recipients = recipients,
                            caption = caption,
                            filterName = filterName,
                            mediaType = mediaType,
                            viewOnce = viewOnce,
                            replayAllowed = replayAllowed,
                            timerSeconds = timer
                        )
                    }
                )
            } else if (subOverlay == SubOverlayScreen.SAVED_SNAPS) {
                BackHandler { subOverlay = SubOverlayScreen.NONE }
                SavedSnapsScreen(
                    savedSnaps = savedSnaps,
                    onBack = { subOverlay = SubOverlayScreen.NONE },
                    onToggleFavorite = { viewModel.toggleSavedSnapFavorite(it) },
                    onDeleteSnaps = { viewModel.deleteSavedSnaps(it) },
                    onShowMessage = { msg -> viewModel.showBanner(msg) }
                )
            } else if (subOverlay == SubOverlayScreen.SETTINGS) {
                BackHandler { subOverlay = SubOverlayScreen.NONE }
                SettingsScreen(
                    userProfile = userProfile,
                    friends = viewModel.allSimulatedFriends,
                    friendStates = friendStates,
                    onBack = { subOverlay = SubOverlayScreen.NONE },
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onUnblockFriend = { fid -> viewModel.setFriendRelation(fid, "friend", false) },
                    onTriggerSimulatedSnapNow = { viewModel.triggerSimulatedIncomingSnap(context) },
                    onLogOut = {
                        signOutUser(credentialManager, scope)
                    },
                    onDeleteAccount = {
                        viewModel.deleteAccount {
                            signOutUser(credentialManager, scope)
                        }
                    },
                    onShowMessage = { msg -> viewModel.showBanner(msg) }
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 600.dp

                    if (isWideScreen) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                containerColor = ObsidianBlack,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = activeTab == tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { activeTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tabIcon(tab, selected),
                                                contentDescription = tab.label,
                                                tint = if (selected) SnapYellow else PureWhite.copy(alpha = 0.7f)
                                            )
                                        },
                                        label = {
                                            Text(
                                                tab.label,
                                                color = if (selected) SnapYellow else PureWhite.copy(alpha = 0.7f)
                                            )
                                        }
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MainTabBody(
                                    activeTab = activeTab,
                                    viewModel = viewModel,
                                    userProfileState = userProfileState,
                                    notesState = notesState,
                                    sentSnapsState = sentSnapsState,
                                    pnHistoryState = pnHistoryState,
                                    storiesState = storiesState,
                                    friendStates = friendStates,
                                    notifications = notifications,
                                    savedSnaps = savedSnaps,
                                    filterPrefs = filterPrefs,
                                    incomingSimSnaps = incomingSimSnaps,
                                    activeChatFriendId = activeChatFriendId,
                                    activeChatMessages = activeChatMessages,
                                    friendTypingId = friendTypingId,
                                    aiChatError = aiChatError,
                                    userApproxCity = userApproxCity,
                                    userLatLng = userLatLng,
                                    activeFriendsCount = activeFriendsCount,
                                    onSelectTab = { activeTab = it },
                                    onOpenSavedSnaps = { subOverlay = SubOverlayScreen.SAVED_SNAPS },
                                    onOpenSettings = { subOverlay = SubOverlayScreen.SETTINGS }
                                )
                            }
                        }
                    } else {
                        Scaffold(
                            bottomBar = {
                                NileshBottomNavBar(
                                    activeTab = activeTab,
                                    unreadChats = incomingSimSnaps.count { !it.opened },
                                    onSelectTab = { activeTab = it }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            ) {
                                MainTabBody(
                                    activeTab = activeTab,
                                    viewModel = viewModel,
                                    userProfileState = userProfileState,
                                    notesState = notesState,
                                    sentSnapsState = sentSnapsState,
                                    pnHistoryState = pnHistoryState,
                                    storiesState = storiesState,
                                    friendStates = friendStates,
                                    notifications = notifications,
                                    savedSnaps = savedSnaps,
                                    filterPrefs = filterPrefs,
                                    incomingSimSnaps = incomingSimSnaps,
                                    activeChatFriendId = activeChatFriendId,
                                    activeChatMessages = activeChatMessages,
                                    friendTypingId = friendTypingId,
                                    aiChatError = aiChatError,
                                    userApproxCity = userApproxCity,
                                    userLatLng = userLatLng,
                                    activeFriendsCount = activeFriendsCount,
                                    onSelectTab = { activeTab = it },
                                    onOpenSavedSnaps = { subOverlay = SubOverlayScreen.SAVED_SNAPS },
                                    onOpenSettings = { subOverlay = SubOverlayScreen.SETTINGS }
                                )
                            }
                        }
                    }
                }
            }

            // Animated PN Point Celebration Banner
            AnimatedVisibility(
                visible = pnCelebration != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SnapYellow,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = ObsidianBlack)
                        Text(
                            text = "+${pnCelebration ?: 1000} PN POINTS!",
                            style = MaterialTheme.typography.titleMedium.copy(color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            // Global SnackbarHost visible across main tabs, wide screens, editor, and overlays
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(
                        bottom = if (currentDraft == null && subOverlay == SubOverlayScreen.NONE) 82.dp else 16.dp
                    )
            )
        }
    }
}

@Composable
private fun MainTabBody(
    activeTab: MainTab,
    viewModel: MainViewModel,
    userProfileState: UiState<com.example.data.model.UserProfile?>,
    notesState: UiState<List<com.example.data.model.UserNote>>,
    sentSnapsState: UiState<List<com.example.data.model.SentSnap>>,
    pnHistoryState: UiState<List<com.example.data.model.PnHistoryEntry>>,
    storiesState: UiState<List<com.example.data.model.UserStory>>,
    friendStates: List<com.example.data.model.FriendState>,
    notifications: List<com.example.data.model.AppNotification>,
    savedSnaps: List<com.example.data.local.SavedSnapEntity>,
    filterPrefs: List<com.example.data.local.FilterPrefEntity>,
    incomingSimSnaps: List<com.example.data.local.IncomingSimSnapEntity>,
    activeChatFriendId: String?,
    activeChatMessages: UiState<List<com.example.data.model.ChatMessage>>,
    friendTypingId: String?,
    aiChatError: String?,
    userApproxCity: String,
    userLatLng: Pair<Double, Double>?,
    activeFriendsCount: Int,
    onSelectTab: (MainTab) -> Unit,
    onOpenSavedSnaps: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val userProfile = (userProfileState as? UiState.Success)?.data

    when (activeTab) {
        MainTab.FRIENDS -> FriendsScreen(
            allSimulatedFriends = viewModel.allSimulatedFriends,
            friendStates = friendStates,
            sentSnapsState = sentSnapsState,
            userProfile = userProfile,
            userApproxCity = userApproxCity,
            userLatLng = userLatLng,
            onOpenChat = { friend ->
                viewModel.openChat(friend.id)
                onSelectTab(MainTab.CHAT)
            },
            onSetFriendRelation = { fid, st, rep -> viewModel.setFriendRelation(fid, st, rep) },
            onToggleLocationSharing = { enabled -> viewModel.updateSettings(mapOf("locationSharingEnabled" to enabled)) },
            onToggleGhostMode = { ghost -> viewModel.updateSettings(mapOf("ghostMode" to ghost)) },
            onUpdateUserLocation = { city, lat, lng -> viewModel.updateUserLocation(city, lat, lng) }
        )

        MainTab.CHAT -> ChatScreen(
            friends = viewModel.allSimulatedFriends,
            friendStates = friendStates,
            incomingSnaps = incomingSimSnaps,
            activeChatFriendId = activeChatFriendId,
            activeMessagesState = activeChatMessages,
            friendTypingId = friendTypingId,
            aiChatError = aiChatError,
            onOpenChat = { fid -> viewModel.openChat(fid) },
            onSendChatMessage = { friend, type, text, uri -> viewModel.sendChatMessageWithAiReply(friend, type, text, uri) },
            onReactToMessage = { fid, msgId, emoji -> viewModel.reactToMessage(fid, msgId, emoji) },
            onOpenIncomingSnap = { id -> viewModel.openIncomingSimSnap(id) },
            onReplayIncomingSnap = { id -> viewModel.replayIncomingSimSnap(id) },
            onTriggerSimSnap = { viewModel.triggerSimulatedIncomingSnap(context) }
        )

        MainTab.CAMERA -> CameraScreen(
            allFilters = viewModel.allFilters,
            filterPrefs = filterPrefs,
            onFilterUsed = { viewModel.markFilterUsed(it) },
            onToggleFavoriteFilter = { viewModel.toggleFilterFavorite(it) },
            onCaptureBitmap = { bmp, filter, beauty, bright ->
                viewModel.setDraftFromBitmap(context, bmp, filter, beauty, bright)
            },
            onVideoRecorded = { path, filter ->
                viewModel.setDraftFromVideoFile(path, filter)
            },
            onPickedGalleryMedia = { uri, isVideo, filter ->
                viewModel.setDraftFromPickedUri(context, uri, isVideo, filter)
            },
            onOpenSavedSnaps = onOpenSavedSnaps
        )

        MainTab.STORIES -> StoriesScreen(
            storiesState = storiesState,
            friends = viewModel.allSimulatedFriends,
            onCreateStory = { type, caption, filter, music, privacy, bg, path ->
                viewModel.createStory(type, caption, filter, music, privacy, bg, path)
            },
            onIncrementStoryView = { id -> viewModel.incrementStoryView(id) },
            onDeleteStory = { id -> viewModel.deleteStory(id) },
            onReplyToFriendStory = { friend, msg ->
                viewModel.openChat(friend.id)
                viewModel.sendChatMessageWithAiReply(friend, "text", msg, null)
                onSelectTab(MainTab.CHAT)
            },
            onOpenCameraTab = { onSelectTab(MainTab.CAMERA) }
        )

        MainTab.PROFILE -> ProfileAndNotesScreen(
            userProfileState = userProfileState,
            pnHistoryState = pnHistoryState,
            notesState = notesState,
            notifications = notifications,
            friendsCount = activeFriendsCount,
            savedSnapsCount = savedSnaps.size,
            favoriteFiltersCount = filterPrefs.count { it.favorite },
            onOpenSavedSnaps = onOpenSavedSnaps,
            onOpenSettings = onOpenSettings,
            onUpdateProfile = { name, uname, bio ->
                viewModel.updateSettings(mapOf("displayName" to name, "username" to uname, "bio" to bio))
            },
            onSaveNote = { id, title, content, tag, pinned -> viewModel.saveNote(id, title, content, tag, pinned) },
            onDeleteNote = { id -> viewModel.deleteNote(id) },
            onMarkNotificationRead = { id -> viewModel.markNotificationRead(id) }
        )
    }
}

@Composable
private fun NileshBottomNavBar(
    activeTab: MainTab,
    unreadChats: Int,
    onSelectTab: (MainTab) -> Unit
) {
    Surface(
        color = ObsidianBlack,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val selected = activeTab == tab
                if (tab == MainTab.CAMERA) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (selected) SnapYellow else SnapGold)
                            .clickable { onSelectTab(tab) }
                            .testTag("nav_tab_camera")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Camera",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("nav_tab_${tab.name.lowercase()}")
                    ) {
                        Box {
                            Icon(
                                imageVector = tabIcon(tab, selected),
                                contentDescription = tab.label,
                                tint = if (selected) SnapYellow else PureWhite.copy(alpha = 0.65f),
                                modifier = Modifier.size(24.dp)
                            )
                            if (tab == MainTab.CHAT && unreadChats > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B5C))
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (selected) SnapYellow else PureWhite.copy(alpha = 0.65f),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: MainTab, selected: Boolean) = when (tab) {
    MainTab.FRIENDS -> if (selected) Icons.Filled.Group else Icons.Outlined.Group
    MainTab.CHAT -> if (selected) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat
    MainTab.CAMERA -> Icons.Filled.CameraAlt
    MainTab.STORIES -> if (selected) Icons.Filled.AutoStories else Icons.Outlined.AutoStories
    MainTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}
