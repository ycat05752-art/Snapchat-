package com.example.data.repository

import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.SimulatedFriendProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "AiFriendChatRepo"

class AiFriendChatRepository(
    private val firebaseEnabled: Boolean = true
) {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun generateFriendReply(
        friend: SimulatedFriendProfile,
        recentMessages: List<ChatMessage>,
        userMessage: String
    ): Result<String> {
        if (!firebaseEnabled) {
            return Result.failure(
                IllegalStateException("Gemini AI chat requires Firebase config (app/google-services.json) in the build.")
            )
        }
        return try {
            val historyLines = recentMessages.takeLast(8).joinToString("\n") { msg ->
                val speaker = if (msg.senderRole == "user") "User" else friend.name
                "$speaker: ${msg.text}"
            }
            val prompt = buildString {
                appendLine("You are ${friend.name} (${friend.username}) from ${friend.city}, India.")
                appendLine("Bio: ${friend.bio}")
                appendLine("You are an AI simulated social friend inside the app NILESH SNAPCHAT.")
                appendLine("Reply naturally in friendly casual Hinglish or English (1-2 short sentences, fun, relatable Indian daily life vibe, with 1-2 emojis max). Never claim to be a real human outside the app.")
                if (historyLines.isNotBlank()) {
                    appendLine("Recent conversation:")
                    appendLine(historyLines)
                }
                appendLine("User just sent: $userMessage")
                appendLine("Your short reply:")
            }

            val response = withTimeout(30_000L) {
                model.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("AI returned an empty reply."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI friend reply timed out", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI friend reply failed", e)
            Result.failure(e)
        }
    }
}
