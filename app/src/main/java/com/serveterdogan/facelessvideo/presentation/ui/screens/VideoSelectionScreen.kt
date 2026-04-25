package com.serveterdogan.facelessvideo.presentation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serveterdogan.facelessvideo.presentation.viewmodel.PromptViewModel
import com.serveterdogan.facelessvideo.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSelectionScreen(
    viewModel: PromptViewModel,
    onNavigateToLoading: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val selectedVideos by viewModel.selectedVideos.collectAsState()
    val isGenerating by viewModel.isGeneratingFinalVideo.collectAsState()
    val finalVideoPath by viewModel.finalVideoPath.collectAsState()

    var previewVideoFile by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.handleGalleryVideo(it) }
    }

    // Navigate to loading when generation starts
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            onNavigateToLoading()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Videoları Seç", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCharcoal)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                val gradientBrush = Brush.horizontalGradient(colors = listOf(NeonPurple, NeonBlue))
                Button(
                    onClick = { viewModel.generateFinalVideo() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradientBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("VİDEOYU OLUŞTUR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = DarkCharcoal
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "İndirilen ve Seçilen Videolar (${selectedVideos.size})",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(selectedVideos) { videoFile ->
                        VideoItem(
                            file = videoFile,
                            onDelete = { viewModel.removeVideo(videoFile) },
                            onPreview = { previewVideoFile = videoFile }
                        )
                    }

                    item {
                        AddVideoButton(onClick = {
                            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        })
                    }
                }
            }

            // Video Preview Dialog
            previewVideoFile?.let { file ->
                VideoPlayerDialog(
                    file = file,
                    onDismiss = { previewVideoFile = null }
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerDialog(file: File, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(file.absolutePath)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(9f/16f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
            }
        }
    }
}

@Composable
fun VideoItem(file: File, onDelete: () -> Unit, onPreview: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPreview() },
        colors = CardDefaults.cardColors(containerColor = MidnightBlue)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }

            Text(
                text = file.name,
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color.Red.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AddVideoButton(onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NeonBlue))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Galeriden Ekle", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
