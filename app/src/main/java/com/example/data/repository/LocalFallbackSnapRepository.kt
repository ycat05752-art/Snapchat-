package com.example.data.repository

import android.content.Context
import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.FriendState
import com.example.data.model.PnHistoryEntry
import com.example.data.model.SentSnap
import com.example.data.model.UserNote
import com.example.data.model.UserProfile
import com.example.data.model.UserStory
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

class LocalFallbackSnapRepository(context: Context) : SnapDataRepository {

    private val prefs = context.getSharedPreferences("nilesh_local_store", Context.MODE_PRIVATE)

    private val profileFlow = MutableStateFlow<UserProfile?>(loadProfile())
    private val notesFlow = MutableStateFlow<List<UserNote>>(loadNotes())
    private val sentSnapsFlow = MutableStateFlow<List<SentSnap>>(loadSentSnaps())
    private val pnHistoryFlow = MutableStateFlow<List<PnHistoryEntry>>(loadPnHistory())
    private val storiesFlow = MutableStateFlow<List<UserStory>>(loadStories())
    private val friendStatesFlow = MutableStateFlow<List<FriendState>>(loadFriendStates())
    private val chatMessagesFlow = MutableStateFlow<Map<String, List<ChatMessage>>>(loadAllChats())
    private val notificationsFlow = MutableStateFlow<List<AppNotification>>(loadNotifications())

    private fun nowTs(millis: Long = System.currentTimeMillis()): Timestamp = Timestamp(Date(millis))

    override suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?
    ): Result<Unit> {
        if (profileFlow.value == null) {
            val safeName = displayName.ifBlank { "Nilesh User" }.take(50)
            val created = UserProfile(
                id = userId,
                userId = userId,
                displayName = safeName,
                username = "nilesh_snap",
                bio = "Snapping daily on NILESH SNAPCHAT",
                email = email?.take(120) ?: "",
                createdAt = nowTs(),
                updatedAt = nowTs()
            )
            profileFlow.value = created
            saveProfile(created)
        }
        return Result.success(Unit)
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = profileFlow

    override suspend fun updateProfileSettings(
        userId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        val current = profileFlow.value ?: UserProfile(id = userId, userId = userId)
        val updated = current.copy(
            displayName = (updates["displayName"] as? String) ?: current.displayName,
            username = (updates["username"] as? String) ?: current.username,
            bio = (updates["bio"] as? String) ?: current.bio,
            email = (updates["email"] as? String) ?: current.email,
            phone = (updates["phone"] as? String) ?: current.phone,
            avatarColorHex = (updates["avatarColorHex"] as? String) ?: current.avatarColorHex,
            darkMode = (updates["darkMode"] as? Boolean) ?: current.darkMode,
            locationSharingEnabled = (updates["locationSharingEnabled"] as? Boolean) ?: current.locationSharingEnabled,
            ghostMode = (updates["ghostMode"] as? Boolean) ?: current.ghostMode,
            whoCanContact = (updates["whoCanContact"] as? String) ?: current.whoCanContact,
            whoCanViewStory = (updates["whoCanViewStory"] as? String) ?: current.whoCanViewStory,
            simAutoSnapEnabled = (updates["simAutoSnapEnabled"] as? Boolean) ?: current.simAutoSnapEnabled,
            simOpenSpeed = (updates["simOpenSpeed"] as? String) ?: current.simOpenSpeed,
            soundsEnabled = (updates["soundsEnabled"] as? Boolean) ?: current.soundsEnabled,
            vibrationEnabled = (updates["vibrationEnabled"] as? Boolean) ?: current.vibrationEnabled,
            appLanguage = (updates["appLanguage"] as? String) ?: current.appLanguage,
            uploadQuality = (updates["uploadQuality"] as? String) ?: current.uploadQuality,
            downloadQuality = (updates["downloadQuality"] as? String) ?: current.downloadQuality,
            notificationsEnabled = (updates["notificationsEnabled"] as? Boolean) ?: current.notificationsEnabled,
            updatedAt = nowTs()
        )
        profileFlow.value = updated
        saveProfile(updated)
        return Result.success(Unit)
    }

    override fun observeNotes(userId: String): Flow<List<UserNote>> = notesFlow

    override suspend fun saveNote(
        userId: String,
        noteId: String?,
        title: String,
        content: String,
        colorTag: String,
        pinned: Boolean
    ): Result<Unit> {
        val list = notesFlow.value.toMutableList()
        val safeTitle = title.trim().ifBlank { "Quick Note" }.take(100)
        val safeContent = content.trim().take(2000)
        val ts = nowTs()
        if (noteId.isNullOrBlank()) {
            val newNote = UserNote(
                id = "note_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                userId = userId,
                title = safeTitle,
                content = safeContent,
                colorTag = colorTag.take(20),
                pinned = pinned,
                createdAt = ts,
                updatedAt = ts
            )
            list.add(0, newNote)
        } else {
            val idx = list.indexOfFirst { it.id == noteId }
            if (idx >= 0) {
                list[idx] = list[idx].copy(
                    title = safeTitle,
                    content = safeContent,
                    colorTag = colorTag.take(20),
                    pinned = pinned,
                    updatedAt = ts
                )
            }
        }
        notesFlow.value = list
        saveNotes(list)
        return Result.success(Unit)
    }

    override suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val list = notesFlow.value.filterNot { it.id == noteId }
        notesFlow.value = list
        saveNotes(list)
        return Result.success(Unit)
    }

    override fun observeSentSnaps(userId: String): Flow<List<SentSnap>> = sentSnapsFlow

    override suspend fun sendSnapAndAwardPnPoints(
        userId: String,
        recipientId: String,
        recipientName: String,
        caption: String,
        filterName: String,
        mediaType: String,
        viewOnce: Boolean,
        replayAllowed: Boolean,
        timerSeconds: Int
    ): Result<String> {
        val ts = nowTs()
        val snapId = "snap_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val snap = SentSnap(
            id = snapId,
            userId = userId,
            recipientId = recipientId.take(128),
            recipientName = recipientName.take(60),
            caption = caption.take(200),
            filterName = filterName.take(80),
            mediaType = if (mediaType == "video") "video" else "photo",
            viewOnce = viewOnce,
            replayAllowed = replayAllowed,
            timerSeconds = timerSeconds.coerceIn(1, 60).toLong(),
            status = "SENT",
            pnAwarded = 1000L,
            createdAt = ts,
            updatedAt = ts
        )
        val snaps = listOf(snap) + sentSnapsFlow.value
        sentSnapsFlow.value = snaps
        saveSentSnaps(snaps)

        val currentProfile = profileFlow.value ?: UserProfile(id = userId, userId = userId)
        val updatedProfile = currentProfile.copy(
            pnPoints = currentProfile.pnPoints + 1000L,
            totalSnapsSent = currentProfile.totalSnapsSent + 1L,
            updatedAt = ts
        )
        profileFlow.value = updatedProfile
        saveProfile(updatedProfile)

        val historyEntry = PnHistoryEntry(
            id = "pn_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            userId = userId,
            delta = 1000L,
            reason = "Snap sent to ${recipientName.take(50)}",
            snapId = snapId,
            createdAt = ts
        )
        val history = listOf(historyEntry) + pnHistoryFlow.value.take(99)
        pnHistoryFlow.value = history
        savePnHistory(history)

        addNotification(
            userId = userId,
            category = "pn_points",
            title = "+1000 PN Points Earned!",
            body = "Sent $mediaType snap to ${recipientName.take(40)}.",
            relatedId = snapId
        )
        upsertFriendState(
            userId = userId,
            friendId = recipientId,
            lastMessage = "Sent a Snap • +1000 PN",
            unreadCount = 0L
        )
        return Result.success(snapId)
    }

    override suspend fun updateSentSnapStatus(
        userId: String,
        snapId: String,
        newStatus: String
    ): Result<Unit> {
        val list = sentSnapsFlow.value.map {
            if (it.id == snapId && it.status != "EXPIRED") it.copy(status = newStatus, updatedAt = nowTs()) else it
        }
        sentSnapsFlow.value = list
        saveSentSnaps(list)
        return Result.success(Unit)
    }

    override fun observePnHistory(userId: String): Flow<List<PnHistoryEntry>> = pnHistoryFlow

    override fun observeStories(userId: String): Flow<List<UserStory>> =
        storiesFlow.map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.expiresAtMillis > now }
        }

    override suspend fun createStory(
        userId: String,
        authorName: String,
        storyType: String,
        caption: String,
        filterName: String,
        musicTrack: String,
        privacy: String,
        bgColorHex: String,
        localMediaPath: String?
    ): Result<Unit> {
        val ts = nowTs()
        val story = UserStory(
            id = "story_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            userId = userId,
            authorName = authorName.take(60).ifBlank { "Nilesh User" },
            storyType = storyType,
            caption = caption.take(300),
            filterName = filterName.take(80),
            musicTrack = musicTrack.take(80),
            privacy = privacy,
            bgColorHex = bgColorHex.take(12),
            localMediaPath = localMediaPath?.take(300),
            viewCount = 0L,
            expiresAtMillis = System.currentTimeMillis() + 24L * 60L * 60L * 1000L,
            createdAt = ts,
            updatedAt = ts
        )
        val list = listOf(story) + storiesFlow.value
        storiesFlow.value = list
        saveStories(list)
        return Result.success(Unit)
    }

    override suspend fun incrementStoryView(userId: String, storyId: String): Result<Unit> {
        val list = storiesFlow.value.map {
            if (it.id == storyId) it.copy(viewCount = it.viewCount + 1L, updatedAt = nowTs()) else it
        }
        storiesFlow.value = list
        saveStories(list)
        return Result.success(Unit)
    }

    override suspend fun deleteStory(userId: String, storyId: String): Result<Unit> {
        val list = storiesFlow.value.filterNot { it.id == storyId }
        storiesFlow.value = list
        saveStories(list)
        return Result.success(Unit)
    }

    override fun observeFriendStates(userId: String): Flow<List<FriendState>> = friendStatesFlow

    override suspend fun upsertFriendState(
        userId: String,
        friendId: String,
        relationStatus: String?,
        reported: Boolean?,
        unreadCount: Long?,
        lastMessage: String?
    ): Result<Unit> {
        val list = friendStatesFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.friendId == friendId }
        val ts = nowTs()
        if (idx >= 0) {
            val cur = list[idx]
            list[idx] = cur.copy(
                relationStatus = relationStatus ?: cur.relationStatus,
                reported = reported ?: cur.reported,
                unreadCount = unreadCount ?: cur.unreadCount,
                lastMessage = lastMessage?.take(200) ?: cur.lastMessage,
                updatedAt = ts
            )
        } else {
            list.add(
                FriendState(
                    id = friendId,
                    userId = userId,
                    friendId = friendId,
                    relationStatus = relationStatus ?: "friend",
                    reported = reported ?: false,
                    unreadCount = unreadCount ?: 0L,
                    lastMessage = lastMessage?.take(200) ?: "",
                    createdAt = ts,
                    updatedAt = ts
                )
            )
        }
        friendStatesFlow.value = list
        saveFriendStates(list)
        return Result.success(Unit)
    }

    override fun observeChatMessages(userId: String, friendId: String): Flow<List<ChatMessage>> =
        chatMessagesFlow.map { it[friendId].orEmpty() }

    override suspend fun sendChatMessage(
        userId: String,
        friendId: String,
        senderRole: String,
        messageType: String,
        text: String,
        mediaUri: String?
    ): Result<Unit> {
        val ts = nowTs()
        val msg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            userId = userId,
            friendId = friendId,
            senderRole = senderRole,
            messageType = messageType,
            text = text.take(2000),
            mediaUri = mediaUri?.take(300),
            deliveryStatus = if (senderRole == "user") "DELIVERED" else "SEEN",
            createdAt = ts,
            updatedAt = ts
        )
        val map = chatMessagesFlow.value.toMutableMap()
        map[friendId] = map[friendId].orEmpty() + msg
        chatMessagesFlow.value = map
        saveAllChats(map)

        val preview = when (messageType) {
            "photo" -> "📷 Photo"
            "video" -> "🎥 Video"
            "voice" -> "🎤 Voice note"
            else -> text
        }
        upsertFriendState(userId, friendId, lastMessage = preview.take(200), unreadCount = 0L)
        return Result.success(Unit)
    }

    override suspend fun updateMessageReaction(
        userId: String,
        friendId: String,
        messageId: String,
        reaction: String
    ): Result<Unit> {
        val map = chatMessagesFlow.value.toMutableMap()
        map[friendId] = map[friendId].orEmpty().map {
            if (it.id == messageId) it.copy(reaction = reaction.take(16), updatedAt = nowTs()) else it
        }
        chatMessagesFlow.value = map
        saveAllChats(map)
        return Result.success(Unit)
    }

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> = notificationsFlow

    override suspend fun addNotification(
        userId: String,
        category: String,
        title: String,
        body: String,
        relatedId: String?
    ): Result<Unit> {
        val ts = nowTs()
        val notif = AppNotification(
            id = "notif_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            userId = userId,
            category = category,
            title = title.trim().ifBlank { "Update" }.take(100),
            body = body.trim().ifBlank { "Notification from NILESH SNAPCHAT" }.take(250),
            readFlag = false,
            relatedId = relatedId?.take(128),
            createdAt = ts,
            updatedAt = ts
        )
        val list = (listOf(notif) + notificationsFlow.value).take(80)
        notificationsFlow.value = list
        saveNotifications(list)
        return Result.success(Unit)
    }

    override suspend fun markNotificationRead(userId: String, notifId: String): Result<Unit> {
        val list = notificationsFlow.value.map {
            if (it.id == notifId) it.copy(readFlag = true, updatedAt = nowTs()) else it
        }
        notificationsFlow.value = list
        saveNotifications(list)
        return Result.success(Unit)
    }

    override suspend fun deleteUserProfile(userId: String): Result<Unit> {
        prefs.edit().clear().apply()
        profileFlow.value = null
        notesFlow.value = emptyList()
        sentSnapsFlow.value = emptyList()
        pnHistoryFlow.value = emptyList()
        storiesFlow.value = emptyList()
        friendStatesFlow.value = emptyList()
        chatMessagesFlow.value = emptyMap()
        notificationsFlow.value = emptyList()
        return Result.success(Unit)
    }

    private fun saveProfile(p: UserProfile) {
        val obj = JSONObject().apply {
            put("id", p.id)
            put("userId", p.userId)
            put("displayName", p.displayName)
            put("username", p.username)
            put("bio", p.bio)
            put("email", p.email)
            put("phone", p.phone)
            put("avatarColorHex", p.avatarColorHex)
            put("pnPoints", p.pnPoints)
            put("totalSnapsSent", p.totalSnapsSent)
            put("darkMode", p.darkMode)
            put("locationSharingEnabled", p.locationSharingEnabled)
            put("ghostMode", p.ghostMode)
            put("whoCanContact", p.whoCanContact)
            put("whoCanViewStory", p.whoCanViewStory)
            put("simAutoSnapEnabled", p.simAutoSnapEnabled)
            put("simOpenSpeed", p.simOpenSpeed)
            put("soundsEnabled", p.soundsEnabled)
            put("vibrationEnabled", p.vibrationEnabled)
            put("appLanguage", p.appLanguage)
            put("uploadQuality", p.uploadQuality)
            put("downloadQuality", p.downloadQuality)
            put("notificationsEnabled", p.notificationsEnabled)
        }
        prefs.edit().putString("profile", obj.toString()).apply()
    }

    private fun loadProfile(): UserProfile? {
        val raw = prefs.getString("profile", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            UserProfile(
                id = o.optString("id"),
                userId = o.optString("userId"),
                displayName = o.optString("displayName", "Nilesh User"),
                username = o.optString("username", "nilesh_snap"),
                bio = o.optString("bio"),
                email = o.optString("email"),
                phone = o.optString("phone"),
                avatarColorHex = o.optString("avatarColorHex", "#FFFC00"),
                pnPoints = o.optLong("pnPoints", 0L),
                totalSnapsSent = o.optLong("totalSnapsSent", 0L),
                darkMode = o.optBoolean("darkMode", true),
                locationSharingEnabled = o.optBoolean("locationSharingEnabled", false),
                ghostMode = o.optBoolean("ghostMode", true),
                whoCanContact = o.optString("whoCanContact", "everyone"),
                whoCanViewStory = o.optString("whoCanViewStory", "friends"),
                simAutoSnapEnabled = o.optBoolean("simAutoSnapEnabled", true),
                simOpenSpeed = o.optString("simOpenSpeed", "realistic"),
                soundsEnabled = o.optBoolean("soundsEnabled", true),
                vibrationEnabled = o.optBoolean("vibrationEnabled", true),
                appLanguage = o.optString("appLanguage", "english"),
                uploadQuality = o.optString("uploadQuality", "high"),
                downloadQuality = o.optString("downloadQuality", "high"),
                notificationsEnabled = o.optBoolean("notificationsEnabled", true),
                createdAt = nowTs(),
                updatedAt = nowTs()
            )
        }.getOrNull()
    }

    private fun saveNotes(list: List<UserNote>) {
        val arr = JSONArray()
        list.forEach { n ->
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("userId", n.userId)
                put("title", n.title)
                put("content", n.content)
                put("colorTag", n.colorTag)
                put("pinned", n.pinned)
                put("ms", n.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
            })
        }
        prefs.edit().putString("notes", arr.toString()).apply()
    }

    private fun loadNotes(): List<UserNote> = runCatching {
        val arr = JSONArray(prefs.getString("notes", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ts = nowTs(o.optLong("ms", System.currentTimeMillis()))
            UserNote(o.optString("id"), o.optString("userId"), o.optString("title"), o.optString("content"), o.optString("colorTag", "YELLOW"), o.optBoolean("pinned"), ts, ts)
        }
    }.getOrDefault(emptyList())

    private fun saveSentSnaps(list: List<SentSnap>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id); put("userId", s.userId); put("recipientId", s.recipientId)
                put("recipientName", s.recipientName); put("caption", s.caption); put("filterName", s.filterName)
                put("mediaType", s.mediaType); put("viewOnce", s.viewOnce); put("replayAllowed", s.replayAllowed)
                put("timerSeconds", s.timerSeconds); put("status", s.status); put("pnAwarded", s.pnAwarded)
                put("ms", s.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            })
        }
        prefs.edit().putString("sent_snaps", arr.toString()).apply()
    }

    private fun loadSentSnaps(): List<SentSnap> = runCatching {
        val arr = JSONArray(prefs.getString("sent_snaps", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ts = nowTs(o.optLong("ms", System.currentTimeMillis()))
            SentSnap(o.optString("id"), o.optString("userId"), o.optString("recipientId"), o.optString("recipientName"), o.optString("caption"), o.optString("filterName", "Original"), o.optString("mediaType", "photo"), o.optBoolean("viewOnce", true), o.optBoolean("replayAllowed", true), o.optLong("timerSeconds", 5L), o.optString("status", "SENT"), o.optLong("pnAwarded", 1000L), ts, ts)
        }
    }.getOrDefault(emptyList())

    private fun savePnHistory(list: List<PnHistoryEntry>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("userId", p.userId); put("delta", p.delta)
                put("reason", p.reason); put("snapId", p.snapId ?: ""); put("ms", p.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            })
        }
        prefs.edit().putString("pn_history", arr.toString()).apply()
    }

    private fun loadPnHistory(): List<PnHistoryEntry> = runCatching {
        val arr = JSONArray(prefs.getString("pn_history", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ts = nowTs(o.optLong("ms", System.currentTimeMillis()))
            PnHistoryEntry(o.optString("id"), o.optString("userId"), o.optLong("delta", 1000L), o.optString("reason"), o.optString("snapId").ifBlank { null }, ts)
        }
    }.getOrDefault(emptyList())

    private fun saveStories(list: List<UserStory>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id); put("userId", s.userId); put("authorName", s.authorName)
                put("storyType", s.storyType); put("caption", s.caption); put("filterName", s.filterName)
                put("musicTrack", s.musicTrack); put("privacy", s.privacy); put("bgColorHex", s.bgColorHex)
                put("localMediaPath", s.localMediaPath ?: ""); put("viewCount", s.viewCount); put("expiresAtMillis", s.expiresAtMillis)
                put("ms", s.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            })
        }
        prefs.edit().putString("stories", arr.toString()).apply()
    }

    private fun loadStories(): List<UserStory> = runCatching {
        val arr = JSONArray(prefs.getString("stories", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ts = nowTs(o.optLong("ms", System.currentTimeMillis()))
            UserStory(o.optString("id"), o.optString("userId"), o.optString("authorName"), o.optString("storyType", "text"), o.optString("caption"), o.optString("filterName"), o.optString("musicTrack"), o.optString("privacy", "friends"), o.optString("bgColorHex", "#FFFC00"), o.optString("localMediaPath").ifBlank { null }, o.optLong("viewCount"), o.optLong("expiresAtMillis"), ts, ts)
        }
    }.getOrDefault(emptyList())

    private fun saveFriendStates(list: List<FriendState>) {
        val arr = JSONArray()
        list.forEach { f ->
            arr.put(JSONObject().apply {
                put("id", f.id); put("userId", f.userId); put("friendId", f.friendId)
                put("relationStatus", f.relationStatus); put("reported", f.reported)
                put("unreadCount", f.unreadCount); put("lastMessage", f.lastMessage)
            })
        }
        prefs.edit().putString("friend_states", arr.toString()).apply()
    }

    private fun loadFriendStates(): List<FriendState> = runCatching {
        val arr = JSONArray(prefs.getString("friend_states", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            FriendState(o.optString("id"), o.optString("userId"), o.optString("friendId"), o.optString("relationStatus", "friend"), o.optBoolean("reported"), o.optLong("unreadCount"), o.optString("lastMessage"), nowTs(), nowTs())
        }
    }.getOrDefault(emptyList())

    private fun saveAllChats(map: Map<String, List<ChatMessage>>) {
        val root = JSONObject()
        map.forEach { (fid, msgs) ->
            val arr = JSONArray()
            msgs.forEach { m ->
                arr.put(JSONObject().apply {
                    put("id", m.id); put("userId", m.userId); put("friendId", m.friendId)
                    put("senderRole", m.senderRole); put("messageType", m.messageType); put("text", m.text)
                    put("mediaUri", m.mediaUri ?: ""); put("reaction", m.reaction ?: ""); put("deliveryStatus", m.deliveryStatus)
                    put("ms", m.createdAt?.toDate()?.time ?: System.currentTimeMillis())
                })
            }
            root.put(fid, arr)
        }
        prefs.edit().putString("chats", root.toString()).apply()
    }

    private fun loadAllChats(): Map<String, List<ChatMessage>> = runCatching {
        val root = JSONObject(prefs.getString("chats", "{}"))
        val out = mutableMapOf<String, List<ChatMessage>>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val fid = keys.next()
            val arr = root.getJSONArray(fid)
            out[fid] = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ts = nowTs(o.optLong("ms", System.currentTimeMillis()))
                ChatMessage(o.optString("id"), o.optString("userId"), o.optString("friendId"), o.optString("senderRole", "user"), o.optString("messageType", "text"), o.optString("text"), o.optString("mediaUri").ifBlank { null }, o.optString("reaction").ifBlank { null }, o.optString("deliveryStatus", "DELIVERED"), ts, ts)
            }
        }
        out
    }.getOrDefault(emptyMap())

    private fun saveNotifications(list: List<AppNotification>) {
        val arr = JSONArray()
        list.forEach { n ->
            arr.put(JSONObject().apply {
                put("id", n.id); put("userId", n.userId); put("category", n.category)
                put("title", n.title); put("body", n.body); put("readFlag", n.readFlag)
                put("relatedId", n.relatedId ?: ""); put("ms", n.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            })
        }
        prefs.edit().putString("notifications", arr.toString()).apply()
    }

    private fun loadNotifications(): List<AppNotification> = runCatching {
        val arr = JSONArray(prefs.getString("notifications", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ts = nowTs(o.optLong("ms", System.currentTimeMillis()))
            AppNotification(o.optString("id"), o.optString("userId"), o.optString("category", "snap"), o.optString("title"), o.optString("body"), o.optBoolean("readFlag"), o.optString("relatedId").ifBlank { null }, ts, ts)
        }
    }.getOrDefault(emptyList())
}
