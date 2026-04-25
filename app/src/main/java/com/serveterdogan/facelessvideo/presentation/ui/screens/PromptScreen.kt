package com.serveterdogan.facelessvideo.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serveterdogan.facelessvideo.domain.model.VoiceProfile
import com.serveterdogan.facelessvideo.domain.model.mockVoiceProfiles
import com.serveterdogan.facelessvideo.presentation.ui.components.GlassmorphismCard
import com.serveterdogan.facelessvideo.presentation.viewmodel.PromptViewModel
import com.serveterdogan.facelessvideo.ui.theme.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptScreen(
    viewModel: PromptViewModel = hiltViewModel(),
    onNavigateToLoading: () -> Unit
) {
    val topic by viewModel.topic.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val resultText by viewModel.resultText.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDirectScript by viewModel.isDirectScript.collectAsState()
    val videoSearchTopic by viewModel.videoSearchTopic.collectAsState()

    var expandedMusicMenu by remember { mutableStateOf(false) }
    val availableMusic by viewModel.availableMusic.collectAsState()
    val selectedMusic by viewModel.selectedMusic.collectAsState()

    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current

    var expandedMenu by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(DarkCharcoal)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Text(
                text = "FLUX VIDEO AI",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )

            // DIRECT SCRIPT TOGGLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Direkt Metin Modu",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = isDirectScript,
                    onCheckedChange = { viewModel.toggleDirectScript(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonPurple,
                        checkedTrackColor = NeonPurple.copy(alpha = 0.5f)
                    )
                )
            }

            // TOPIC INPUT
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = if (isDirectScript) "Tam Senaryoyu Girin" else "Konu veya Komut",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { viewModel.updateTopic(it) },
                        placeholder = { 
                            Text(
                                if (isDirectScript) "Buraya videoda okunacak tüm metni yazın..." 
                                else "Bir siberpunk şehrinde yolculuk...", 
                                color = Color.Gray
                            ) 
                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = NeonPurple,
                            unfocusedIndicatorColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // VIDEO SEARCH KEYWORD (Only in Direct Mode)
            if (isDirectScript) {
                Spacer(modifier = Modifier.height(16.dp))
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Video Arama Konusu (Anahtar Kelime)",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = videoSearchTopic,
                            onValueChange = { viewModel.updateVideoSearchTopic(it) },
                            placeholder = { Text("Örn: cyberpunk, nature, technology", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = NeonBlue,
                                unfocusedIndicatorColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // VOICE SELECTION
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Select Voice",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedMenu,
                        onExpandedChange = { expandedMenu = !expandedMenu }
                    ) {
                        OutlinedTextField(
                            value = selectedVoice?.name ?: "Sesler yükleniyor...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = NeonBlue,
                                unfocusedIndicatorColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = MidnightBlue,
                                unfocusedContainerColor = MidnightBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.background(MidnightBlue)
                        ) {
                            if (availableVoices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Sesler yükleniyor...", color = TextSecondary) },
                                    onClick = { expandedMenu = false }
                                )
                            }
                            availableVoices.forEach { voice ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(voice.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            Text(voice.description, color = TextSecondary, fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectVoice(voice)
                                        expandedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))



            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Select Background Music",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedMusicMenu,
                        onExpandedChange = { expandedMusicMenu = !expandedMusicMenu }
                    ) {
                        OutlinedTextField(
                            value = selectedMusic.name,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMusicMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = NeonPurple,
                                unfocusedIndicatorColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = MidnightBlue,
                                unfocusedContainerColor = MidnightBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMusicMenu,
                            onDismissRequest = { expandedMusicMenu = false },
                            modifier = Modifier.background(MidnightBlue)
                        ) {
                            availableMusic.forEach { music ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(music.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            Text(music.description, color = TextSecondary, fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectMusic(music)
                                        expandedMusicMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // GENERATE BUTTON
            val gradientBrush = Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd))
            val disabledBrush = SolidColor(Color.DarkGray)
            
                    val isButtonEnabled = if (isDirectScript) {
                        topic.isNotBlank() && videoSearchTopic.isNotBlank()
                    } else {
                        topic.isNotBlank()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(if (!isButtonEnabled) disabledBrush else gradientBrush)
                            .clickable(enabled = isButtonEnabled) {
                                viewModel.resetState()
                                viewModel.prepareResources()
                                onNavigateToLoading()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                Text(
                    text = "GENERATE VIDEO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}