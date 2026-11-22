package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.FriendState
import com.example.data.model.PnHistoryEntry
import com.example.data.model.SentSnap
import com.example.data.model.UserNote
import com.example.data.model.UserProfile
import com.example.data.model.UserStory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SnapFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : SnapDataRepository {
    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    override suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId(userId)
            val docRef = userDoc(userId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.ifBlank { "Nilesh User" }.take(50)
                val baseHandle = safeName.lowercase()
                    .replace(Regex("[^a-z0-9]"), "_")
                    .trim('_')
                    .ifBlank { "nilesh" }
                    .take(18)
                val username = "${baseHandle}_${userId.take(5).lowercase()}"
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to safeName,
                    "username" to username,
                    "bio" to "Snapping daily on NILESH SNAPCHAT",
                    "email" to (email?.take(120) ?: ""),
                    "phone" to "",
                    "avatarColorHex" to "#FFFC00",
                    "pnPoints" to 0L,
                    "totalSnapsSent" to 0L,
                    "darkMode" to true,
                    "locationSharingEnabled" to false,
                    "ghostMode" to true,
                    "whoCanContact" to "everyone",
                    "whoCanViewStory" to "friends",
                    "simAutoSnapEnabled" to true,
                    "simOpenSpeed" to "realistic",
                    "soundsEnabled" to true,
                    "vibrationEnabled" to true,
                    "appLanguage" to "english",
                    "uploadQuality" to "high",
                    "downloadQuality" to "high",
                    "notificationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val docRef = userDoc(uid)
        docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logIfActiveUser(error, OperationType.GET, docRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    override suspend fun updateProfileSettings(
        userId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        if (updates.isEmpty()) return Result.success(Unit)
        val path = "users/$userId"
        return try {
            requireUserId(userId)
            val clean = updates.filterValues { it != null }.toMutableMap()
            if (clean.isEmpty()) return Result.success(Unit)
            clean["updatedAt"] = FieldValue.serverTimestamp()
            userDoc(userId).update(clean).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override fun observeNotes(userId: String): Flow<List<UserNote>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("notes")
        colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(UserNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logIfActiveUser(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    override suspend fun saveNote(
        userId: String,
        noteId: String?,
        title: String,
        content: String,
        colorTag: String,
        pinned: Boolean
    ): Result<Unit> {
        val safeTitle = title.trim().ifBlank { "Quick Note" }.take(100)
        val safeContent = content.trim().take(2000)
        val path = "users/$userId/notes/${noteId ?: "new"}"
        return try {
            requireUserId(userId)
            val notesCol = userDoc(userId).collection("notes")
            if (noteId.isNullOrBlank()) {
                val docRef = notesCol.document()
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "title" to safeTitle,
                        "content" to safeContent,
                        "colorTag" to colorTag.take(20),
                        "pinned" to pinned,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                notesCol.document(noteId).update(
                    mapOf(
                        "title" to safeTitle,
                        "content" to safeContent,
                        "colorTag" to colorTag.take(20),
                        "pinned" to pinned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val path = "users/$userId/notes/$noteId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("notes").document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override fun observeSentSnaps(userId: String): Flow<List<SentSnap>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("sent_snaps")
        colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(SentSnap::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logIfActiveUser(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

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
        val path = "users/$userId/sent_snaps"
        return try {
            requireUserId(userId)
            val snapRef = userDoc(userId).collection("sent_snaps").document()
            val pnRef = userDoc(userId).collection("pn_history").document()
            val notifRef = userDoc(userId).collection("notifications").document()
            val friendStateRef = userDoc(userId).collection("friend_states").document(recipientId)
            val existingFriendState = friendStateRef.get().await()

            val batch = db.batch()
            batch.set(
                snapRef,
                mapOf(
                    "userId" to userId,
                    "recipientId" to recipientId.take(128),
                    "recipientName" to recipientName.take(60),
                    "caption" to caption.take(200),
                    "filterName" to filterName.take(80),
                    "mediaType" to if (mediaType == "video") "video" else "photo",
                    "viewOnce" to viewOnce,
                    "replayAllowed" to replayAllowed,
                    "timerSeconds" to timerSeconds.coerceIn(1, 60).toLong(),
                    "status" to "SENT",
                    "pnAwarded" to 1000L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userDoc(userId),
                mapOf(
                    "pnPoints" to FieldValue.increment(1000L),
                    "totalSnapsSent" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                pnRef,
                mapOf(
                    "userId" to userId,
                    "delta" to 1000L,
                    "reason" to "Snap sent to ${recipientName.take(50)}",
                    "snapId" to snapRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "userId" to userId,
                    "category" to "pn_points",
                    "title" to "+1000 PN Points Earned!",
                    "body" to "Sent ${mediaType} snap to ${recipientName.take(40)}.",
                    "readFlag" to false,
                    "relatedId" to snapRef.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (existingFriendState.exists()) {
                batch.update(
                    friendStateRef,
                    mapOf(
                        "lastMessage" to "Sent a Snap • +1000 PN",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    friendStateRef,
                    mapOf(
                        "userId" to userId,
                        "friendId" to recipientId.take(128),
                        "relationStatus" to "friend",
                        "reported" to false,
                        "unreadCount" to 0L,
                        "lastMessage" to "Sent a Snap • +1000 PN",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(snapRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateSentSnapStatus(
        userId: String,
        snapId: String,
        newStatus: String
    ): Result<Unit> {
        val path = "users/$userId/sent_snaps/$snapId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("sent_snaps").document(snapId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override fun observePnHistory(userId: String): Flow<List<PnHistoryEntry>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("pn_history")
        colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snap ->
                snap.toObjects(PnHistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logIfActiveUser(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    override fun observeStories(userId: String): Flow<List<UserStory>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("stories")
        colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                val now = System.currentTimeMillis()
                snap.toObjects(UserStory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.expiresAtMillis > now }
            }
            .catch { error ->
                logIfActiveUser(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
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
        val path = "users/$userId/stories"
        return try {
            requireUserId(userId)
            val docRef = userDoc(userId).collection("stories").document()
            val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
            val payload = mapOf(
                "userId" to userId,
                "authorName" to authorName.take(60).ifBlank { "Nilesh User" },
                "storyType" to storyType,
                "caption" to caption.take(300),
                "filterName" to filterName.take(80),
                "musicTrack" to musicTrack.take(80),
                "privacy" to privacy,
                "bgColorHex" to bgColorHex.take(12),
                "localMediaPath" to localMediaPath?.take(300),
                "viewCount" to 0L,
                "expiresAtMillis" to expiresAt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun incrementStoryView(userId: String, storyId: String): Result<Unit> {
        val path = "users/$userId/stories/$storyId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("stories").document(storyId).update(
                mapOf(
                    "viewCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteStory(userId: String, storyId: String): Result<Unit> {
        val path = "users/$userId/stories/$storyId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("stories").document(storyId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override fun observeFriendStates(userId: String): Flow<List<FriendState>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("friend_states")
        colRef.snapshots()
            .map { snap ->
                snap.toObjects(FriendState::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logIfActiveUser(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    override suspend fun upsertFriendState(
        userId: String,
        friendId: String,
        relationStatus: String?,
        reported: Boolean?,
        unreadCount: Long?,
        lastMessage: String?
    ): Result<Unit> {
        val path = "users/$userId/friend_states/$friendId"
        return try {
            requireUserId(userId)
            val docRef = userDoc(userId).collection("friend_states").document(friendId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
                relationStatus?.let { updates["relationStatus"] = it }
                reported?.let { updates["reported"] = it }
                unreadCount?.let { updates["unreadCount"] = it }
                lastMessage?.let { updates["lastMessage"] = it.take(200) }
                docRef.update(updates).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "friendId" to friendId,
                        "relationStatus" to (relationStatus ?: "friend"),
                        "reported" to (reported ?: false),
                        "unreadCount" to (unreadCount ?: 0L),
                        "lastMessage" to (lastMessage?.take(200) ?: ""),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override fun observeChatMessages(userId: String, friendId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("chats").document(friendId).collection("messages")
        colRef.orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logIfActiveUser(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    override suspend fun sendChatMessage(
        userId: String,
        friendId: String,
        senderRole: String,
        messageType: String,
        text: String,
        mediaUri: String?
    ): Result<Unit> {
        val path = "users/$userId/chats/$friendId/messages"
        return try {
            requireUserId(userId)
            val msgRef = userDoc(userId).collection("chats").document(friendId)
                .collection("messages").document()
            val payload = mapOf(
                "userId" to userId,
                "friendId" to friendId,
                "senderRole" to senderRole,
                "messageType" to messageType,
                "text" to text.take(2000),
                "mediaUri" to mediaUri?.take(300),
                "deliveryStatus" to if (senderRole == "user") "DELIVERED" else "SEEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            msgRef.set(payload).await()
            val preview = when (messageType) {
                "photo" -> "📷 Photo"
                "video" -> "🎥 Video"
                "voice" -> "🎤 Voice note"
                "sticker" -> text
                else -> text
            }
            upsertFriendState(
                userId = userId,
                friendId = friendId,
                lastMessage = preview.take(200),
                unreadCount = 0L
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateMessageReaction(
        userId: String,
        friendId: String,
        messageId: String,
        reaction: String
    ): Result<Unit> {
        val path = "users/$userId/chats/$friendId/messages/$messageId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("chats").document(friendId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "reaction" to reaction.take(16),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val uid = requireUserId(userId)
        val colRef = userDoc(uid).collection("notifications")
        colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .snapshots()
            .map { snap ->
                snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logIfActiveUser(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
            .collect { emit(it) }
    }

    override suspend fun addNotification(
        userId: String,
        category: String,
        title: String,
        body: String,
        relatedId: String?
    ): Result<Unit> {
        val path = "users/$userId/notifications"
        return try {
            requireUserId(userId)
            val docRef = userDoc(userId).collection("notifications").document()
            val payload = mapOf(
                "userId" to userId,
                "category" to category,
                "title" to title.trim().ifBlank { "Update" }.take(100),
                "body" to body.trim().ifBlank { "Notification from NILESH SNAPCHAT" }.take(250),
                "readFlag" to false,
                "relatedId" to relatedId?.take(128),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun markNotificationRead(userId: String, notifId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notifId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("notifications").document(notifId).update(
                mapOf(
                    "readFlag" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteUserProfile(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId(userId)
            userDoc(userId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun logIfActiveUser(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
