@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nitrous.docanalyzer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nitrous.docanalyzer.model.UploadFile
import com.nitrous.docanalyzer.ui.components.ChatBubble
import com.nitrous.docanalyzer.ui.components.SessionItem
import com.nitrous.docanalyzer.ui.components.TypingIndicator
import com.nitrous.docanalyzer.ui.components.UploadCard
import com.nitrous.docanalyzer.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val drawerWidth = (screenWidth * 0.75f).coerceAtMost(300.dp)
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            val fileName = "upload_${System.currentTimeMillis()}.pdf"
            val tempFile = File(context.cacheDir, fileName)
            contentResolver.openInputStream(it)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.uploadFile(fileName, tempFile.length(), tempFile)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(modifier = Modifier.width(drawerWidth)) {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxHeight(),
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    drawerShape = RoundedCornerShape(0.dp)
                ) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Chat History",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.sessions, key = { it.id }) { session ->
                            SessionItem(
                                session = session,
                                isActive = session.id == uiState.activeSessionId,
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    scope.launch { drawerState.close() }
                                },
                                onDelete = { viewModel.deleteSession(session.id) }
                            )
                        }
                    }
                    Box(modifier = Modifier.padding(16.dp)) {
                        OutlinedButton(
                            onClick = { 
                                viewModel.createNewSession()
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("New Chat", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val title = uiState.sessions.find { it.id == uiState.activeSessionId }?.title ?: "Chat"
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Menu", modifier = Modifier.size(20.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleTestSetting("network") }) {
                            Icon(
                                if (uiState.testSettings.networkError) Icons.Filled.SignalWifiOff else Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(20.dp),
                                tint = if (uiState.testSettings.networkError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            val messages = remember(uiState.activeSessionId, uiState.messages) {
                uiState.messages[uiState.activeSessionId] ?: emptyList()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Message List Layer
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(message)
                    }
                    if (uiState.isTyping) {
                        item(key = "typing_indicator") {
                            TypingIndicator()
                        }
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }

                // Keyboard Visibility Detection
                val isKeyboardVisible = WindowInsets.isImeVisible
                
                // Animated offset for center content
                val centerOffset by animateDpAsState(
                    targetValue = if (isKeyboardVisible) (-100).dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "center_content_offset"
                )

                // Center Content Layer
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = centerOffset),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyChatState(
                            isKeyboardVisible = isKeyboardVisible,
                            onSuggestionClick = { viewModel.sendMessage(it) }
                        )
                    }
                }

                // Input Bar Layer anchored to IME
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                ) {
                    ChatInputArea(
                        uploads = uiState.currentUploads,
                        onSend = { viewModel.sendMessage(it) },
                        onUpload = { filePickerLauncher.launch("application/pdf") },
                        onCancelUpload = { viewModel.removeUpload(it) },
                        isTyping = uiState.isTyping
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputArea(
    uploads: List<UploadFile>,
    onSend: (String) -> Unit,
    onUpload: () -> Unit,
    onCancelUpload: (String) -> Unit,
    isTyping: Boolean
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uploads.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth(0.92f).padding(bottom = 8.dp)) {
                uploads.forEach { file ->
                    UploadCard(file = file, onCancel = { onCancelUpload(file.id) })
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = onUpload, modifier = Modifier.size(40.dp).padding(bottom = 4.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 150.dp),
                    placeholder = { 
                        Text(
                            "Message", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        ) 
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 6
                )
                
                val canSend = (text.isNotBlank() || uploads.isNotEmpty()) && !isTyping
                Box(
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (canSend) {
                                onSend(text)
                                text = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        if (isTyping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Send",
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(isKeyboardVisible: Boolean, onSuggestionClick: (String) -> Unit) {
    // Quick action buttons offset to stay above keyboard
    val actionOffset by animateDpAsState(
        targetValue = if (isKeyboardVisible) (-20).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "action_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isKeyboardVisible) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "How can I help you today?",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Upload a document to summarize, analyze, or ask questions with AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
        }
        
        val suggestions = listOf("Summarize this document", "Extract key findings", "Draft an email from this")
        Column(modifier = Modifier.offset(y = actionOffset)) {
            suggestions.forEach { suggestion ->
                OutlinedButton(
                    onClick = { onSuggestionClick(suggestion) },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(vertical = 4.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(suggestion, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}
