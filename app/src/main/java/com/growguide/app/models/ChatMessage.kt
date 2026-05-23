package com.growguide.app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * A chat message between the user and the AI assistant.
 * Stored in Firestore at: users/{userId}/plants/{plantId}/chats/{chatId}
 *
 * @property role Either "user" or "assistant"
 * @property content The message text
 * @property createdAt Timestamp when the message was sent
 */
data class ChatMessage(
    @get:PropertyName("role")
    @set:PropertyName("role")
    var role: String = "",

    @get:PropertyName("content")
    @set:PropertyName("content")
    var content: String = "",

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null
)
