package com.serveterdogan.facelessvideo.data.repository

import android.content.Context
import com.serveterdogan.facelessvideo.data.remote.ApiService
import com.serveterdogan.facelessvideo.domain.repository.VideoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : VideoRepository {

    override suspend fun fetchAndSaveBackgroundVideo(query: String): Result<List<File>> {
        return try {
            val keywords = query.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(5)
            val videoFiles = mutableListOf<File>()

            keywords.forEachIndexed { index, keyword ->
                try {
                    val response = apiService.searchVideo(keyword)
                    val video = response.videos.firstOrNull()
                    video?.let {

                        val videoFileLink = it.video_files.find { f -> f.quality == "hd" } ?: it.video_files.firstOrNull()

                        videoFileLink?.let { link ->
                            val bytes = apiService.downloadVideoFile(link.link)
                            val file = File(context.cacheDir, "faceless_clip_$index.mp4")
                            withContext(Dispatchers.IO) {
                                file.writeBytes(bytes)
                            }
                            videoFiles.add(file)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoRepo", "Error fetching video for $keyword: ${e.message}")
                }
            }

            if (videoFiles.isEmpty()) {
                return Result.failure(Exception("Pexels'te uygun video bulunamadı."))
            }

            Result.success(videoFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAndSaveBackgroundMusic(url: String): Result<File> {
        return try {
            val bytes = apiService.downloadVideoFile(url) // Reuse same download logic
            val file = File(context.cacheDir, "background_music.mp3")
            withContext(Dispatchers.IO) {
                file.writeBytes(bytes)
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUriToTempFile(uri: android.net.Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val file = File(context.cacheDir, "gallery_video_${System.currentTimeMillis()}.mp4")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
