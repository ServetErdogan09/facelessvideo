package com.serveterdogan.facelessvideo.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.serveterdogan.facelessvideo.core.utils.StorageUtils
import com.serveterdogan.facelessvideo.presentation.ui.components.GlassmorphismCard
import com.serveterdogan.facelessvideo.ui.theme.*
import java.io.File

@Composable
fun ResultScreen(
    videoPath: String,
    script: String,
    audioPath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Video Player State
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(videoPath)))
            setMediaItem(mediaItem)
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoal)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Text(
                text = "PRODUCTION COMPLETE",
                color = NeonBlue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 40.dp, bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            // VIDEO PREVIEW CARD
            GlassmorphismCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SCRIPT CARD
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Script Text",
                        color = NeonPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = script,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // MAIN ACTIONS (Download & Refresh)
            val downloadBrush = Brush.horizontalGradient(colors = listOf(NeonPurple, NeonBlue))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(downloadBrush)
                    .clickable {
                        val result = StorageUtils.saveVideoToGallery(context, File(videoPath))
                        result.onSuccess {
                            Toast.makeText(context, "Video Galeriye Kaydedildi!", Toast.LENGTH_LONG).show()
                        }.onFailure { error ->
                            Toast.makeText(context, "Kayıt Hatası: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DOWNLOAD TO GALLERY",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECONDARY ACTIONS (Share Video / Share Audio)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Share Video
                SecondaryActionButton(
                    modifier = Modifier.weight(1f),
                    text = "Share Video",
                    onClick = { shareFile(context, File(videoPath), "video/mp4") }
                )
                // Share Audio
                SecondaryActionButton(
                    modifier = Modifier.weight(1f),
                    text = "Share Audio",
                    onClick = { shareFile(context, File(audioPath), "audio/mpeg") }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "← CREATE NEW VIDEO",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateBack() }
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SecondaryActionButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(25.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Share, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun shareFile(context: Context, file: File, type: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = type
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Paylaş")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        // Handle error
    }
}
