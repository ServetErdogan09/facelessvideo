package com.serveterdogan.facelessvideo.domain.repository

import java.io.File

interface VideoRepository {
    suspend fun fetchAndSaveBackgroundVideo(query: String): Result<List<File>>
    suspend fun fetchAndSaveBackgroundMusic(url: String): Result<File>
    suspend fun saveUriToTempFile(uri: android.net.Uri): File?
}
