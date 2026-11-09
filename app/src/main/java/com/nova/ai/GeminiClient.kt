package com.nova.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    // Masukkan API Key Google AI Studio Anda di sini
    private const val API_KEY = "YOUR_GEMINI_API_KEY"
    private const val URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(URL).post(body).build()

            client.newCall(request).execute().use { response ->
                val responseData = response.body?.string() ?: return@withContext "Tidak ada respon dari server."
                val jsonResponse = JSONObject(responseData)
                
                if (jsonResponse.has("candidates")) {
                    val text = jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    return@withContext text
                } else {
                    return@withContext "Error: Gagal memproses permintaan."
                }
            }
        } catch (e: Exception) {
            return@withContext "Terjadi kesalahan: ${e.localizedMessage}"
        }
    }
}
