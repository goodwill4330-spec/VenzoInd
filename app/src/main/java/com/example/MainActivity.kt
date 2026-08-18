package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.BharatChatTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BharatChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val chatViewModel: BharatChatViewModel = viewModel()
            val isDark by chatViewModel.isDarkTheme.collectAsState()
            val currentScreen by chatViewModel.currentScreen.collectAsState()
            val isLoggedIn by chatViewModel.isUserLoggedIn.collectAsState()

            BharatChatTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.SPLASH -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        if (isLoggedIn) {
                                            chatViewModel.navigateTo(AppScreen.MAIN_APP)
                                        } else {
                                            chatViewModel.navigateTo(AppScreen.ONBOARDING)
                                        }
                                    }
                                )
                            }
                            AppScreen.ONBOARDING -> {
                                OnboardingScreen(
                                    onContinueToAuth = {
                                        chatViewModel.navigateTo(AppScreen.AUTH)
                                    }
                                )
                            }
                            AppScreen.AUTH -> {
                                AuthScreen(
                                    viewModel = chatViewModel,
                                    onLoginSuccess = {
                                        chatViewModel.navigateTo(AppScreen.MAIN_APP)
                                    }
                                )
                            }
                            AppScreen.MAIN_APP -> {
                                MainHomeScreen(viewModel = chatViewModel)
                            }
                            AppScreen.CHAT_DETAIL -> {
                                ChatDetailScreen(viewModel = chatViewModel)
                            }
                            AppScreen.ACTIVE_CALL -> {
                                ActiveCallScreen(viewModel = chatViewModel)
                            }
                            AppScreen.STORY_VIEWER -> {
                                StoryViewerScreen(viewModel = chatViewModel)
                            }
                            AppScreen.USER_PROFILE -> {
                                UserProfileScreen(
                                    viewModel = chatViewModel,
                                    onBackClick = {
                                        chatViewModel.navigateTo(AppScreen.MAIN_APP)
                                    }
                                )
                            }
                            AppScreen.CONTACTS_LIST -> {
                                ContactsListScreen(
                                    viewModel = chatViewModel,
                                    onBackClick = {
                                        chatViewModel.navigateTo(AppScreen.MAIN_APP)
                                    }
                                )
                            }
                            AppScreen.SETTINGS -> {
                                WhatsAppSettingsScreen(
                                    viewModel = chatViewModel,
                                    onBackClick = {
                                        chatViewModel.navigateTo(AppScreen.MAIN_APP)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
