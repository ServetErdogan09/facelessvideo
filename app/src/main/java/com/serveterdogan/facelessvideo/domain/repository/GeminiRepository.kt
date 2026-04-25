package com.serveterdogan.facelessvideo.domain.repository

interface GeminiRepository {
    suspend fun generateScript(prompt: String): Result<String>
}
