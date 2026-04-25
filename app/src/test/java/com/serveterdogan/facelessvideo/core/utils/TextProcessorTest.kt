package com.serveterdogan.facelessvideo.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TextProcessorTest {

    @Test
    fun `script icindeki markdown isaretleri temizlenmelidir`() {
        // GIVEN (Elimizde ne var?)
        val kirliMetin = "```json Bu bir video senaryosudur. ```"

        // WHEN (Ne yapıyoruz?)
        val temizMetin = TextProcessor.cleanScript(kirliMetin)

        // THEN (Sonuç ne olmalı?)
        val beklenenMetin = "Bu bir video senaryosudur."

        assertEquals(beklenenMetin, temizMetin)
    }

    @Test
    fun `bosluklu metin verildiginde trimlenmelidir`() {
        val metin = "    Merhaba dünya    "
        val sonuc = TextProcessor.cleanScript(metin)

        assertEquals("Merhaba dünya", sonuc)
    }


    @Test
    fun `yildiz isaretleri temizlenmelidir`(){
        val metin = "**Önemli**Başlık**"
        val sonuc = TextProcessor.cleanScript(metin)
        val beklenenMetin = "  Önemli  Başlık  "

        assertEquals(beklenenMetin , sonuc)

    }


    @Test
    fun `metin icindeki tum yildizlar silinince kelimeler birlesiyor mu`() {
        val metin = "**Önemli**Başlık**"
        val sonuc = TextProcessor.cleanScript(metin)

        // Bakalım gerçekten birleşecek mi?
        assertEquals("ÖnemliBaşlık", sonuc)
    }

}
