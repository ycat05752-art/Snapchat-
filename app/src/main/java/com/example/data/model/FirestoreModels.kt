package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Nilesh User",
    val username: String = "nilesh_snap",
    val bio: String = "Capturing moments on NILESH SNAPCHAT",
    val email: String = "",
    val phone: String = "",
    val avatarColorHex: String = "#FFFC00",
    val pnPoints: Long = 0L,
    val totalSnapsSent: Long = 0L,
    val darkMode: Boolean = true,
    val locationSharingEnabled: Boolean = false,
    val ghostMode: Boolean = true,
    val whoCanContact: String = "everyone",
    val whoCanViewStory: String = "friends",
    val simAutoSnapEnabled: Boolean = true,
    val simOpenSpeed: String = "realistic",
    val soundsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val appLanguage: String = "english",
    val uploadQuality: String = "high",
    val downloadQuality: String = "high",
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val colorTag: String = "YELLOW",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SentSnap(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val caption: String = "",
    val filterName: String = "Original",
    val mediaType: String = "photo",
    val viewOnce: Boolean = true,
    val replayAllowed: Boolean = true,
    val timerSeconds: Long = 5L,
    val status: String = "SENDING",
    val pnAwarded: Long = 1000L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PnHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val delta: Long = 1000L,
    val reason: String = "",
    val snapId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserStory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val storyType: String = "text",
    val caption: String = "",
    val filterName: String = "Original",
    val musicTrack: String = "",
    val privacy: String = "friends",
    val bgColorHex: String = "#FFFC00",
    val localMediaPath: String? = null,
    val viewCount: Long = 0L,
    val expiresAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendState(
    @DocumentId val id: String = "",
    val userId: String = "",
    val friendId: String = "",
    val relationStatus: String = "friend",
    val reported: Boolean = false,
    val unreadCount: Long = 0L,
    val lastMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val friendId: String = "",
    val senderRole: String = "user",
    val messageType: String = "text",
    val text: String = "",
    val mediaUri: String? = null,
    val reaction: String? = null,
    val deliveryStatus: String = "SENT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "snap",
    val title: String = "",
    val body: String = "",
    val readFlag: Boolean = false,
    val relatedId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
