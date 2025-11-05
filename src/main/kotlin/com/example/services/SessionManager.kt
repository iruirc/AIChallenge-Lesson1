package com.example.services

import com.example.models.Session
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages chat sessions and their message history
 * Thread-safe implementation using ConcurrentHashMap and Mutex
 */
class SessionManager(
    private val sessionTimeoutSeconds: Long = 3600 // 1 hour default
) {
    private val logger = LoggerFactory.getLogger(SessionManager::class.java)
    private val sessions = ConcurrentHashMap<String, Session>()
    private val mutex = Mutex()

    /**
     * Get or create a session by ID
     */
    suspend fun getOrCreateSession(sessionId: String?): Session = mutex.withLock {
        if (sessionId != null && sessions.containsKey(sessionId)) {
            val session = sessions[sessionId]!!
            session.lastAccessedAt = Instant.now().epochSecond
            logger.info("Retrieved existing session: $sessionId (${session.messages.size} messages)")
            return@withLock session
        }

        val newSession = Session()
        sessions[newSession.id] = newSession
        logger.info("Created new session: ${newSession.id}")
        return@withLock newSession
    }

    /**
     * Get a session by ID (without creating a new one)
     */
    fun getSession(sessionId: String): Session? {
        return sessions[sessionId]
    }

    /**
     * Clear all messages in a session
     */
    suspend fun clearSession(sessionId: String): Boolean = mutex.withLock {
        val session = sessions[sessionId]
        if (session != null) {
            session.clear()
            logger.info("Cleared session: $sessionId")
            return@withLock true
        }
        logger.warn("Attempted to clear non-existent session: $sessionId")
        return@withLock false
    }

    /**
     * Delete a session completely
     */
    suspend fun deleteSession(sessionId: String): Boolean = mutex.withLock {
        val removed = sessions.remove(sessionId)
        if (removed != null) {
            logger.info("Deleted session: $sessionId")
            return@withLock true
        }
        logger.warn("Attempted to delete non-existent session: $sessionId")
        return@withLock false
    }

    /**
     * Clean up expired sessions
     * Should be called periodically by a background job
     */
    suspend fun cleanupExpiredSessions(): Int = mutex.withLock {
        val now = Instant.now().epochSecond
        val expiredSessions = sessions.filter { (_, session) ->
            now - session.lastAccessedAt > sessionTimeoutSeconds
        }

        expiredSessions.forEach { (id, _) ->
            sessions.remove(id)
            logger.info("Removed expired session: $id")
        }

        val count = expiredSessions.size
        if (count > 0) {
            logger.info("Cleaned up $count expired sessions")
        }
        return@withLock count
    }

    /**
     * Get statistics about current sessions
     */
    fun getStats(): SessionStats {
        return SessionStats(
            totalSessions = sessions.size,
            totalMessages = sessions.values.sumOf { it.messages.size }
        )
    }
}

data class SessionStats(
    val totalSessions: Int,
    val totalMessages: Int
)
