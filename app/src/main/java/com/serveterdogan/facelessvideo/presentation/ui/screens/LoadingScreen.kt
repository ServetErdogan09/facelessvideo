package com.serveterdogan.facelessvideo.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serveterdogan.facelessvideo.presentation.ui.components.GlassmorphismCard
import com.serveterdogan.facelessvideo.presentation.viewmodel.PromptViewModel
import com.serveterdogan.facelessvideo.ui.theme.*

@Composable
fun LoadingScreen(
    viewModel: PromptViewModel,
    onNavigateToResult: (String, String, String) -> Unit,
    onNavigateBackError: () -> Unit,
    onNavigateToSelection: () -> Unit
) {
    val statusText by viewModel.statusText.collectAsState()
    val isReadyForSelection by viewModel.isReadyForSelection.collectAsState()
    val finalVideoPath by viewModel.finalVideoPath.collectAsState()
    val audioPath by viewModel.audioPath.collectAsState()
    val resultText by viewModel.resultText.collectAsState()
    val errorSignal by viewModel.errorSignal.collectAsState()

    val steps = listOf(
        "Arka plan müziği hazırlanıyor...",
        "Yapay zeka senaryoyu yazıyor...",
        "Senaryo seslendiriliyor ve altyazılar hazırlanıyor...",
        "Stok videolar aranıyor ve indiriliyor...",
        "Video, müzik ve altyazılar birleştiriliyor (Final Montaj)..."
    )

    LaunchedEffect(finalVideoPath) {
        if (finalVideoPath != null) {
            onNavigateToResult(finalVideoPath!!, resultText, audioPath ?: "")
        }
    }

    LaunchedEffect(isReadyForSelection) {
        if (isReadyForSelection) {
            onNavigateToSelection()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoal),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            if (errorSignal) {
                // ERROR UI
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "BİR HATA OLUŞTU",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = statusText,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                Button(
                    onClick = { onNavigateBackError() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Geri Dön ve Tekrar Dene")
                }
            } else {
                // LOADING UI
                Text(
                    text = "AI PRODUCING...",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        steps.forEach { step ->
                            val isDone = isStepCompleted(currentStatus = statusText, stepText = step, allSteps = steps)
                            val isCurrent = statusText == step
                            
                            LoadingStepItem(
                                text = step,
                                isCompleted = isDone,
                                isActive = isCurrent
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                /*
                CircularProgressIndicator(
                    color = NeonBlue,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )

                 */
            }
        }
    }
}

@Composable
fun LoadingStepItem(text: String, isCompleted: Boolean, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonBlue,
                modifier = Modifier.size(24.dp)
            )
        } else if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = NeonPurple
            )
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            color = if (isCompleted) TextPrimary else if (isActive) NeonPurple else Color.Gray,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun isStepCompleted(currentStatus: String, stepText: String, allSteps: List<String>): Boolean {
    val currentIndex = allSteps.indexOf(currentStatus)
    val stepIndex = allSteps.indexOf(stepText)
    if (currentStatus == "Video Başarıyla Oluşturuldu!" || currentStatus == "Videolar hazır, seçim yapabilirsiniz.") {
        val totalSteps = allSteps.size
        // If we are in selection stage, first 4 steps are done
        if (currentStatus.contains("seçim")) return stepIndex < 4
        return true
    }
    return if (currentIndex == -1) false else stepIndex < currentIndex
}
