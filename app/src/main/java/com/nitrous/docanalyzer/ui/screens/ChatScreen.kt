@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nitrous.docanalyzer.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nitrous.docanalyzer.model.MessageRole
import com.nitrous.docanalyzer.ui.components.*
import com.nitrous.docanalyzer.ui.dialogs.DeleteConfirmDialog
import com.nitrous.docanalyzer.ui.dialogs.RenameChatDialog
import com.nitrous.docanalyzer.ui.menu.ChatMenu
import com.nitrous.docanalyzer.ui.profile.ProfileScreen
import com.nitrous.docanalyzer.ui.sheets.DocViewerSheet
import com.nitrous.docanalyzer.ui.sidebar.SidebarDrawer
import com.nitrous.docanalyzer.ui.theme.*
import com.nitrous.docanalyzer.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class AppScreen { Chat, Profile }

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onNavigateToRequestReset: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    
    var currentAppScreen by remember { mutableStateOf(AppScreen.Chat) }
    var inputText by remember { mutableStateOf("") }

    BackHandler(enabled = currentAppScreen == AppScreen.Profile || drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentAppScreen = AppScreen.Chat
        }
    }

    val messages = remember(uiState.activeSessionId, uiState.messages) {
        uiState.messages[uiState.activeSessionId] ?: emptyList()
    }
    
    LaunchedEffect(messages.size, uiState.isTyping) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            val fileName = "upload_${System.currentTimeMillis()}.pdf"
            val tempFile = File(context.cacheDir, fileName)
            contentResolver.openInputStream(it)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            viewModel.uploadFile(fileName, tempFile.length(), tempFile)
        }
    }

    if (currentAppScreen == AppScreen.Profile) {
        ProfileScreen(
            viewModel = viewModel,
            onBack = { currentAppScreen = AppScreen.Chat },
            onLogout = onLogout,
            onChangePassword = onNavigateToRequestReset
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = BackgroundBlack,
                    drawerShape = RoundedCornerShape(Dimens.PaddingNone),
                    modifier = Modifier.width(300.dp).fillMaxHeight()
                ) {
                    SidebarDrawer(
                        viewModel = viewModel,
                        onClose = { scope.launch { drawerState.close() } },
                        onProfileClick = {
                            currentAppScreen = AppScreen.Profile
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                containerColor = BackgroundBlack,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Surface(
                                color = ElevatedSurface,
                                shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                                modifier = Modifier.padding(vertical = Dimens.PaddingSmall),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StrokeColor)
                            ) {
                                Text(
                                    text = Constants.APP_NAME,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = Dimens.LetterSpacingLoose
                                    ),
                                    color = PrimaryText,
                                    modifier = Modifier.padding(horizontal = Dimens.CornerRadiusMedium, vertical = Dimens.PaddingTiny)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        actions = {
                            ChatMenu(
                                viewModel = viewModel,
                                onRename = { viewModel.updateUi { it.copy(showRenameDialog = true) } },
                                onViewDocs = {
                                    viewModel.loadSessionPdfs(uiState.activeSessionId ?: "")
                                    viewModel.updateUi { it.copy(showDocViewer = true) }
                                },
                                onDelete = { viewModel.updateUi { it.copy(showDeleteConfirm = true) } }
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = BackgroundBlack
                        )
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .background(BackgroundBlack)
                            .imePadding()
                    ) {
                        if (uiState.currentUploads.isNotEmpty()) {
                            uiState.currentUploads.forEach { file ->
                                UploadCard(file = file, onCancel = { viewModel.removeUpload(file.id) })
                            }
                        }
                        
                        ChatInputBar(
                            text = inputText,
                            onValueChange = { inputText = it },
                            onSend = {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            },
                            onUpload = { filePickerLauncher.launch("application/pdf") },
                            isSending = uiState.isSending
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = Dimens.PaddingSmall, bottom = 16.dp)
                    ) {
                        itemsIndexed(messages, key = { _, msg -> msg.id }) { index, message ->
                            ChatBubble(
                                message = message,
                                isStreaming = uiState.isTyping && index == messages.size - 1 && message.role != MessageRole.USER
                            )
                        }
                        if (uiState.isTyping && (messages.isEmpty() || messages.last().role == MessageRole.USER)) {
                            item(key = "typing_indicator") {
                                TypingIndicator()
                            }
                        }
                    }

                    // ChatGPT-style Empty Chat State
                    // Fix: Ensure it is only visible when messages are empty AND not typing
                    AnimatedVisibility(
                        visible = messages.isEmpty() && !uiState.isTyping,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(200)),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .offset(y = (-60).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "How can I help you today?",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            EmptyStateSuggestions(
                                onAction = { action ->
                                    viewModel.sendMessage(action)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showRenameDialog) {
        val currentSession = uiState.sessions.find { it.id == uiState.activeSessionId }
        RenameChatDialog(
            currentTitle = currentSession?.title ?: "",
            onConfirm = { newTitle ->
                viewModel.renameSession(uiState.activeSessionId ?: "", newTitle)
                viewModel.updateUi { it.copy(showRenameDialog = false) }
            },
            onDismiss = { viewModel.updateUi { it.copy(showRenameDialog = false) } }
        )
    }

    if (uiState.showDeleteConfirm) {
        DeleteConfirmDialog(
            onConfirm = {
                viewModel.deleteSession(uiState.activeSessionId ?: "")
                viewModel.updateUi { it.copy(showDeleteConfirm = false) }
            },
            onDismiss = { viewModel.updateUi { it.copy(showDeleteConfirm = false) } }
        )
    }

    if (uiState.showDocViewer) {
        DocViewerSheet(
            documents = uiState.sessionDocuments,
            isLoading = uiState.isDocsLoading,
            onDelete = { viewModel.deletePdf(it) },
            onDismiss = { viewModel.updateUi { it.copy(showDocViewer = false) } }
        )
    }
}

@Composable
fun EmptyStateSuggestions(
    onAction: (String) -> Unit
) {
    val actions = listOf(
        "Summarize document",
        "Extract key points",
        "Explain main ideas"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { action ->
            Surface(
                onClick = { onAction(action) },
                color = ElevatedSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StrokeColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (action) {
                        "Summarize document" -> Icons.Default.Description
                        "Extract key points" -> Icons.Default.List
                        else -> Icons.Default.Lightbulb
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = action,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText
                    )
                }
            }
        }
    }
}
