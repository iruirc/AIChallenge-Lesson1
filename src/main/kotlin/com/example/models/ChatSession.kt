package com.example.models

import java.util.*

/**
 * Представляет сессию чата с историей сообщений.
 * Каждая сессия хранит полную историю диалога между пользователем и Claude.
 */
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    private val _messages: MutableList<ClaudeMessage> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var lastAccessedAt: Long = System.currentTimeMillis()
) {
    /**
     * Возвращает копию списка сообщений
     */
    val messages: List<ClaudeMessage>
        get() = _messages.toList()

    /**
     * Добавляет сообщение в историю сессии
     */
    fun addMessage(role: MessageRole, content: String) {
        _messages.add(ClaudeMessage(role = role, content = content))
        lastAccessedAt = System.currentTimeMillis()
    }

    /**
     * Очищает историю сообщений
     */
    fun clear() {
        _messages.clear()
        lastAccessedAt = System.currentTimeMillis()
    }
}
