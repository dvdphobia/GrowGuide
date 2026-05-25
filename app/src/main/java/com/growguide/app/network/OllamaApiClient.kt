package com.growguide.app.network

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.growguide.app.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

/**
 * Client for calling the Ollama Chat API to get AI-powered plant advice.
 * Uses OkHttp for HTTP requests and Gson for JSON parsing.
 *
 * Also supports vision model identification of plant photos.
 *
 * Important: Set OLLAMA_API_KEY in local.properties before building.
 */
object OllamaApiClient {

    private const val API_URL = "https://ollama.com/api/chat"
    private const val MODEL_NAME = "gpt-oss:120b"
    private const val VISION_MODEL = "gemma4:31b-cloud"

    // System prompt instructs the model to act as a plant expert
    private const val SYSTEM_PROMPT_BASE = "You are a helpful plant and agriculture " +
            "expert. Answer questions about growing plants, diagnosing issues, " +
            "and farming advice. Be concise and practical."

    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class IdentifyResult(
        val name: String,
        val commonName: String,
        val description: String
    )

    /**
     * Sends a user message to Ollama and returns the AI response text.
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

        val requestBody = ChatRequest(
            model = MODEL_NAME,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userMessage)
            ),
            stream = false
        )

        executeRequest(requestBody, callback)
    }

    /**
     * Identifies a plant from a photo using an Ollama vision model.
     * Returns structured name, common name, and description.
     */
    fun identifyPlant(photoFile: File, callback: (IdentifyResult?) -> Unit) {
        val apiKey = BuildConfig.OLLAMA_API_KEY
        if (apiKey.isBlank()) {
            callback(null)
            return
        }

        val base64Image = try {
            val bytes = photoFile.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            callback(null)
            return
        }

        val prompt = "Identify the plant in this image. " +
                "Return ONLY three lines in this exact format:\n" +
                "Name: <scientific name>\n" +
                "Common Name: <common name or 'Unknown'>\n" +
                "Description: <one sentence care description>"

        val requestBody = ChatRequest(
            model = VISION_MODEL,
            messages = listOf(
                Message(
                    role = "user",
                    content = prompt,
                    images = listOf(base64Image)
                )
            ),
            stream = false
        )

        val jsonBody = gson.toJson(requestBody)
        val body = jsonBody.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                        val content = chatResponse.message?.content ?: ""
                        callback(parseIdentifyResult(content))
                    } else {
                        callback(null)
                    }
                }
            } catch (e: IOException) {
                callback(null)
            }
        }.start()
    }

    private fun parseIdentifyResult(content: String): IdentifyResult? {
        val nameRegex = Regex("Name:\\s*(.+)", RegexOption.IGNORE_CASE)
        val commonRegex = Regex("Common Name:\\s*(.+)", RegexOption.IGNORE_CASE)
        val descRegex = Regex("Description:\\s*(.+)", RegexOption.IGNORE_CASE)

        val name = nameRegex.find(content)?.groupValues?.get(1)?.trim() ?: return null
        val commonName = commonRegex.find(content)?.groupValues?.get(1)?.trim() ?: ""
        val description = descRegex.find(content)?.groupValues?.get(1)?.trim() ?: ""

        if (name.isBlank() || name.equals("unknown", true)) return null
        return IdentifyResult(name, commonName, description)
    }

    private fun executeRequest(requestBody: ChatRequest, callback: (String) -> Unit) {
        val jsonBody = gson.toJson(requestBody)
        val body = jsonBody.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${BuildConfig.OLLAMA_API_KEY}")
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
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

    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val stream: Boolean
    )

    private data class Message(
        val role: String,
        val content: String,
        val images: List<String>? = null
    )

    private data class ChatResponse(
        @SerializedName("message") val message: MessageContent?
    )

    private data class MessageContent(
        @SerializedName("content") val content: String?
    )
}
