package com.serveterdogan.facelessvideo.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PexelsResponse(
    val page: Int,
    val per_page: Int,
    val total_results: Int,
    val videos: List<PexelsVideo>
)

@Serializable
data class PexelsVideo(
    val id: Int,
    val width: Int,
    val height: Int,
    val duration: Int,
    val video_files: List<PexelsVideoFile>
)

@Serializable
data class PexelsVideoFile(
    val id: Int,
    val quality: String?, // "hd", "sd" or null
    val file_type: String, // "video/mp4"
    val width: Int,
    val height: Int,
    val link: String
)
