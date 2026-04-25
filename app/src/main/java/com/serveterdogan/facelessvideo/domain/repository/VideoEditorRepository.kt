package com.serveterdogan.facelessvideo.domain.repository

import java.io.File

interface VideoEditorRepository {
    suspend fun mergeAudioAndVideo(videoFile: File, audioFile: File): Result<File>
    suspend fun mergeAudioWithMultipleVideos(videoFiles: List<File>, audioFile: File, script: String, musicFile: File? = null, subtitleFile: File? = null): Result<File>
}
