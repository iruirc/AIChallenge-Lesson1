package com.example

import com.example.routes.chatRoutes
import com.example.services.ClaudeService
import com.example.services.SessionManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(claudeService: ClaudeService, sessionManager: SessionManager) {
    routing {
        // Главная страница - перенаправление на index.html
        get("/") {
            call.respondRedirect("/index.html")
        }

        // API роуты для чата
        chatRoutes(claudeService, sessionManager)

        // Статические файлы (HTML, CSS, JS)
        staticResources("/", "static")
    }
}
