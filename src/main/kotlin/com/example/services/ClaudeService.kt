package com.example.services

import com.example.config.ClaudeConfig
import com.example.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ClaudeService(private val config: ClaudeConfig) {

    private val logger = LoggerFactory.getLogger(ClaudeService::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    suspend fun sendMessage(userMessage: String): String {
        return sendMessageWithHistory(userMessage, emptyList())
    }

    /**
     * Send a message to Claude API with conversation history
     * @param userMessage The current user message
     * @param messageHistory Previous messages in the conversation
     * @return Claude's response text
     */
    suspend fun sendMessageWithHistory(userMessage: String, messageHistory: List<ClaudeMessage>): String {
        return try {
            logger.info("Sending message to Claude API with ${messageHistory.size} history messages")

            // Build messages list: history + new user message
            val messages = messageHistory.toMutableList()
            messages.add(ClaudeMessage(role = "user", content = userMessage))

            val claudeRequest = ClaudeRequest(
                model = config.model,
                maxTokens = config.maxTokens,
                messages = messages,
                temperature = config.temperature
            )

            logger.info("Claude Request: model=${config.model}, maxTokens=${config.maxTokens}, totalMessages=${messages.size}")

            val httpResponse: HttpResponse = client.post(config.apiUrl) {
                header("x-api-key", config.apiKey)
                header("anthropic-version", config.apiVersion)
                contentType(ContentType.Application.Json)
                setBody(claudeRequest)
            }

            logger.info("Claude API response status: ${httpResponse.status}")

            if (!httpResponse.status.isSuccess()) {
                val errorBody = httpResponse.bodyAsText()
                logger.error("Claude API error response: $errorBody")

                return try {
                    val errorResponse: ClaudeError = Json.decodeFromString(errorBody)
                    "Claude API Error: ${errorResponse.error.message}"
                } catch (e: Exception) {
                    logger.error("Failed to parse error response: ${e.message}")
                    "Claude API Error (${httpResponse.status}): $errorBody"
                }
            }

            val response: ClaudeResponse = httpResponse.body()
            val responseText = response.content.firstOrNull()?.text ?: "No response from Claude"

            logger.info("Successfully received response from Claude API (input: ${response.usage.inputTokens}, output: ${response.usage.outputTokens} tokens)")
            responseText

        } catch (e: Exception) {
            logger.error("Exception in ClaudeService: ${e.message}", e)
            "Error: ${e.message}"
        }
    }

    fun close() {
        client.close()
    }
}
