package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val message: String,
    val sessionId: String? = null
)
