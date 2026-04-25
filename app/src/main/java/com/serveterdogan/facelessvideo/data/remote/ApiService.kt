package com.serveterdogan.facelessvideo.data.remote

import com.serveterdogan.facelessvideo.BuildConfig
import com.serveterdogan.facelessvideo.domain.model.ElevenLabsRequest
import com.serveterdogan.facelessvideo.domain.model.ElevenLabsTTSWithTimestampsResponse
import com.serveterdogan.facelessvideo.domain.model.GeminiRequest
import com.serveterdogan.facelessvideo.domain.model.GeminiResponse
import com.serveterdogan.facelessvideo.domain.model.PexelsResponse
import com.serveterdogan.facelessvideo.domain.model.VoicesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiService @Inject constructor(
    private val client: HttpClient
) {
    suspend fun generateScript(requestBody: GeminiRequest): GeminiResponse {
        val response: HttpResponse = client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent") {
            parameter("key",BuildConfig.GEMINI_API_KEY)
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        if (response.status != HttpStatusCode.OK) {
            val error = response.bodyAsText()
            throw Exception("Gemini API Hatası (${response.status}): $error")
        }
        return response.body()
    }

    suspend fun generateTTS(voiceId: String, requestBody: ElevenLabsRequest): ByteArray {
        val response: HttpResponse = client.post("https://api.elevenlabs.io/v1/text-to-speech/$voiceId") {
            header("xi-api-key", BuildConfig.ELEVENLABS_API_KEY)
            contentType(ContentType.Application.Json)
            header("Accept", "audio/mpeg")
            setBody(requestBody)
        }

        if (response.status != HttpStatusCode.OK) {
            val error = response.bodyAsText()
            // Eğer ElevenLabs hata verirse (kredi bitmesi vb.) burada yakalıyoruz
            throw Exception("ElevenLabs Hatası (${response.status}): $error")
        }
        
        return response.body()
    }

    suspend fun generateTTSWithTimestamps(voiceId: String, requestBody: ElevenLabsRequest): ElevenLabsTTSWithTimestampsResponse {
        val response: HttpResponse = client.post("https://api.elevenlabs.io/v1/text-to-speech/$voiceId/with-timestamps") {
            header("xi-api-key", BuildConfig.ELEVENLABS_API_KEY)
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            setBody(requestBody)
        }

        if (response.status != HttpStatusCode.OK) {
            val error = response.bodyAsText()
            throw Exception("ElevenLabs Timestamps Hatası (${response.status}): $error")
        }

        return response.body()
    }

    suspend fun getVoices(): VoicesResponse {
        val response: HttpResponse = client.get("https://api.elevenlabs.io/v1/voices") {
            header("xi-api-key", BuildConfig.ELEVENLABS_API_KEY)
            header("Accept", "application/json")
        }

        if (response.status != HttpStatusCode.OK) {
            val error = response.bodyAsText()
            throw Exception("ElevenLabs Ses Listesi Hatası (${response.status}): $error")
        }
        return response.body()
    }
    
    suspend fun searchVideo(query: String): PexelsResponse {
        val response: HttpResponse = client.get("https://api.pexels.com/videos/search") {
            header("Authorization", BuildConfig.PEXELS_API_KEY)
            parameter("query", query)
            parameter("orientation", "portrait")
            parameter("size", "medium")
            parameter("per_page", 3)
        }

        if (response.status != HttpStatusCode.OK) {
            val error = response.bodyAsText()
            throw Exception("Pexels API Hatası (${response.status}): $error")
        }
        return response.body()
    }

    suspend fun downloadVideoFile(url: String): ByteArray {
        val response: HttpResponse = client.get(url)
        if (response.status != HttpStatusCode.OK) {
            throw Exception("Video İndirme Hatası: Sunucu ${response.status} kodu döndürdü.")
        }
        return response.body()
    }
}
