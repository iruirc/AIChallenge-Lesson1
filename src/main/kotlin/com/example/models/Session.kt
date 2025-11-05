package com.example.models

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Represents a chat session with message history
 */
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val messages: MutableList<MessageHistory> = mutableListOf(),
    val createdAt: Long = Instant.now().epochSecond,
    var lastAccessedAt: Long = Instant.now().epochSecond
) {
    /**
     * Add a message to the session history
     */
    fun addMessage(role: String, content: String) {
        messages.add(MessageHistory(role = role, content = content))
        lastAccessedAt = Instant.now().epochSecond
    }

    /**
     * Get messages in Claude API format
     */
    fun getClaudeMessages(): List<ClaudeMessage> {
        return messages.map { ClaudeMessage(role = it.role, content = it.content) }
    }

    /**
     * Clear all messages in the session
     */
    fun clear() {
        messages.clear()
        lastAccessedAt = Instant.now().epochSecond
    }
}

/**
 * Represents a single message in the conversation history
 */
@Serializable
data class MessageHistory(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = Instant.now().epochSecond
)
