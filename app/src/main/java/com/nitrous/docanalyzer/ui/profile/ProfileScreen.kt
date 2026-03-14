package com.nitrous.docanalyzer.ui.profile

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitrous.docanalyzer.viewmodel.ChatViewModel
import com.nitrous.docanalyzer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.currentUser

    val formattedDate = remember(user?.createdAt) {
        if (user?.createdAt == null) "unknown"
        else {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(user.createdAt)
                val outputFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                outputFormat.format(date!!)
            } catch (e: Exception) {
                user.createdAt
            }
        }
    }

    Scaffold(
        containerColor = BackgroundBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "PROFILE", 
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = PrimaryText
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundBlack
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            
            // Avatar
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = ElevatedSurface,
                border = BorderStroke(1.dp, StrokeColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (user?.name ?: "U").take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Name
            Text(
                text = user?.name ?: "User",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = PrimaryText
            )

            // Email
            Text(
                text = user?.email ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText
            )

            Spacer(Modifier.height(8.dp))

            // Joined Date
            Text(
                text = "Member since $formattedDate",
                style = MaterialTheme.typography.labelMedium,
                color = HintText
            )

            Spacer(Modifier.weight(1f))

            // Change Password Button
            OutlinedButton(
                onClick = onChangePassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StrokeColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText)
            ) {
                Text("Update Password", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            }

            Spacer(Modifier.height(12.dp))

            // Logout Button - Using Theme DestructiveRed
            Button(
                onClick = { 
                    viewModel.logout {
                        onLogout()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DestructiveRed,
                    contentColor = Color.White
                )
            ) {
                Text("Log out", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = HintText
            )
        }
    }
}
