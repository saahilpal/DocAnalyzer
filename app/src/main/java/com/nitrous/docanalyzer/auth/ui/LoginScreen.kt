package com.nitrous.docanalyzer.auth.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitrous.docanalyzer.auth.ui.components.AuthButton
import com.nitrous.docanalyzer.auth.ui.components.AuthTextField
import com.nitrous.docanalyzer.auth.viewmodel.AuthViewModel
import com.nitrous.docanalyzer.ui.components.DocAnalyzerLaunchAnimation
import com.nitrous.docanalyzer.ui.theme.*
import com.nitrous.docanalyzer.util.ValidationUtils

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearInputs()
    }

    val isEmailValid = remember(uiState.email) { ValidationUtils.isValidEmail(uiState.email) }
    val isPasswordNotEmpty = remember(uiState.password) { uiState.password.isNotEmpty() }
    val canSubmit = isEmailValid && isPasswordNotEmpty

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.AuthHorizontalPadding)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Use fixed-size animation
            DocAnalyzerLaunchAnimation()
            
            Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
            
            Text(
                text = Strings.APP_NAME_UPPER,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    fontSize = 12.sp
                ),
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.weight(0.2f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.WELCOME_BACK,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = Dimens.LetterSpacingTight
                    ),
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = Strings.LOGIN_SUBTITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    modifier = Modifier.padding(top = Dimens.PaddingSmall, bottom = Dimens.PaddingExtraLarge),
                    textAlign = TextAlign.Center
                )

                AuthTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = Strings.EMAIL_LABEL,
                    error = if (uiState.isSubmitted) uiState.emailError else null,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
                
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                
                AuthTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = Strings.PASSWORD_LABEL,
                    error = if (uiState.isSubmitted) uiState.emailError else null,
                    isPassword = true,
                    imeAction = ImeAction.Done
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = Strings.FORGOT_PASSWORD,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

                AuthButton(
                    text = Strings.CONTINUE_BUTTON,
                    onClick = { viewModel.login() },
                    isLoading = uiState.isLoading,
                    enabled = canSubmit
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = DestructiveRed,
                        modifier = Modifier.padding(top = Dimens.PaddingMedium),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.padding(top = Dimens.PaddingLarge),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.SIGN_UP_PROMPT,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            text = Strings.SIGN_UP_ACTION,
                            color = Color.White, 
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(0.8f))
        }
    }
}
