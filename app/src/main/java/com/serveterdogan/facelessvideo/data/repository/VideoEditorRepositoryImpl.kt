package com.serveterdogan.facelessvideo.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.serveterdogan.facelessvideo.domain.repository.VideoEditorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class VideoEditorRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoEditorRepository {

    override suspend fun mergeAudioAndVideo(videoFile: File, audioFile: File): Result<File> = withContext(Dispatchers.IO) {
        // Eski basit birleştirme metodunu koruyoruz (Gerekirse diye)
        val result = mergeAudioWithMultipleVideos(listOf(videoFile), audioFile, "")
        return@withContext result
    }

    override suspend fun mergeAudioWithMultipleVideos(
        videoFiles: List<File>,
        audioFile: File,
        script: String,
        musicFile: File?,
        subtitleFile: File?
    ): Result<File> = withContext(Dispatchers.IO) {
        
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.cacheDir
        outputDir.listFiles()?.filter { it.name.contains("Final_Video") || it.name.contains("FacelessVideo") }?.forEach { it.delete() }

        if (videoFiles.isEmpty()) return@withContext Result.failure(Exception("Hata: Video dosyaları eksik."))
        
        val outputFile = File(outputDir, "Faceless_Final_${System.currentTimeMillis()}.mp4")

        // 1. SES SÜRESİNİ HESAPLA
        val retriever = MediaMetadataRetriever()
        var durationSeconds = 0.0
        try {
            retriever.setDataSource(audioFile.absolutePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationSeconds = (time?.toDouble() ?: 0.0) / 1000.0
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }

        // 2. FFMPEG ARGÜMANLARINI OLUŞTUR
        val args = mutableListOf<String>()

        // Girdiler
        val maxClips = 5
        val clips = videoFiles.take(maxClips)
        val clipCount = clips.size

        clips.forEach { 
            args.add("-stream_loop")
            args.add("10")
            args.add("-i")
            args.add(it.absolutePath)
        }
        
        // Seslendirme (Indeks: clipCount)
        args.add("-i")
        args.add(audioFile.absolutePath)

        // Müzik (Indeks: clipCount + 1 - Eğer varsa)
        if (musicFile != null && musicFile.exists()) {
            args.add("-stream_loop")
            args.add("-1") // Müziği sonsuz döndür ki video bitene kadar çalsın
            args.add("-i")
            args.add(musicFile.absolutePath)
        }

        // FILTER COMPLEX
        val filterComplex = StringBuilder()
        
        val sceneDuration = durationSeconds / clipCount
        
        for (i in 0 until clipCount) {
            filterComplex.append("[$i:v]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,setsar=1,trim=duration=$sceneDuration[v$i];")
        }
        
        for (i in 0 until clipCount) {
            filterComplex.append("[v$i]")
        }
        filterComplex.append("concat=n=$clipCount:v=1:a=0[out_video];")

        // Ses Miksleme
        if (musicFile != null && musicFile.exists()) {
            val voiceIndex = clipCount
            val musicIndex = clipCount + 1
            // Seslendirme net (1.0), Müzik arka planda (0.08)
            filterComplex.append("[$voiceIndex:a]volume=1.0[voice];")
            filterComplex.append("[$musicIndex:a]volume=0.2[music];")
            filterComplex.append("[voice][music]amix=inputs=2:duration=first[out_audio]")
        } else {
            filterComplex.append("[${clipCount}:a]volume=1.0[out_audio]")
        }

        val finalFilter = filterComplex.toString()

        args.add("-filter_complex")
        args.add(finalFilter)
        
        args.add("-map")
        args.add("[out_video]")
        args.add("-map")
        args.add("[out_audio]")

        args.add("-c:v")
        args.add("mpeg4")
        args.add("-preset")
        args.add("ultrafast")
        args.add("-c:a")
        args.add("aac")
        args.add("-b:a")
        args.add("192k")
        args.add("-t")
        args.add(String.format(java.util.Locale.US, "%.3f", durationSeconds))
        args.add("-y")

        // Altyazı dosyası varsa: önce videoyu geçici dosyaya yaz, sonra Canvas bitmap overlay ile yak
        android.util.Log.d("SubtitleDebug", "[6] subtitleFile null mu: ${subtitleFile == null}, var mı: ${subtitleFile?.exists()}")

        if (subtitleFile != null && subtitleFile.exists()) {
            val tempFile = File(outputDir, "temp_no_subs_${System.currentTimeMillis()}.mp4")
            args.add(tempFile.absolutePath)

            android.util.Log.d("SubtitleDebug", "[7] Adım 1: Video+Ses birleştiriliyor")
            val session1 = FFmpegKit.executeWithArguments(args.toTypedArray())
            if (!ReturnCode.isSuccess(session1.returnCode)) {
                val logs = session1.allLogsAsString.takeLast(500)
                android.util.Log.e("SubtitleDebug", "[7] BAŞARISIZ: $logs")
                return@withContext Result.failure(Exception("Video Montaj Hatası: $logs"))
            }
            android.util.Log.d("SubtitleDebug", "[7] Adım 1 başarılı. Boyut: ${tempFile.length()}")

            // SRT'yi parse et, her satır için Canvas ile PNG oluştur, overlay ile yak
            val entries = parseSrt(subtitleFile)
            android.util.Log.d("SubtitleDebug", "[8] ${entries.size} altyazı satırı parse edildi")

            if (entries.isEmpty()) {
                // Altyazı yoksa doğrudan taşı
                tempFile.renameTo(outputFile)
                return@withContext Result.success(outputFile)
            }

            // Her altyazı için PNG oluştur
            val subDir = File(context.cacheDir, "subtitles_${System.currentTimeMillis()}")
            subDir.mkdirs()
            val pngFiles = entries.mapIndexed { i, entry ->
                val png = File(subDir, "sub_$i.png")
                createSubtitleBitmap(entry.text, png, videoWidth = 1080, videoHeight = 1920)
                png
            }
            android.util.Log.d("SubtitleDebug", "[8] ${pngFiles.size} PNG oluşturuldu")

            // FFmpeg overlay filter zinciri: her PNG için overlay + enable
            val filterComplex = buildOverlayFilterComplex(entries, pngFiles)
            android.util.Log.d("SubtitleDebug", "[8] overlay filter (ilk 300): ${filterComplex.take(300)}")

            val subtitleArgs = mutableListOf("-i", tempFile.absolutePath)
            pngFiles.forEach { png ->
                subtitleArgs.add("-i")
                subtitleArgs.add(png.absolutePath)
            }
            subtitleArgs.addAll(listOf(
                "-filter_complex", filterComplex,
                "-map", "[final]",
                "-map", "0:a",
                "-c:v", "mpeg4",
                "-c:a", "copy",
                "-y",
                outputFile.absolutePath
            ))

            val session2 = FFmpegKit.executeWithArguments(subtitleArgs.toTypedArray())
            tempFile.delete()
            subDir.deleteRecursively()

            if (ReturnCode.isSuccess(session2.returnCode)) {
                android.util.Log.d("SubtitleDebug", "[8] Altyazı overlay başarılı!")
                Result.success(outputFile)
            } else {
                val logs = session2.allLogsAsString.takeLast(800)
                android.util.Log.e("SubtitleDebug", "[8] Overlay BAŞARISIZ:\n$logs")
                Result.failure(Exception("Altyazı Ekleme Hatası: $logs"))
            }
        } else {
            android.util.Log.w("SubtitleDebug", "[6] subtitleFile yok, altyazısız devam")
            args.add(outputFile.absolutePath)
            val session = FFmpegKit.executeWithArguments(args.toTypedArray())
            if (ReturnCode.isSuccess(session.returnCode)) {
                Result.success(outputFile)
            } else {
                val logs = session.allLogsAsString.takeLast(500)
                Result.failure(Exception("Montaj Başarısız: $logs"))
            }
        }
    }

    data class SubEntry(val text: String, val start: Double, val end: Double)

    private fun parseSrt(srtFile: File): List<SubEntry> {
        val entries = mutableListOf<SubEntry>()
        try {
            val lines = srtFile.readLines()
            var i = 0
            val timeRegex = Regex("""(\d{2}):(\d{2}):(\d{2}),(\d{3}) --> (\d{2}):(\d{2}):(\d{2}),(\d{3})""")
            while (i < lines.size) {
                val line = lines.getOrNull(i)?.trim() ?: break
                if (line.isEmpty()) { i++; continue }
                if (line.all { it.isDigit() }) {
                    val timeLine = lines.getOrNull(i + 1)?.trim() ?: break
                    val match = timeRegex.find(timeLine)
                    if (match == null) {
                        i++
                        continue
                    }
                    val (h1, m1, s1, ms1, h2, m2, s2, ms2) = match.destructured
                    val start = h1.toDouble() * 3600 + m1.toDouble() * 60 + s1.toDouble() + ms1.toDouble() / 1000
                    val end = h2.toDouble() * 3600 + m2.toDouble() * 60 + s2.toDouble() + ms2.toDouble() / 1000

                    // Metin satırlarını topla (boş satıra veya rakama kadar)
                    val textLines = mutableListOf<String>()
                    var j = i + 2
                    while (j < lines.size) {
                        val tl = lines.getOrNull(j)?.trim() ?: break
                        if (tl.isEmpty() || tl.all { it.isDigit() }) break
                        textLines.add(tl)
                        j++
                    }
                    if (textLines.isNotEmpty()) {
                        entries.add(SubEntry(textLines.joinToString(" "), start, end))
                    }
                    i = j
                } else {
                    i++
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SubtitleDebug", "SRT parse hatası: ${e.message}")
        }
        return entries
    }

    /**
     * Android Canvas kullanarak altyazı metnini beyaz bold yazı + siyah şeffaf arka plan
     * olarak çizer ve PNG dosyasına kaydeder.
     */
    private fun createSubtitleBitmap(text: String, outputPng: File, videoWidth: Int, videoHeight: Int) {
        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 58f * (videoWidth / 1080f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
        }

        val padding = (24f * videoWidth / 1080f).toInt()
        val maxTextWidth = videoWidth - padding * 4

        // Metni satırlara böl
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (textPaint.measureText(testLine) <= maxTextWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        val lineHeight = textPaint.descent() - textPaint.ascent()
        val totalTextHeight = lineHeight * lines.size
        val bitmapHeight = (totalTextHeight + padding * 2).toInt().coerceAtLeast(1)

        val bitmap = android.graphics.Bitmap.createBitmap(videoWidth, bitmapHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Siyah yarı şeffaf arka plan kutusu
        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
        }
        val cornerRadius = 16f * videoWidth / 1080f
        val bgRect = android.graphics.RectF(
            padding.toFloat(), padding.toFloat() / 2,
            (videoWidth - padding).toFloat(), bitmapHeight.toFloat() - padding.toFloat() / 2
        )
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

        // Metni çiz
        lines.forEachIndexed { idx, line ->
            val y = padding - textPaint.ascent() + idx * lineHeight
            canvas.drawText(line, videoWidth / 2f, y, textPaint)
        }

        outputPng.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
    }

    /**
     * Her altyazı PNG'si için overlay filtresi zinciri oluşturur.
     * PNG'ler ekranın alt %20'sine ortalanmış olarak overlay edilir.
     * enable='between(t,start,end)' ile doğru zamanda gösterilir.
     */
    private fun buildOverlayFilterComplex(entries: List<SubEntry>, pngFiles: List<File>): String {
        // Video genişliği 1080, yüksekliği 1920
        // Altyazı Y pozisyonu: alt %20 bölgesinde
        val filter = StringBuilder()

        var lastOut = "[0:v]"
        entries.forEachIndexed { i, entry ->
            val inputIdx = i + 1 // 0 = video, 1+ = PNG'ler
            val startStr = String.format(java.util.Locale.US, "%.3f", entry.start)
            val endStr = String.format(java.util.Locale.US, "%.3f", entry.end)
            val outLabel = if (i == entries.size - 1) "[final]" else "[v${i + 1}]"

            // PNG'yi videoya overlay et: x ortalı, y alt %20'de
            filter.append("${lastOut}[$inputIdx:v]overlay=x=(W-w)/2:y=H-h-H*0.05:enable='between(t\\,$startStr\\,$endStr)'$outLabel")
            if (i < entries.size - 1) filter.append(";")
            lastOut = outLabel
        }

        return filter.toString()
    }
}

