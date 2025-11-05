package com.example.routes

import com.example.models.ChatRequest
import com.example.models.ChatResponse
import com.example.services.ClaudeService
import com.example.services.SessionManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoutes(claudeService: ClaudeService, sessionManager: SessionManager) {
    route("/chat") {
        post {
            try {
                val request = call.receive<ChatRequest>()

                if (request.message.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Message cannot be empty"))
                    return@post
                }

                // Get or create session
                val session = sessionManager.getOrCreateSession(request.sessionId)

                // Get conversation history
                val history = session.getClaudeMessages()

                // Send message with history
                val claudeResponse = claudeService.sendMessageWithHistory(request.message, history)

                // Add user message and assistant response to session
                session.addMessage("user", request.message)
                session.addMessage("assistant", claudeResponse)

                // Return response with session info
                call.respond(
                    ChatResponse(
                        response = claudeResponse,
                        sessionId = session.id,
                        messageCount = session.messages.size
                    )
                )

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to process request: ${e.message}")
                )
            }
        }
    }

    // Clear session history
    route("/session") {
        delete("/{id}") {
            try {
                val sessionId = call.parameters["id"]
                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID is required"))
                    return@delete
                }

                val deleted = sessionManager.clearSession(sessionId)
                if (deleted) {
                    call.respond(mapOf("message" to "Session cleared successfully", "sessionId" to sessionId))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to clear session: ${e.message}")
                )
            }
        }

        // Get session stats
        get("/stats") {
            try {
                val stats = sessionManager.getStats()
                call.respond(stats)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to get stats: ${e.message}")
                )
            }
        }
    }

    get("/health") {
        call.respond(mapOf("status" to "ok"))
    }
}
