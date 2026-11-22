package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppLocalDatabase
import com.example.data.local.FilterPrefEntity
import com.example.data.local.IncomingSimSnapEntity
import com.example.data.local.LocalNotificationHelper
import com.example.data.local.PrivateMediaStorage
import com.example.data.local.SavedSnapEntity
import com.example.data.local.UserPreferencesStore
import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.FilterCatalog
import com.example.data.model.FilterEngine
import com.example.data.model.FriendState
import com.example.data.model.PnHistoryEntry
import com.example.data.model.SentSnap
import com.example.data.model.SimulatedFriendProfile
import com.example.data.model.SimulatedFriendsCatalog
import com.example.data.model.SnapFilter
import com.example.data.model.UiState
import com.example.data.model.UserNote
import com.example.data.model.UserProfile
import com.example.data.model.UserStory
import com.example.data.repository.AiFriendChatRepository
import com.example.data.repository.SnapDataRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

data class CapturedSnapDraft(
    val filePath: String,
    val mediaType: String, // "photo" or "video"
    val filter: SnapFilter,
    val beautyLevel: Float,
    val brightnessLevel: Float,
    val rotationDegrees: Float = 0f,
    val caption: String = ""
)

class MainViewModel(
    private val firestoreRepo: SnapDataRepository,
    private val aiChatRepo: AiFriendChatRepository,
    private val localDb: AppLocalDatabase,
    private val prefsStore: UserPreferencesStore,
    val currentUserId: String,
    initialDisplayName: String,
    initialEmail: String?
) : ViewModel() {

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _pnCelebration = MutableStateFlow<Int?>(null)
    val pnCelebration: StateFlow<Int?> = _pnCelebration.asStateFlow()

    private val _draftSnap = MutableStateFlow<CapturedSnapDraft?>(null)
    val draftSnap: StateFlow<CapturedSnapDraft?> = _draftSnap.asStateFlow()

    private val _activeChatFriendId = MutableStateFlow<String?>(null)
    val activeChatFriendId: StateFlow<String?> = _activeChatFriendId.asStateFlow()

    private val _friendTypingId = MutableStateFlow<String?>(null)
    val friendTypingId: StateFlow<String?> = _friendTypingId.asStateFlow()

    private val _aiChatError = MutableStateFlow<String?>(null)
    val aiChatError: StateFlow<String?> = _aiChatError.asStateFlow()

    private val _userApproxCity = MutableStateFlow<String>("Your Location (Approximate)")
    val userApproxCity: StateFlow<String> = _userApproxCity.asStateFlow()

    private val _userLatLng = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLatLng: StateFlow<Pair<Double, Double>?> = _userLatLng.asStateFlow()

    val allSimulatedFriends: List<SimulatedFriendProfile> = SimulatedFriendsCatalog.allFriends
    val allFilters: List<SnapFilter> = FilterCatalog.allFilters

    val userProfileState: StateFlow<UiState<UserProfile?>> = firestoreRepo.observeUserProfile(currentUserId)
        .toUiStateFlow("Failed to load user profile")

    val notesState: StateFlow<UiState<List<UserNote>>> = firestoreRepo.observeNotes(currentUserId)
        .toUiStateFlow("Failed to load notes")

    val sentSnapsState: StateFlow<UiState<List<SentSnap>>> = firestoreRepo.observeSentSnaps(currentUserId)
        .toUiStateFlow("Failed to load sent snaps")

    val pnHistoryState: StateFlow<UiState<List<PnHistoryEntry>>> = firestoreRepo.observePnHistory(currentUserId)
        .toUiStateFlow("Failed to load PN Point history")

    val storiesState: StateFlow<UiState<List<UserStory>>> = firestoreRepo.observeStories(currentUserId)
        .toUiStateFlow("Failed to load stories")

    val friendStates: StateFlow<List<FriendState>> = firestoreRepo.observeFriendStates(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = firestoreRepo.observeNotifications(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedSnaps: StateFlow<List<SavedSnapEntity>> = localDb.savedSnapDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val filterPrefs: StateFlow<List<FilterPrefEntity>> = localDb.filterPrefDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val incomingSimSnaps: StateFlow<List<IncomingSimSnapEntity>> = localDb.incomingSimSnapDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeChatMessages: StateFlow<UiState<List<ChatMessage>>> = combine(
        _activeChatFriendId,
        MutableStateFlow(Unit)
    ) { friendId, _ -> friendId }
        .let { flow ->
            val resultFlow = MutableStateFlow<UiState<List<ChatMessage>>>(UiState.Success(emptyList()))
            var job: Job? = null
            viewModelScope.launch {
                _activeChatFriendId.collect { fId ->
                    job?.cancel()
                    if (fId.isNullOrBlank()) {
                        resultFlow.value = UiState.Success(emptyList())
                    } else {
                        resultFlow.value = UiState.Loading
                        job = viewModelScope.launch {
                            firestoreRepo.observeChatMessages(currentUserId, fId)
                                .toUiStateRaw()
                                .collect { resultFlow.value = it }
                        }
                    }
                }
            }
            resultFlow.asStateFlow()
        }

    init {
        viewModelScope.launch {
            firestoreRepo.ensureUserProfile(currentUserId, initialDisplayName, initialEmail)
            seedInitialSimulatedDataIfEmpty()
            startSimulatedSnapScheduler()
        }
    }

    private suspend fun seedInitialSimulatedDataIfEmpty() {
        // Seed 3 incoming friend requests in friend_states if none exist yet
        val existing = firestoreRepo.observeFriendStates(currentUserId).firstOrNull().orEmpty()
        if (existing.isEmpty()) {
            val requestIds = listOf("sim_046", "sim_047", "sim_048", "sim_049")
            requestIds.forEach { fid ->
                firestoreRepo.upsertFriendState(
                    userId = currentUserId,
                    friendId = fid,
                    relationStatus = "pending_incoming",
                    lastMessage = "Sent you a friend request"
                )
            }
            firestoreRepo.addNotification(
                userId = currentUserId,
                category = "friend_request",
                title = "New Friend Requests",
                body = "4 simulated Indian friends want to connect on NILESH SNAPCHAT."
            )
        }
        val existingSnaps = localDb.incomingSimSnapDao().observeAll().firstOrNull().orEmpty()
        if (existingSnaps.isEmpty()) {
            deliverSimulatedIncomingSnap(null, boostActivity = false)
        }
    }

    private fun startSimulatedSnapScheduler() {
        viewModelScope.launch {
            while (true) {
                val profile = (userProfileState.value as? UiState.Success)?.data
                val enabled = profile?.simAutoSnapEnabled ?: true
                val now = System.currentTimeMillis()
                var nextAt = prefsStore.getNextSimSnapAt()
                if (nextAt == 0L) {
                    // Randomize 2 to 3 hours (7,200,000 ms to 10,800,000 ms)
                    val randomGap = Random.nextLong(7_200_000L, 10_800_000L)
                    nextAt = now + randomGap
                    prefsStore.setSimSnapSchedule(now, nextAt)
                }
                if (enabled && now >= nextAt) {
                    deliverSimulatedIncomingSnap(null, boostActivity = false)
                    val randomGap = Random.nextLong(7_200_000L, 10_800_000L)
                    prefsStore.setSimSnapSchedule(now, now + randomGap)
                }
                delay(60_000L)
            }
        }
    }

    fun triggerSimulatedIncomingSnap(context: Context? = null) {
        viewModelScope.launch {
            deliverSimulatedIncomingSnap(context, boostActivity = true)
        }
    }

    private suspend fun deliverSimulatedIncomingSnap(context: Context?, boostActivity: Boolean) {
        val friendPool = allSimulatedFriends.take(150)
        val sender = friendPool[Random.nextInt(friendPool.size)]
        val theme = SimulatedFriendsCatalog.incomingSnapThemes[
            Random.nextInt(SimulatedFriendsCatalog.incomingSnapThemes.size)
        ]
        val filter = allFilters[Random.nextInt(allFilters.size)]
        val palettes = listOf(
            Pair(0xFFFF8F00L, 0xFFE65100L),
            Pair(0xFF00B0FFL, 0xFF1A237EL),
            Pair(0xFFFF3B5CL, 0xFF880E4FL),
            Pair(0xFF18E07FL, 0xFF004D40L),
            Pair(0xFF7C4DFFL, 0xFF311B92L)
        )
        val palette = palettes[Random.nextInt(palettes.size)]
        val emojis = listOf("☕", "🌅", "🔥", "🎉", "🪔", "🏏", "😂", "✨", "🍕", "🚗")
        val snap = IncomingSimSnapEntity(
            id = "in_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            senderId = sender.id,
            senderName = sender.name,
            senderUsername = sender.username,
            themeTitle = theme.first,
            caption = theme.second,
            filterName = filter.name,
            paletteStartArgb = palette.first,
            paletteEndArgb = palette.second,
            stickerEmoji = emojis[Random.nextInt(emojis.size)],
            viewOnce = Random.nextBoolean(),
            replayAllowed = Random.nextBoolean(),
            opened = false,
            replayed = false,
            timerSeconds = listOf(3, 5, 7, 10).random(),
            receivedAtMillis = System.currentTimeMillis()
        )
        localDb.incomingSimSnapDao().insert(snap)
        firestoreRepo.addNotification(
            userId = currentUserId,
            category = "snap",
            title = "New Snap from ${sender.name}",
            body = "${theme.first}: ${theme.second}",
            relatedId = snap.id
        )
        if (context != null) {
            LocalNotificationHelper.postNotification(
                context = context,
                title = "Snap from ${sender.name}",
                body = theme.second,
                notificationId = snap.id.hashCode()
            )
        }
        if (boostActivity) {
            _bannerMessage.value = "New snap received from ${sender.name}!"
        }
    }

    fun openIncomingSimSnap(snapId: String) {
        viewModelScope.launch {
            localDb.incomingSimSnapDao().markOpened(snapId)
        }
    }

    fun replayIncomingSimSnap(snapId: String) {
        viewModelScope.launch {
            localDb.incomingSimSnapDao().markReplayed(snapId)
        }
    }

    fun markFilterUsed(filter: SnapFilter) {
        viewModelScope.launch {
            val existing = filterPrefs.value.firstOrNull { it.filterId == filter.id }
            localDb.filterPrefDao().upsert(
                FilterPrefEntity(
                    filterId = filter.id,
                    favorite = existing?.favorite ?: false,
                    lastUsedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleFilterFavorite(filter: SnapFilter) {
        viewModelScope.launch {
            val existing = filterPrefs.value.firstOrNull { it.filterId == filter.id }
            val nextFav = !(existing?.favorite ?: false)
            localDb.filterPrefDao().upsert(
                FilterPrefEntity(
                    filterId = filter.id,
                    favorite = nextFav,
                    lastUsedAtMillis = existing?.lastUsedAtMillis ?: System.currentTimeMillis()
                )
            )
        }
    }

    fun setDraftFromBitmap(
        context: Context,
        bitmap: Bitmap,
        filter: SnapFilter,
        beautyLevel: Float,
        brightnessLevel: Float
    ) {
        viewModelScope.launch {
            val filteredBitmap = FilterEngine.applyFilterToBitmap(
                source = bitmap,
                filter = filter,
                beautyLevel = beautyLevel,
                brightnessLevel = brightnessLevel
            )
            val file = PrivateMediaStorage.savePrivateBitmap(context, filteredBitmap)
            _draftSnap.value = CapturedSnapDraft(
                filePath = file.absolutePath,
                mediaType = "photo",
                filter = filter,
                beautyLevel = beautyLevel,
                brightnessLevel = brightnessLevel
            )
        }
    }

    fun setDraftFromPickedUri(context: Context, uri: Uri, isVideo: Boolean, filter: SnapFilter) {
        viewModelScope.launch {
            val ext = if (isVideo) "mp4" else "jpg"
            val copied = PrivateMediaStorage.copyUriToPrivateFile(context, uri, ext)
            if (copied != null) {
                _draftSnap.value = CapturedSnapDraft(
                    filePath = copied.absolutePath,
                    mediaType = if (isVideo) "video" else "photo",
                    filter = filter,
                    beautyLevel = 0f,
                    brightnessLevel = 0f
                )
            } else {
                _bannerMessage.value = "Could not import selected media."
            }
        }
    }

    fun setDraftFromVideoFile(filePath: String, filter: SnapFilter) {
        _draftSnap.value = CapturedSnapDraft(
            filePath = filePath,
            mediaType = "video",
            filter = filter,
            beautyLevel = 0f,
            brightnessLevel = 0f
        )
    }

    fun clearDraft() {
        _draftSnap.value = null
    }

    fun saveDraftToMySavedSnaps(
        context: Context,
        editedBitmap: Bitmap?,
        mediaType: String,
        filePath: String,
        caption: String,
        filterName: String,
        isFiltered: Boolean
    ) {
        viewModelScope.launch {
            val finalPath = if (editedBitmap != null && mediaType == "photo") {
                PrivateMediaStorage.savePrivateBitmap(context, editedBitmap).absolutePath
            } else {
                filePath
            }
            val now = System.currentTimeMillis()
            val dateKey = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(now))
            val entity = SavedSnapEntity(
                id = "sav_${now}_${UUID.randomUUID().toString().take(5)}",
                filePath = finalPath,
                mediaType = mediaType,
                caption = caption.trim(),
                filterName = filterName,
                filtered = isFiltered,
                favorite = false,
                dateKey = dateKey,
                createdAtMillis = now
            )
            localDb.savedSnapDao().insert(entity)
            firestoreRepo.addNotification(
                userId = currentUserId,
                category = "memory",
                title = "Saved to MY SAVED SNAPS",
                body = if (caption.isNotBlank()) caption else "Private ${mediaType} saved securely."
            )
            _bannerMessage.value = "Saved to MY SAVED SNAPS privately!"
        }
    }

    fun toggleSavedSnapFavorite(snap: SavedSnapEntity) {
        viewModelScope.launch {
            localDb.savedSnapDao().setFavorite(snap.id, !snap.favorite)
        }
    }

    fun deleteSavedSnaps(snaps: List<SavedSnapEntity>) {
        viewModelScope.launch {
            snaps.forEach { PrivateMediaStorage.deleteFileQuietly(it.filePath) }
            localDb.savedSnapDao().deleteByIds(snaps.map { it.id })
            _bannerMessage.value = "Deleted ${snaps.size} snap(s)."
        }
    }

    fun sendSnapToFriends(
        context: Context,
        recipients: List<SimulatedFriendProfile>,
        caption: String,
        filterName: String,
        mediaType: String,
        viewOnce: Boolean,
        replayAllowed: Boolean,
        timerSeconds: Int
    ) {
        if (recipients.isEmpty()) return
        viewModelScope.launch {
            var totalEarned = 0
            val profile = (userProfileState.value as? UiState.Success)?.data
            val fastMode = profile?.simOpenSpeed == "fast"

            recipients.forEach { friend ->
                val result = firestoreRepo.sendSnapAndAwardPnPoints(
                    userId = currentUserId,
                    recipientId = friend.id,
                    recipientName = friend.name,
                    caption = caption,
                    filterName = filterName,
                    mediaType = mediaType,
                    viewOnce = viewOnce,
                    replayAllowed = replayAllowed,
                    timerSeconds = timerSeconds
                )
                if (result.isSuccess) {
                    val snapId = result.getOrNull().orEmpty()
                    totalEarned += 1000
                    scheduleAutoSeenSimulation(context, snapId, friend, viewOnce, fastMode)
                }
            }
            if (totalEarned > 0) {
                _pnCelebration.value = totalEarned
                _bannerMessage.value = "Snap sent! +$totalEarned PN Points earned!"
                LocalNotificationHelper.postNotification(
                    context = context,
                    title = "+$totalEarned PN Points!",
                    body = "Sent snap to ${recipients.size} friend(s).",
                    notificationId = System.currentTimeMillis().toInt()
                )
            } else {
                _bannerMessage.value = "Failed to send snap. Please check connection."
            }
        }
    }

    private fun scheduleAutoSeenSimulation(
        context: Context,
        snapId: String,
        friend: SimulatedFriendProfile,
        viewOnce: Boolean,
        fastMode: Boolean
    ) {
        viewModelScope.launch {
            // SENDING -> DELIVERED within 2-4 seconds
            delay(Random.nextLong(2_000L, 4_500L))
            firestoreRepo.updateSentSnapStatus(currentUserId, snapId, "DELIVERED")

            // Auto-Open Simulation: realistic 5-10 minutes (300s-600s) or fast preview (8s-25s)
            val openDelayMs = if (fastMode) {
                Random.nextLong(8_000L, 25_000L)
            } else {
                Random.nextLong(300_000L, 600_000L)
            }
            delay(openDelayMs)
            firestoreRepo.updateSentSnapStatus(currentUserId, snapId, "OPENED")
            firestoreRepo.addNotification(
                userId = currentUserId,
                category = "snap",
                title = "${friend.name} opened your Snap",
                body = "Status updated to Opened.",
                relatedId = snapId
            )
            LocalNotificationHelper.postNotification(
                context = context,
                title = "${friend.name} opened your Snap",
                body = "Your snap was viewed!",
                notificationId = snapId.hashCode()
            )

            if (viewOnce) {
                delay(8_000L)
                firestoreRepo.updateSentSnapStatus(currentUserId, snapId, "EXPIRED")
            }
        }
    }

    fun openChat(friendId: String?) {
        _activeChatFriendId.value = friendId
        _aiChatError.value = null
        if (!friendId.isNullOrBlank()) {
            viewModelScope.launch {
                firestoreRepo.upsertFriendState(
                    userId = currentUserId,
                    friendId = friendId,
                    unreadCount = 0L
                )
            }
        }
    }

    fun sendChatMessageWithAiReply(
        friend: SimulatedFriendProfile,
        messageType: String,
        text: String,
        mediaUri: String? = null
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank() && mediaUri.isNullOrBlank()) return
        viewModelScope.launch {
            _aiChatError.value = null
            val sendRes = firestoreRepo.sendChatMessage(
                userId = currentUserId,
                friendId = friend.id,
                senderRole = "user",
                messageType = messageType,
                text = cleanText,
                mediaUri = mediaUri
            )
            if (sendRes.isFailure) {
                _bannerMessage.value = "Failed to send message."
                return@launch
            }

            // Trigger Gemini AI reply from simulated Indian friend
            _friendTypingId.value = friend.id
            val history = (activeChatMessages.value as? UiState.Success)?.data.orEmpty()
            val promptInput = if (messageType == "voice") {
                "[Sent a voice note: $cleanText]"
            } else if (messageType == "photo") {
                "[Sent a photo: $cleanText]"
            } else {
                cleanText
            }
            val aiResult = aiChatRepo.generateFriendReply(friend, history, promptInput)
            _friendTypingId.value = null

            aiResult.onSuccess { reply ->
                firestoreRepo.sendChatMessage(
                    userId = currentUserId,
                    friendId = friend.id,
                    senderRole = "friend",
                    messageType = "text",
                    text = reply
                )
                firestoreRepo.addNotification(
                    userId = currentUserId,
                    category = "chat",
                    title = "New Chat from ${friend.name}",
                    body = reply.take(120),
                    relatedId = friend.id
                )
            }.onFailure { err ->
                _aiChatError.value = err.localizedMessage ?: "AI reply unavailable right now."
            }
        }
    }

    fun reactToMessage(friendId: String, messageId: String, emoji: String) {
        viewModelScope.launch {
            firestoreRepo.updateMessageReaction(currentUserId, friendId, messageId, emoji)
        }
    }

    fun setFriendRelation(friendId: String, status: String, reported: Boolean? = null) {
        viewModelScope.launch {
            firestoreRepo.upsertFriendState(
                userId = currentUserId,
                friendId = friendId,
                relationStatus = status,
                reported = reported
            )
            val label = when {
                reported == true -> "User reported and blocked."
                status == "blocked" -> "User blocked."
                status == "friend" -> "Friend added!"
                status == "none" -> "Friend removed."
                else -> "Updated."
            }
            _bannerMessage.value = label
        }
    }

    fun createStory(
        storyType: String,
        caption: String,
        filterName: String,
        musicTrack: String,
        privacy: String,
        bgColorHex: String,
        localMediaPath: String? = null
    ) {
        viewModelScope.launch {
            val author = (userProfileState.value as? UiState.Success)?.data?.displayName ?: "Nilesh User"
            val res = firestoreRepo.createStory(
                userId = currentUserId,
                authorName = author,
                storyType = storyType,
                caption = caption,
                filterName = filterName,
                musicTrack = musicTrack,
                privacy = privacy,
                bgColorHex = bgColorHex,
                localMediaPath = localMediaPath
            )
            if (res.isSuccess) {
                firestoreRepo.addNotification(
                    userId = currentUserId,
                    category = "story",
                    title = "Story Published (24h)",
                    body = "Your $storyType story is live for 24 hours."
                )
                _bannerMessage.value = "Story posted for 24 hours!"
            } else {
                _bannerMessage.value = "Failed to publish story."
            }
        }
    }

    fun incrementStoryView(storyId: String) {
        viewModelScope.launch {
            firestoreRepo.incrementStoryView(currentUserId, storyId)
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteStory(currentUserId, storyId)
            _bannerMessage.value = "Story deleted."
        }
    }

    fun saveNote(noteId: String?, title: String, content: String, colorTag: String, pinned: Boolean) {
        viewModelScope.launch {
            val res = firestoreRepo.saveNote(currentUserId, noteId, title, content, colorTag, pinned)
            if (res.isSuccess) _bannerMessage.value = "Note saved!"
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteNote(currentUserId, noteId)
            _bannerMessage.value = "Note deleted."
        }
    }

    fun updateSettings(updates: Map<String, Any?>) {
        viewModelScope.launch {
            firestoreRepo.updateProfileSettings(currentUserId, updates)
        }
    }

    fun updateUserLocation(cityLabel: String, lat: Double, lng: Double) {
        _userApproxCity.value = cityLabel
        _userLatLng.value = Pair(lat, lng)
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            firestoreRepo.markNotificationRead(currentUserId, notifId)
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            firestoreRepo.deleteUserProfile(currentUserId)
            onComplete()
        }
    }

    fun resetLocalDataAndReinit(displayName: String = "Nilesh User") {
        viewModelScope.launch {
            firestoreRepo.deleteUserProfile(currentUserId)
            firestoreRepo.ensureUserProfile(currentUserId, displayName, null)
            seedInitialSimulatedDataIfEmpty()
            _bannerMessage.value = "Local app data reset!"
        }
    }

    fun showBanner(message: String) {
        _bannerMessage.value = message
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    fun dismissPnCelebration() {
        _pnCelebration.value = null
    }

    private fun <T> Flow<T>.toUiStateRaw(): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("MainViewModel", "Stream error", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to sync data"))
            }

    private fun <T> Flow<T>.toUiStateFlow(fallbackMsg: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("MainViewModel", fallbackMsg, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackMsg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private fun isPureCancellation(error: Throwable): Boolean {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        return firestoreError == null && error is CancellationException
    }
}
