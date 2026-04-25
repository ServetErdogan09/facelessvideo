package com.serveterdogan.facelessvideo.domain.repository

import com.serveterdogan.facelessvideo.domain.model.VoiceProfile
import java.io.File

interface TTSRepository {
    suspend fun generateSpeech(text: String, voiceId: String = "21m00Tcm4TlvDq8ikWAM"): Result<File>
    suspend fun generateSpeechWithTimestamps(text: String, voiceId: String): Result<Pair<File, File?>>
    suspend fun getAvailableVoices(): Result<List<VoiceProfile>>
}
