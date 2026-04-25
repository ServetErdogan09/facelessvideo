package com.serveterdogan.facelessvideo.domain.model

data class VoiceProfile(
    val id: String,
    val name: String,
    val description: String,
    val isMale: Boolean
)

val mockVoiceProfiles = listOf(
    VoiceProfile("EXAVITQu4vr4xnSDxMaL", "Bella (Sakin - Kadın)", "Yumuşak ve rahatlatıcı anlatım", isMale = false), // çalışıyor
    VoiceProfile("ErXwobaYiN019PkySvjV", "Antoni (Profesyonel - Erkek)", "Kaliteli ve güven veren hikaye anlatımı", isMale = true), // çalışıyor
    VoiceProfile("21m00Tcm4TlvDq8ikWAM", "Rachel (Neşeli - Kadın)", "Enerjik ve motive edici ses", isMale = false),
    VoiceProfile("pNInz6obpgDQGcFmaJgB", "Adam (Derin - Erkek)", "Belgesel ve ciddi konulara uygun ses", isMale = true), // çalışıyor
    VoiceProfile("jBpfuIE2acL6zB5BBB3C", "Gigi (Heyecanlı - Kadın)", "Hızlı ve merak uyandırıcı ses", isMale = false)
)

