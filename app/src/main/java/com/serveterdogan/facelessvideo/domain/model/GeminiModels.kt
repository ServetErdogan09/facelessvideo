package com.serveterdogan.facelessvideo.domain.model

import kotlinx.serialization.Serializable


// gelen listenin içinde ses görseler de olabilir o yüzden biz sadece metini almak için böyle yaptık
@Serializable
data class GeminiRequest(
    val contents : List<Content>
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

// gelen cevap
@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

