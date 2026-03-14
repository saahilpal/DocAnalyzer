package com.nitrous.docanalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nitrous.docanalyzer.auth.data.AuthRepository
import com.nitrous.docanalyzer.auth.ui.*
import com.nitrous.docanalyzer.auth.viewmodel.AuthViewModel
import com.nitrous.docanalyzer.auth.viewmodel.AuthViewModelFactory
import com.nitrous.docanalyzer.network.RetrofitClient
import com.nitrous.docanalyzer.network.RuntimeConfigManager
import com.nitrous.docanalyzer.ui.screens.ChatScreen
import com.nitrous.docanalyzer.ui.theme.*
import com.nitrous.docanalyzer.ui.motion.AppMotion
import com.nitrous.docanalyzer.ui.components.AppLaunchScreen
import kotlinx.coroutines.launch

object LaunchState {
    var hasPlayed = false
}

class MainActivity : ComponentActivity() {
    private var isConfigLoaded by mutableStateOf(false)
    private var configError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        RetrofitClient.init(applicationContext)
        
        lifecycleScope.launch {
            try {
                RuntimeConfigManager.fetchRemoteConfig()
                RetrofitClient.rebuild()
                isConfigLoaded = true
            } catch (e: Exception) {
                configError = e.message ?: "Failed to load configuration"
            }
        }
        
        enableEdgeToEdge()
        setContent {
            DocAnalyzerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundBlack
                ) {
                    var showLaunchAnimation by remember { mutableStateOf(!LaunchState.hasPlayed) }

                    Crossfade(
                        targetState = showLaunchAnimation,
                        animationSpec = AppMotion.FadeSpec,
                        label = "LaunchTransition"
                    ) { isLaunching ->
                        if (isLaunching) {
                            AppLaunchScreen(onAnimationComplete = { 
                                LaunchState.hasPlayed = true
                                showLaunchAnimation = false 
                            })
                        } else {
                            if (configError != null) {
                                ErrorGate(configError!!) {
                                    isConfigLoaded = false
                                    configError = null
                                    lifecycleScope.launch {
                                        try {
                                            RuntimeConfigManager.fetchRemoteConfig()
                                            RetrofitClient.rebuild()
                                            isConfigLoaded = true
                                        } catch (e: Exception) {
                                            configError = e.message
                                        }
                                    }
                                }
                            } else if (isConfigLoaded) {
                                AppNavigation()
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorGate(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Dimens.PaddingExtraLarge)) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeLarge), tint = Color.White)
            Spacer(Modifier.height(Dimens.PaddingMedium))
            Text(
                text = Strings.CONNECTION_ERROR, 
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            )
            Spacer(Modifier.height(Dimens.PaddingSmall))
            Text(
                text = message, 
                style = MaterialTheme.typography.bodyMedium, 
                color = Color.White.copy(alpha = 0.7f), 
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Dimens.PaddingExtraLarge))
            OutlinedButton(
                onClick = onRetry,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
            ) {
                Text(Strings.RETRY_BUTTON, color = Color.White)
            }
        }
    }
}

enum class Screen {
    Login, Register, Verify, Chat, RequestReset, ResetPassword
}

@Composable
fun AppNavigation() {
    val authManager = RetrofitClient.authManager
    val authRepository = remember { AuthRepository(RetrofitClient.apiService, authManager) }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    
    val authState by authViewModel.uiState.collectAsState()
    val isTokenValid by authManager.authState.collectAsState()
    
    var currentScreen by remember { mutableStateOf(if (authState.isAuthenticated) Screen.Chat else Screen.Login) }

    LaunchedEffect(authState.isAuthenticated, isTokenValid) {
        val authenticated = authState.isAuthenticated && isTokenValid
        if (authenticated) {
            if (currentScreen == Screen.Login || currentScreen == Screen.Register || currentScreen == Screen.Verify) {
                currentScreen = Screen.Chat
            }
        } else {
            if (currentScreen == Screen.Chat || currentScreen == Screen.RequestReset || currentScreen == Screen.ResetPassword) {
                currentScreen = Screen.Login
            }
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { 
            AppMotion.EnterTransition togetherWith AppMotion.ExitTransition
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.Login -> LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.Register 
                },
                onNavigateToForgotPassword = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.RequestReset 
                }
            )
            Screen.Register -> RegisterScreen(
                viewModel = authViewModel,
                onNavigateToVerify = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.Verify 
                },
                onNavigateToLogin = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.Login 
                }
            )
            Screen.Verify -> VerifyScreen(
                viewModel = authViewModel,
                onSuccess = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.Login 
                },
                onBack = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.Register 
                }
            )
            Screen.Chat -> ChatScreen(
                onNavigateToRequestReset = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.RequestReset 
                },
                onLogout = {
                    authViewModel.logout()
                    currentScreen = Screen.Login
                }
            )
            Screen.RequestReset -> RequestResetScreen(
                viewModel = authViewModel,
                onNavigateBack = { 
                    authViewModel.clearStatus()
                    currentScreen = if (authState.isAuthenticated && isTokenValid) Screen.Chat else Screen.Login
                },
                onNavigateToReset = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.ResetPassword 
                }
            )
            Screen.ResetPassword -> ResetPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.RequestReset 
                },
                onSuccess = { 
                    authViewModel.clearStatus()
                    currentScreen = Screen.Login 
                }
            )
        }
    }
}
