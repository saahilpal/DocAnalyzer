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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitrous.docanalyzer.auth.ui.components.AuthButton
import com.nitrous.docanalyzer.auth.ui.components.AuthTextField
import com.nitrous.docanalyzer.auth.viewmodel.AuthViewModel
import com.nitrous.docanalyzer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestResetScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReset: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearInputs()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Reset password",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                ),
                color = PrimaryText,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Enter your email to receive a code",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            AuthTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = "Email Address",
                error = if (uiState.isSubmitted) uiState.emailError else null,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(32.dp))

            AuthButton(
                text = "Send Code",
                onClick = {
                    viewModel.requestReset {
                        onNavigateToReset()
                    }
                },
                isLoading = uiState.isLoading
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
