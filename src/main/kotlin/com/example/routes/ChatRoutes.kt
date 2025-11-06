package com.example.routes

import com.example.models.ChatRequest
import com.example.models.ChatResponse
import com.example.models.MessageRole
import com.example.services.ChatSessionManager
import com.example.services.ClaudeService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoutes(claudeService: ClaudeService, sessionManager: ChatSessionManager) {
    route("/chat") {
        post {
            try {
                val request = call.receive<ChatRequest>()

                if (request.message.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Message cannot be empty"))
                    return@post
                }

                // Получаем или создаем сессию
                val (sessionId, session) = sessionManager.getOrCreateSession(request.sessionId)

                // Получаем историю сообщений из сессии
                val messageHistory = session.messages

                // Отправляем сообщение в Claude API с историей
                val claudeResponse = claudeService.sendMessage(
                    userMessage = request.message,
                    format = request.format,
                    messageHistory = messageHistory
                )

                // Сохраняем сообщение пользователя в историю (без форматирования)
                session.addMessage(MessageRole.USER, request.message)

                // Сохраняем ответ ассистента в историю
                session.addMessage(MessageRole.ASSISTANT, claudeResponse)

                // Возвращаем ответ вместе с sessionId
                call.respond(ChatResponse(response = claudeResponse, sessionId = sessionId))

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to process request: ${e.message}")
                )
            }
        }
    }

    get("/health") {
        call.respond(mapOf("status" to "ok"))
    }
}
