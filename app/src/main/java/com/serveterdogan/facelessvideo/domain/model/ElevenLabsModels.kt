package com.serveterdogan.facelessvideo.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ElevenLabsRequest(
    val text: String,
    val model_id: String = "eleven_multilingual_v2",
    val voice_settings: VoiceSettings = VoiceSettings()
)

@Serializable
data class VoiceSettings(
    val stability: Double = 0.6,
    val similarity_boost: Double = 0.8,
    val style: Double = 0.4,
    val use_speaker_boost: Boolean = true
)

@Serializable
data class VoicesResponse(
    val voices: List<Voice>
)

@Serializable
data class Voice(
    val voice_id: String,
    val name: String,
    val category: String? = null,
    val description: String? = null,
    val labels: Map<String, String>? = null,
    val preview_url: String? = null
)

// --- WITH-TIMESTAMPS API RESPONSE MODELS ---

@Serializable
data class ElevenLabsTTSWithTimestampsResponse(
    val audio_base64: String,
    val alignment: WordAlignment? = null,
    val normalized_alignment: WordAlignment? = null
)

@Serializable
data class WordAlignment(
    val characters: List<String>,
    val character_start_times_seconds: List<Double>,
    val character_end_times_seconds: List<Double>
)
