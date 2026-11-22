package com.example.data.repository

import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.FriendState
import com.example.data.model.PnHistoryEntry
import com.example.data.model.SentSnap
import com.example.data.model.UserNote
import com.example.data.model.UserProfile
import com.example.data.model.UserStory
import kotlinx.coroutines.flow.Flow

interface SnapDataRepository {
    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?
    ): Result<Unit>

    fun observeUserProfile(userId: String): Flow<UserProfile?>

    suspend fun updateProfileSettings(
        userId: String,
        updates: Map<String, Any?>
    ): Result<Unit>

    fun observeNotes(userId: String): Flow<List<UserNote>>

    suspend fun saveNote(
        userId: String,
        noteId: String?,
        title: String,
        content: String,
        colorTag: String,
        pinned: Boolean
    ): Result<Unit>

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit>

    fun observeSentSnaps(userId: String): Flow<List<SentSnap>>

    suspend fun sendSnapAndAwardPnPoints(
        userId: String,
        recipientId: String,
        recipientName: String,
        caption: String,
        filterName: String,
        mediaType: String,
        viewOnce: Boolean,
        replayAllowed: Boolean,
        timerSeconds: Int
    ): Result<String>

    suspend fun updateSentSnapStatus(
        userId: String,
        snapId: String,
        newStatus: String
    ): Result<Unit>

    fun observePnHistory(userId: String): Flow<List<PnHistoryEntry>>

    fun observeStories(userId: String): Flow<List<UserStory>>

    suspend fun createStory(
        userId: String,
        authorName: String,
        storyType: String,
        caption: String,
        filterName: String,
        musicTrack: String,
        privacy: String,
        bgColorHex: String,
        localMediaPath: String?
    ): Result<Unit>

    suspend fun incrementStoryView(userId: String, storyId: String): Result<Unit>

    suspend fun deleteStory(userId: String, storyId: String): Result<Unit>

    fun observeFriendStates(userId: String): Flow<List<FriendState>>

    suspend fun upsertFriendState(
        userId: String,
        friendId: String,
        relationStatus: String? = null,
        reported: Boolean? = null,
        unreadCount: Long? = null,
        lastMessage: String? = null
    ): Result<Unit>

    fun observeChatMessages(userId: String, friendId: String): Flow<List<ChatMessage>>

    suspend fun sendChatMessage(
        userId: String,
        friendId: String,
        senderRole: String,
        messageType: String,
        text: String,
        mediaUri: String? = null
    ): Result<Unit>

    suspend fun updateMessageReaction(
        userId: String,
        friendId: String,
        messageId: String,
        reaction: String
    ): Result<Unit>

    fun observeNotifications(userId: String): Flow<List<AppNotification>>

    suspend fun addNotification(
        userId: String,
        category: String,
        title: String,
        body: String,
        relatedId: String? = null
    ): Result<Unit>

    suspend fun markNotificationRead(userId: String, notifId: String): Result<Unit>

    suspend fun deleteUserProfile(userId: String): Result<Unit>
}
