package com.serveterdogan.facelessvideo.domain.model

data class MusicProfile(
    val id: String,
    val name: String,
    val description: String,
    val url: String
)

val availableMusicProfiles = listOf(
    MusicProfile(
        id = "epic",
        name = "Destansı Motivasyon",
        description = "Güçlü orkestra ve derin baslar.",
        url = "https://incompetech.com/music/royalty-free/mp3-royaltyfree/The%20Descent.mp3"
    ),
    MusicProfile(
        id = "lofi",
        name = "Sakin Lofi",
        description = "Derin düşünceler için chill-out piyano.",
        url = "https://incompetech.com/music/royalty-free/mp3-royaltyfree/Lobby%20Time.mp3"
    ),
    MusicProfile(
        id = "cyberpunk",
        name = "Ritmik Teknoloji",
        description = "Gelecek odaklı, enerjik ve sentetik.",
        url = "https://incompetech.com/music/royalty-free/mp3-royaltyfree/Future%20Gladiator.mp3"
    ),
    MusicProfile(
        id = "deep",
        name = "Derin Dram",
        description = "Etkileyici ve sarsıcı bir sinematik hava.",
        url = "https://incompetech.com/music/royalty-free/mp3-royaltyfree/Deep%20Haze.mp3"
    )
)
