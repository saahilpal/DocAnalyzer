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
import com.nitrous.docanalyzer.util.ValidationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToVerify: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearInputs()
    }

    val isNameNotEmpty = remember(uiState.name) { uiState.name.isNotBlank() }
    val isEmailValid = remember(uiState.email) { ValidationUtils.isValidEmail(uiState.email) }
    val isPasswordValid = remember(uiState.password) { uiState.password.length >= 8 }
    val canSubmit = isNameNotEmpty && isEmailValid && isPasswordValid

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
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(Dimens.ScreenTopPadding))

            Text(
                text = "Create account",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = Dimens.LetterSpacingTight
                ),
                color = PrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))

            AuthTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = "Full Name",
                error = if (uiState.isSubmitted) uiState.nameError else null,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

            AuthTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = "Email Address",
                error = if (uiState.isSubmitted) uiState.emailError else null,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

            AuthTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = "Password",
                error = if (uiState.isSubmitted) uiState.passwordError else null,
                isPassword = true,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))

            AuthButton(
                text = "Continue",
                onClick = { viewModel.register(onNavigateToVerify) },
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

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.padding(bottom = Dimens.PaddingExtraLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Log in", 
                        color = Color.White, 
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
