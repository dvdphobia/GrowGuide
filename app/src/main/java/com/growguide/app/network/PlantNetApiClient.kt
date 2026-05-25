package com.growguide.app.network

import com.google.gson.Gson
import com.growguide.app.BuildConfig
import com.growguide.app.models.IdentifyResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

/**
 * Client for the Pl@ntNet API (free tier).
 * Uploads a plant photo and returns identification suggestions.
 *
 * Requires PLANTNET_API_KEY in local.properties.
 * Get a key at: https://my.plantnet.org/settings/api-key
 */
object PlantNetApiClient {

    private val client = OkHttpClient()
    private val gson = Gson()

    fun identifyPlant(photoFile: File, callback: (IdentifyResult?) -> Unit) {
        val apiKey = BuildConfig.PLANTNET_API_KEY
        if (apiKey.isBlank()) {
            callback(null)
            return
        }

        val url = "https://my-api.plantnet.org/v2/identify/all?api-key=$apiKey"

        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("images", photoFile.name, photoFile.asRequestBody(mediaType))
            .build()

        val request = Request.Builder()
            .url(url)
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
            val results = root["results"] as? List<*> ?: return null
            val first = results.firstOrNull() as? Map<*, *> ?: return null

            val score = first["score"] as? Double ?: 0.0
            if (score < 0.1) return null

            val species = first["species"] as? Map<*, *> ?: return null

            val name = species["scientificNameWithoutAuthor"] as? String
                ?: species["scientificName"] as? String
                ?: "Unknown plant"

            val commonNames = species["commonNames"] as? List<*>
            val commonName = commonNames?.firstOrNull() as? String ?: ""

            val family = species["family"] as? Map<*, *>
            val familyName = family?.get("scientificNameWithoutAuthor") as? String ?: ""

            val description = if (familyName.isNotBlank()) {
                "Family: $familyName"
            } else ""

            IdentifyResult(name, commonName, description)
        } catch (e: Exception) {
            null
        }
    }
}
