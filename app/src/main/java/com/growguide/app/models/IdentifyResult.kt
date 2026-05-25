package com.growguide.app.models

/**
 * Result of a plant photo identification attempt.
 * Used by both PlantNet and Ollama vision APIs.
 */
data class IdentifyResult(
    val name: String,
    val commonName: String,
    val description: String
)
