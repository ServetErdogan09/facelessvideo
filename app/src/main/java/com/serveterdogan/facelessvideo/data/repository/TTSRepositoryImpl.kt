package com.serveterdogan.facelessvideo.data.repository

import android.content.Context
import android.util.Base64
import com.serveterdogan.facelessvideo.data.remote.ApiService
import com.serveterdogan.facelessvideo.domain.model.ElevenLabsRequest
import com.serveterdogan.facelessvideo.domain.repository.TTSRepository
import com.serveterdogan.facelessvideo.domain.model.VoiceProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class TTSRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : TTSRepository {

    override suspend fun generateSpeech(text: String, voiceId: String): Result<File> {
        return try {
            val request = ElevenLabsRequest(text = text)
            val audioBytes = apiService.generateTTS(voiceId, request)
            val file = File(context.cacheDir, "faceless_audio.mp3")
            withContext(Dispatchers.IO) {
                file.writeBytes(audioBytes)
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateSpeechWithTimestamps(text: String, voiceId: String): Result<Pair<File, File?>> {
        return try {
            android.util.Log.d("SubtitleDebug", "[1] API çağrısı başlıyor. voiceId=$voiceId, textLen=${text.length}")
            val request = ElevenLabsRequest(text = text)
            val response = apiService.generateTTSWithTimestamps(voiceId, request)

            android.util.Log.d("SubtitleDebug", "[2] API cevabı alındı. audio_base64 boş mu: ${response.audio_base64.isBlank()}")
            android.util.Log.d("SubtitleDebug", "[2] normalized_alignment null mu: ${response.normalized_alignment == null}, alignment null mu: ${response.alignment == null}")

            // 1. Base64 kodlu sesi çöz ve kaydet
            val audioBytes = Base64.decode(response.audio_base64, Base64.DEFAULT)
            val audioFile = File(context.cacheDir, "faceless_audio.mp3")
            withContext(Dispatchers.IO) {
                audioFile.writeBytes(audioBytes)
            }
            android.util.Log.d("SubtitleDebug", "[3] Ses dosyası kaydedildi: ${audioFile.absolutePath}, boyut: ${audioFile.length()} bytes")

            // 2. Karakter zamanlamalarını kelime gruplarına dönüştür ve SRT oluştur
            val alignment = response.normalized_alignment ?: response.alignment
            android.util.Log.d("SubtitleDebug", "[4] Kullanılan alignment null mu: ${alignment == null}")
            if (alignment != null) {
                android.util.Log.d("SubtitleDebug", "[4] Karakter sayısı: ${alignment.characters.size}")
                android.util.Log.d("SubtitleDebug", "[4] İlk 10 karakter: ${alignment.characters.take(10)}")
            }

            val srtFile: File? = if (alignment != null && alignment.characters.isNotEmpty()) {
                val srtContent = buildSrtFromAlignment(alignment.characters, alignment.character_start_times_seconds, alignment.character_end_times_seconds)
                val srt = File(context.cacheDir, "faceless_subtitles.srt")
                withContext(Dispatchers.IO) {
                    srt.writeText(srtContent)
                }
                android.util.Log.d("SubtitleDebug", "[5] SRT dosyası oluşturuldu: ${srt.absolutePath}")
                android.util.Log.d("SubtitleDebug", "[5] SRT içeriği (ilk 300 karakter):\n${srtContent.take(300)}")
                srt
            } else {
                android.util.Log.w("SubtitleDebug", "[5] UYARI: Alignment verisi null veya boş, SRT oluşturulmadı!")
                null
            }

            Result.success(Pair(audioFile, srtFile))
        } catch (e: Exception) {
            android.util.Log.e("SubtitleDebug", "[HATA] generateSpeechWithTimestamps exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Karakter-bazlı zamanlamaları, kelime gruplarına (3-4 kelime) ayırarak SRT formatına dönüştürür.
     * SRT formatı:
     *   1
     *   00:00:01,000 --> 00:00:03,500
     *   Kelime grupları burada
     */
    private fun buildSrtFromAlignment(
        characters: List<String>,
        startTimes: List<Double>,
        endTimes: List<Double>
    ): String {
        // Karakterleri kelimelere birleştir
        data class WordEntry(val word: String, val start: Double, val end: Double)
        val words = mutableListOf<WordEntry>()

        var currentWord = StringBuilder()
        var wordStart = 0.0
        var wordEnd = 0.0

        for (i in characters.indices) {
            val ch = characters[i]
            if (ch == " " || ch == "\n") {
                if (currentWord.isNotEmpty()) {
                    words.add(WordEntry(currentWord.toString(), wordStart, wordEnd))
                    currentWord = StringBuilder()
                }
            } else {
                if (currentWord.isEmpty()) {
                    wordStart = startTimes[i]
                }
                currentWord.append(ch)
                wordEnd = endTimes[i]
            }
        }
        if (currentWord.isNotEmpty()) {
            words.add(WordEntry(currentWord.toString(), wordStart, wordEnd))
        }

        // Her 4 kelimede bir altyazı satırı oluştur
        val srt = StringBuilder()
        var index = 1
        val groupSize = 4

        var i = 0
        while (i < words.size) {
            val group = words.subList(i, minOf(i + groupSize, words.size))
            val groupText = group.joinToString(" ") { it.word }
            val groupStart = group.first().start
            val groupEnd = group.last().end

            srt.append(index)
            srt.append("\n")
            srt.append("${formatSrtTime(groupStart)} --> ${formatSrtTime(groupEnd)}")
            srt.append("\n")
            srt.append(groupText)
            srt.append("\n\n")

            index++
            i += groupSize
        }

        return srt.toString()
    }

    private fun formatSrtTime(seconds: Double): String {
        val totalMs = (seconds * 1000).toLong()
        val h = totalMs / 3600000
        val m = (totalMs % 3600000) / 60000
        val s = (totalMs % 60000) / 1000
        val ms = totalMs % 1000
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ms)
    }

    override suspend fun getAvailableVoices(): Result<List<VoiceProfile>> {
        return try {
            val response = apiService.getVoices()
            val profiles = response.voices.map { voice ->
                VoiceProfile(
                    id = voice.voice_id,
                    name = voice.name,
                    description = voice.description ?: voice.category ?: "ElevenLabs Voice",
                    isMale = voice.labels?.get("gender")?.equals("male", ignoreCase = true) ?: 
                             voice.labels?.get("accent")?.contains("male", ignoreCase = true) ?: false
                )
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
