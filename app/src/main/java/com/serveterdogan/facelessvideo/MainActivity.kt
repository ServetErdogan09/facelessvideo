package com.serveterdogan.facelessvideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.serveterdogan.facelessvideo.presentation.ui.navigation.AppNavGraph
import com.serveterdogan.facelessvideo.ui.theme.FacelessvideoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FacelessvideoTheme {
                AppNavGraph()
            }
        }
    }
}

