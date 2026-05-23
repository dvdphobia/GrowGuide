package com.growguide.app.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.growguide.app.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Client for calling the Ollama Chat API to get AI-powered plant advice.
 * Uses OkHttp for HTTP requests and Gson for JSON parsing.
 *
 * Important: Set OLLAMA_API_KEY in local.properties before building.
 */
object OllamaApiClient {

    private const val API_URL = "https://ollama.com/api/chat"
    private const val MODEL_NAME = "gpt-oss:120b"

    // System prompt instructs the model to act as a plant expert
    private const val SYSTEM_PROMPT_BASE = "You are a helpful plant and agriculture " +
            "expert. Answer questions about growing plants, diagnosing issues, " +
            "and farming advice. Be concise and practical."

    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Sends a user message to Ollama and returns the AI response text.
     *
     * @param userMessage The question/comment from the user
     * @param plantName The name of the plant the user is asking about
     * @param plantType The type/category of the plant
     * @param plantNotes Additional notes the user saved about the plant
     * @param callback Called with the AI reply text on success, or an error message on failure
     */
    fun sendMessage(
        userMessage: String,
        plantName: String = "",
        plantType: String = "",
        plantNotes: String = "",
        callback: (String) -> Unit
    ) {
        val plantContext = buildString {
            append("The user is growing a plant")
            if (plantName.isNotBlank()) append(" named '$plantName'")
            if (plantType.isNotBlank()) append(" of type '$plantType'")
            append(". ")
            if (plantNotes.isNotBlank()) append("User notes: $plantNotes. ")
            append("Tailor your advice specifically to this plant. Keep answers short and actionable.")
        }

        val systemPrompt = "$SYSTEM_PROMPT_BASE $plantContext"

        // Build the request body matching Ollama's expected format
        val requestBody = ChatRequest(
            model = MODEL_NAME,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userMessage)
            ),
            stream = false
        )

        val jsonBody = gson.toJson(requestBody)
        val body = jsonBody.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${BuildConfig.OLLAMA_API_KEY}")
            .build()

        // Execute the API call on a background thread
        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                        // Extract the AI's reply from: response.message.content
                        val reply = chatResponse.message?.content ?: "Sorry, I couldn't process that."
                        callback(reply)
                    } else {
                        callback("API error: ${response.code} - ${response.message}")
                    }
                }
            } catch (e: IOException) {
                callback("Network error: ${e.message}")
            }
        }.start()
    }

    // --- Gson data classes for API request/response serialization ---

    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val stream: Boolean
    )

    private data class Message(
        val role: String,
        val content: String
    )

    private data class ChatResponse(
        @SerializedName("message") val message: MessageContent?
    )

    private data class MessageContent(
        @SerializedName("content") val content: String?
    )
}
