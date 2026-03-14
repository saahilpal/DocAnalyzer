package com.nitrous.docanalyzer.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.nitrous.docanalyzer.auth.ui.components.AuthButton
import com.nitrous.docanalyzer.auth.ui.components.AuthTextField
import com.nitrous.docanalyzer.auth.viewmodel.AuthViewModel
import com.nitrous.docanalyzer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearInputs(keepEmail = true)
    }

    LaunchedEffect(uiState.email) {
        if (uiState.email.isBlank()) {
            onBack()
        }
    }

    if (uiState.email.isBlank()) return

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
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(Dimens.ScreenTopPadding))

            Text(
                text = "Verify your email",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = Dimens.LetterSpacingTight
                ),
                color = PrimaryText,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "We sent a code to ${uiState.email}",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                modifier = Modifier.padding(top = Dimens.PaddingSmall, bottom = Dimens.PaddingExtraLarge),
                textAlign = TextAlign.Center
            )

            AuthTextField(
                value = uiState.otp,
                onValueChange = { viewModel.onOtpChange(it) },
                label = "OTP",
                error = if (uiState.isSubmitted) uiState.otpError else null,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))

            AuthButton(
                text = "Verify",
                onClick = { viewModel.verifyOtp(onSuccess) },
                isLoading = uiState.isLoading
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

            Spacer(modifier = Modifier.height(Dimens.PaddingLarge))

            TextButton(
                onClick = { viewModel.resendOtp() },
                enabled = uiState.resendCountdown == 0 && !uiState.isResending
            ) {
                val text = if (uiState.resendCountdown > 0) {
                    "Resend code in ${uiState.resendCountdown}s"
                } else {
                    "Resend code"
                }
                Text(
                    text = text, 
                    color = if (uiState.resendCountdown > 0) HintText else Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
