package com.growguide.app.network

import com.google.gson.Gson
import com.growguide.app.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

/**
 * Client for the Plant.id API (free tier: 100 requests/day).
 * Uploads a plant photo and returns identification suggestions.
 *
 * Requires PLANT_ID_API_KEY in local.properties.
 */
object PlantIdApiClient {

    private const val BASE_URL = "https://api.plant.id/v3/identification"
    private val client = OkHttpClient()
    private val gson = Gson()

    data class IdentifyResult(
        val name: String,
        val commonName: String,
        val description: String
    )

    fun identifyPlant(photoFile: File, callback: (IdentifyResult?) -> Unit) {
        val apiKey = BuildConfig.PLANT_ID_API_KEY
        if (apiKey.isBlank()) {
            callback(null)
            return
        }

        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("images", photoFile.name, photoFile.asRequestBody(mediaType))
            .addFormDataPart("similar_images", "true")
            .build()

        val request = Request.Builder()
            .url(BASE_URL)
            .header("Api-Key", apiKey)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null)
                    return
                }
                val body = response.body?.string()
                val result = parseResponse(body)
                callback(result)
            }
        })
    }

    private fun parseResponse(json: String?): IdentifyResult? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = gson.fromJson(json, Map::class.java) as Map<*, *>

            // Try v3 structure first: result.classification.suggestions
            val result = root["result"] as? Map<*, *>
            val classification = result?.get("classification") as? Map<*, *>
            val suggestions = classification?.get("suggestions") as? List<*>

            // Fallback to v2 structure: suggestions directly on root
            val finalSuggestions = suggestions
                ?: (root["suggestions"] as? List<*>)
                ?: emptyList()

            val first = finalSuggestions.firstOrNull() as? Map<*, *> ?: return null

            val name = (first["name"] as? String)
                ?: (first["plant_name"] as? String)
                ?: "Unknown plant"

            val probability = first["probability"] as? Double ?: 0.0
            if (probability < 0.3) return null

            val details = first["details"] as? Map<*, *>
            val commonNames = details?.get("common_names") as? List<*>
            val commonName = commonNames?.firstOrNull() as? String ?: ""

            val descriptionMap = details?.get("description") as? Map<*, *>
            val description = descriptionMap?.get("value") as? String ?: ""

            IdentifyResult(name, commonName, description)
        } catch (e: Exception) {
            null
        }
    }
}
