package com.serveterdogan.facelessvideo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serveterdogan.facelessvideo.domain.model.VoiceProfile
import com.serveterdogan.facelessvideo.domain.model.MusicProfile
import com.serveterdogan.facelessvideo.domain.model.availableMusicProfiles
import com.serveterdogan.facelessvideo.domain.model.mockVoiceProfiles
import com.serveterdogan.facelessvideo.domain.repository.GeminiRepository
import com.serveterdogan.facelessvideo.domain.repository.TTSRepository
import com.serveterdogan.facelessvideo.domain.repository.VideoEditorRepository
import com.serveterdogan.facelessvideo.domain.repository.VideoRepository
import com.serveterdogan.facelessvideo.core.utils.TextProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PromptViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val ttsRepository: TTSRepository,
    private val videoRepository: VideoRepository,
    private val videoEditorRepository: VideoEditorRepository
): ViewModel() {

    private val _topic = MutableStateFlow("")
    val topic: StateFlow<String> = _topic

    private val _availableVoices = MutableStateFlow<List<VoiceProfile>>(mockVoiceProfiles)
    val availableVoices: StateFlow<List<VoiceProfile>> = _availableVoices

    private val _isVoicesLoading = MutableStateFlow(true)
    val isVoicesLoading: StateFlow<Boolean> = _isVoicesLoading

    private val _selectedVoice = MutableStateFlow<VoiceProfile?>(mockVoiceProfiles.first())
    val selectedVoice: StateFlow<VoiceProfile?> = _selectedVoice

    private val _availableMusic = MutableStateFlow<List<MusicProfile>>(availableMusicProfiles)
    val availableMusic: StateFlow<List<MusicProfile>> = _availableMusic

    private val _selectedMusic = MutableStateFlow<MusicProfile>(availableMusicProfiles.first())
    val selectedMusic: StateFlow<MusicProfile> = _selectedMusic

    init {
        fetchVoices()
    }

    private fun fetchVoices() {
        viewModelScope.launch {
            _isVoicesLoading.value = true
            ttsRepository.getAvailableVoices().onSuccess { voices ->
                if (voices.isNotEmpty()) {
                    _availableVoices.value = voices
                    _selectedVoice.value = voices.first()
                } else {
                    // Fallback if empty but success
                    _availableVoices.value = mockVoiceProfiles
                    _selectedVoice.value = mockVoiceProfiles.first()
                }
                _isVoicesLoading.value = false
            }.onFailure { error ->
                _isVoicesLoading.value = false
                // Hata durumunda mock veriye dön ama uyar
                _availableVoices.value = mockVoiceProfiles
                _selectedVoice.value = mockVoiceProfiles.first()
                _statusText.value = "Ses listesi alınamadı (Çevrimdışı Mod): ${error.message}"
            }
        }
    }

    private val _resultText = MutableStateFlow("Henüz bir video üretilmedi.")
    val resultText : StateFlow<String> = _resultText
    
    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText

    private val _audioPath = MutableStateFlow<String?>(null)
    val audioPath: StateFlow<String?> = _audioPath

    private val _isLoading = MutableStateFlow(false)
    val isLoading : StateFlow<Boolean> = _isLoading

    private val _finalVideoPath = MutableStateFlow<String?>(null)
    val finalVideoPath: StateFlow<String?> = _finalVideoPath

    private val _errorSignal = MutableStateFlow<Boolean>(false)
    val errorSignal: StateFlow<Boolean> = _errorSignal

    private val _isDirectScript = MutableStateFlow(false)
    val isDirectScript: StateFlow<Boolean> = _isDirectScript

    private val _videoSearchTopic = MutableStateFlow("")
    val videoSearchTopic: StateFlow<String> = _videoSearchTopic

    private val _selectedVideos = MutableStateFlow<List<File>>(emptyList())
    val selectedVideos: StateFlow<List<File>> = _selectedVideos

    private val _isReadyForSelection = MutableStateFlow(false)
    val isReadyForSelection: StateFlow<Boolean> = _isReadyForSelection

    private val _isGeneratingFinalVideo = MutableStateFlow(false)
    val isGeneratingFinalVideo: StateFlow<Boolean> = _isGeneratingFinalVideo

    // Temporary storage for merge parameters
    private var cachedScript: String = ""
    private var cachedAudioFile: File? = null
    private var cachedMusicFile: File? = null
    private var cachedSubtitleFile: File? = null

    fun updateTopic(newTopic: String) {
        _topic.value = newTopic
        // Auto-fill video search topic in normal mode
        if (!_isDirectScript.value) {
            _videoSearchTopic.value = newTopic
        }
    }

    fun updateVideoSearchTopic(newTopic: String) {
        _videoSearchTopic.value = newTopic
    }

    fun toggleDirectScript(enabled: Boolean) {
        _isDirectScript.value = enabled
    }

    fun selectVoice(voice: VoiceProfile) {
        _selectedVoice.value = voice
    }

    fun selectMusic(music: MusicProfile) {
        _selectedMusic.value = music
    }

    fun resetState() {
        _finalVideoPath.value = null
        _audioPath.value = null
        _errorSignal.value = false
        _statusText.value = ""
        _isLoading.value = false
        _isReadyForSelection.value = false
        _isGeneratingFinalVideo.value = false
        _selectedVideos.value = emptyList()
    }

    fun removeVideo(file: File) {
        _selectedVideos.value = _selectedVideos.value.filter { it.absolutePath != file.absolutePath }
    }

    fun handleGalleryVideo(uri: android.net.Uri) {
        viewModelScope.launch {
            val file = videoRepository.saveUriToTempFile(uri)
            file?.let {
                _selectedVideos.value = _selectedVideos.value + it
            }
        }
    }

    fun prepareResources() {
        val currentTopic = _topic.value
        if (currentTopic.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _isReadyForSelection.value = false
            _errorSignal.value = false

            // 0. Arka Plan Müziği İndir
            _statusText.value = "Arka plan müziği hazırlanıyor..."
            val musicUrl = _selectedMusic.value.url
            val musicResult = videoRepository.fetchAndSaveBackgroundMusic(musicUrl)
            cachedMusicFile = musicResult.getOrNull()

            if (cachedMusicFile == null) {
                _statusText.value = "Uyarı: Arka plan müziği indirilemedi."
            }

            // 1. Senaryo Oluştur
            val scriptResult = if (_isDirectScript.value) {
                Result.success(_topic.value)
            } else {
                _statusText.value = "Yapay zeka senaryoyu yazıyor..."
                geminiRepository.generateScript(
                    "Bana '$currentTopic' hakkında 3 cümlelik, çok etkileyici bir YouTube ve instagram Shorts video metni yaz."
                )
            }

            scriptResult.onSuccess { rawScript ->
                val script = TextProcessor.cleanScript(rawScript)
                cachedScript = script
                _resultText.value = script
                _statusText.value = "Seslendirme ve altyazılar hazırlanıyor..."

                // 2. Seslendirme
                val ttsResult = ttsRepository.generateSpeechWithTimestamps(
                    text = script,
                    voiceId = _selectedVoice.value?.id ?: ""
                )

                ttsResult.onSuccess { (audioFile, subtitleFile) ->
                    cachedAudioFile = audioFile
                    cachedSubtitleFile = subtitleFile
                    _audioPath.value = audioFile.absolutePath

                    _statusText.value = "Stok videolar aranıyor ve indiriliyor..."

                    // 3. Videoları İndir
                    val searchKeyword = _videoSearchTopic.value.ifBlank { currentTopic }.take(50)
                    val videoResult = videoRepository.fetchAndSaveBackgroundVideo(searchKeyword)

                    videoResult.onSuccess { videoFiles ->
                        _selectedVideos.value = videoFiles
                        _isReadyForSelection.value = true
                        _statusText.value = "Videolar hazır, seçim yapabilirsiniz."
                    }.onFailure { vError ->
                        _statusText.value = "Video İndirme Hatası: ${vError.message}"
                        _errorSignal.value = true
                    }

                }.onFailure { error ->
                    _statusText.value = "Seslendirme Hatası: ${error.message}"
                    _errorSignal.value = true
                }
            }.onFailure { error ->
                _statusText.value = "Hata: ${error.message}"
                _errorSignal.value = true
            }

            _isLoading.value = false
        }
    }

    fun generateFinalVideo() {
        if (cachedAudioFile == null || _selectedVideos.value.isEmpty()) {
            _statusText.value = "Hata: Kaynaklar eksik."
            return
        }

        viewModelScope.launch {
            _isGeneratingFinalVideo.value = true
            _isReadyForSelection.value = false // Döngüyü kırmak için seçim modunu kapatıyoruz
            _statusText.value = "Video, müzik ve altyazılar birleştiriliyor (Final Montaj)..."

            val mergeResult = videoEditorRepository.mergeAudioWithMultipleVideos(
                videoFiles = _selectedVideos.value,
                audioFile = cachedAudioFile!!,
                script = cachedScript,
                musicFile = cachedMusicFile,
                subtitleFile = cachedSubtitleFile
            )

            mergeResult.onSuccess { finalFile ->
                _statusText.value = "Video Başarıyla Oluşturuldu!"
                _finalVideoPath.value = finalFile.absolutePath
            }.onFailure { mError ->
                _statusText.value = "Montaj Hatası: ${mError.message}"
                _errorSignal.value = true
            }

            _isGeneratingFinalVideo.value = false
        }
    }
}
