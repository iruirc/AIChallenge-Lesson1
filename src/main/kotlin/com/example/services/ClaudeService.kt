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

    /**
     * Enhances user message with plain text formatting instructions
     */
    private fun enhanceMessageForPlainText(userMessage: String): String {
        return userMessage
    }

    /**
     * Enhances user message with JSON formatting instructions
     */
    private fun enhanceMessageForJson(userMessage: String): String {
        return """$userMessage

Please format your response as valid JSON. Structure the response appropriately based on the content."""
    }

    /**
     * Enhances user message with XML formatting instructions
     */
    private fun enhanceMessageForXml(userMessage: String): String {
        return """$userMessage

Please format your response as valid XML. Use appropriate tags and structure based on the content."""
    }

    /**
     * Enhances user message based on the specified format
     */
    private fun enhanceMessage(userMessage: String, format: ResponseFormat): String {
        return when (format) {
            ResponseFormat.PLAIN_TEXT -> enhanceMessageForPlainText(userMessage)
            ResponseFormat.JSON -> enhanceMessageForJson(userMessage)
            ResponseFormat.XML -> enhanceMessageForXml(userMessage)
        }
    }

    suspend fun sendMessage(userMessage: String, format: ResponseFormat = ResponseFormat.PLAIN_TEXT): String {
        return try {
            logger.info("Sending message to Claude API: $userMessage")
            logger.info("Response format: $format")

            val enhancedMessage = enhanceMessage(userMessage, format)

            val claudeRequest = ClaudeRequest(
                model = config.model,
                maxTokens = config.maxTokens,
                messages = listOf(
                    ClaudeMessage(
                        role = "user",
                        content = enhancedMessage
                    )
                ),
                temperature = config.temperature
            )

            logger.info("Claude Request: model=${config.model}, maxTokens=${config.maxTokens}")

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

            logger.info("Successfully received response from Claude API")
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
