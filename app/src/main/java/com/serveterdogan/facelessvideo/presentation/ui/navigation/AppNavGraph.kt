package com.serveterdogan.facelessvideo.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.serveterdogan.facelessvideo.presentation.ui.screens.LoadingScreen
import com.serveterdogan.facelessvideo.presentation.ui.screens.PromptScreen
import com.serveterdogan.facelessvideo.presentation.ui.screens.ResultScreen
import com.serveterdogan.facelessvideo.presentation.ui.screens.VideoSelectionScreen
import com.serveterdogan.facelessvideo.presentation.viewmodel.PromptViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "prompt_screen") {
        
        composable("prompt_screen") { entry ->
            val viewModel: PromptViewModel = hiltViewModel(entry)
            PromptScreen(
                viewModel = viewModel,
                onNavigateToLoading = { 
                    navController.navigate("loading_screen") 
                }
            )
        }
        
        composable("loading_screen") { entry ->
            val promptEntry = remember(entry) { navController.getBackStackEntry("prompt_screen") }
            val viewModel: PromptViewModel = hiltViewModel(promptEntry)
            
            LoadingScreen(
                viewModel = viewModel,
                onNavigateToResult = { videoPath: String, script: String, audioPath: String ->
                    val encodedScript = java.net.URLEncoder.encode(script, "UTF-8")
                    val encodedAudio = java.net.URLEncoder.encode(audioPath, "UTF-8")
                    val encodedVideo = java.net.URLEncoder.encode(videoPath, "UTF-8")
                    
                    navController.navigate("result_screen?videoPath=$encodedVideo&script=$encodedScript&audioPath=$encodedAudio") {
                        popUpTo("prompt_screen") { inclusive = false }
                    }
                },
                onNavigateBackError = {
                    navController.popBackStack()
                },
                onNavigateToSelection = {
                    navController.navigate("video_selection_screen") {
                        popUpTo("prompt_screen") { inclusive = false }
                    }
                }
            )
        }

        composable("video_selection_screen") { entry ->
            val promptEntry = remember(entry) { navController.getBackStackEntry("prompt_screen") }
            val viewModel: PromptViewModel = hiltViewModel(promptEntry)

            VideoSelectionScreen(
                viewModel = viewModel,
                onNavigateToLoading = {
                    navController.navigate("loading_screen")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("result_screen?videoPath={videoPath}&script={script}&audioPath={audioPath}") { backStackEntry ->
            val videoPath = backStackEntry.arguments?.getString("videoPath") ?: ""
            val script = backStackEntry.arguments?.getString("script") ?: ""
            val audioPath = backStackEntry.arguments?.getString("audioPath") ?: ""
            
            ResultScreen(
                videoPath = java.net.URLDecoder.decode(videoPath, "UTF-8"),
                script = java.net.URLDecoder.decode(script, "UTF-8"),
                audioPath = java.net.URLDecoder.decode(audioPath, "UTF-8"),
                onNavigateBack = {
                    navController.popBackStack("prompt_screen", inclusive = false)
                }
            )
        }
    }
}
