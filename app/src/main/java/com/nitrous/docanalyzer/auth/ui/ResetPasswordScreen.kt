@file:OptIn(ExperimentalMaterial3Api::class)

package com.nitrous.docanalyzer.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitrous.docanalyzer.auth.ui.components.AuthButton
import com.nitrous.docanalyzer.auth.ui.components.AuthTextField
import com.nitrous.docanalyzer.auth.viewmodel.AuthViewModel
import com.nitrous.docanalyzer.ui.theme.*

@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearInputs(keepEmail = true)
    }

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(Dimens.ScreenTopPadding))

            Text(
                text = "New password",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = Dimens.LetterSpacingTight
                ),
                color = PrimaryText,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Enter the code sent to your email and your new password",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.PaddingSmall, bottom = Dimens.PaddingExtraLarge)
            )

            AuthTextField(
                value = uiState.otp,
                onValueChange = { viewModel.onOtpChange(it) },
                label = "OTP",
                error = if (uiState.isSubmitted) uiState.otpError else null,
                imeAction = ImeAction.Next
            )

            // Resend OTP Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.resendCountdown > 0) {
                    Text(
                        text = "Resend in ${uiState.resendCountdown}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                } else {
                    TextButton(
                        onClick = { viewModel.resendResetOtp() },
                        enabled = !uiState.isResending,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (uiState.isResending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Resend code",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.PaddingSmall))

            AuthTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = "New Password",
                error = if (uiState.isSubmitted) uiState.passwordError else null,
                isPassword = true,
                imeAction = ImeAction.Done
            )

            if (uiState.successMessage != null) {
                Text(
                    text = uiState.successMessage!!,
                    color = SuccessGreen,
                    modifier = Modifier.padding(top = Dimens.PaddingMedium),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = DestructiveRed,
                    modifier = Modifier.padding(top = Dimens.PaddingMedium),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))

            AuthButton(
                text = "Update Password",
                onClick = { viewModel.resetPassword(onSuccess) },
                isLoading = uiState.isLoading
            )
        }
    }
}
