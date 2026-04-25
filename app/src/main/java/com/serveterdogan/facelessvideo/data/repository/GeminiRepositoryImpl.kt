package com.serveterdogan.facelessvideo.data.repository

import com.serveterdogan.facelessvideo.data.remote.ApiService
import com.serveterdogan.facelessvideo.domain.model.Content
import com.serveterdogan.facelessvideo.domain.model.GeminiRequest
import com.serveterdogan.facelessvideo.domain.model.Part
import com.serveterdogan.facelessvideo.domain.repository.GeminiRepository
import javax.inject.Inject

class GeminiRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : GeminiRepository {

    override suspend fun generateScript(prompt: String): Result<String> {
        return try {
            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )

            val response = apiService.generateScript(requestBody)
            val script = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (script != null) {
                Result.success(script)
            } else {
                Result.failure(Exception("Senaryo oluşturulamadı: Gemini boş yanıt döndü."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
