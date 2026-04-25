package com.serveterdogan.facelessvideo.core.utils

object TextProcessor {
    /**
     * Gemini bazen metni ``` metin ``` şeklinde gönderir. 
     * Bu fonksiyon bu işaretleri temizler.
     */
    fun cleanScript(rawScript: String): String {
        return rawScript
            .replace("```", "")
            .replace("json", "")
            .replace("*" , "  ") // yıldızları boşlukla değiştir(sil)
            .trim()

    }
}