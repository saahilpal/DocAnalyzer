@file:OptIn(ExperimentalFoundationApi::class)

package com.nitrous.docanalyzer.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitrous.docanalyzer.model.ChatMessage
import com.nitrous.docanalyzer.model.ChatSession
import com.nitrous.docanalyzer.model.MessageRole
import com.nitrous.docanalyzer.model.UploadFile
import com.nitrous.docanalyzer.model.UploadStatus
import com.nitrous.docanalyzer.ui.motion.AppMotion
import com.nitrous.docanalyzer.ui.theme.*
import kotlinx.coroutines.delay

private const val TEXT_STREAM_DELAY = 15L
private const val THINKING_ANIMATION_DURATION = 600
private const val CURSOR_ANIMATION_DURATION = 400
private const val TYPING_DOT_ANIMATION_DURATION = 600

@Composable
fun ChatBubble(
    message: ChatMessage,
    isStreaming: Boolean = false
) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = AppMotion.ScreenTransitionSpec,
        label = "fade"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.98f,
        animationSpec = AppMotion.ScreenTransitionSpec,
        label = "scale"
    )

    var displayedText by remember { mutableStateOf(if (isStreaming && message.content.isEmpty()) "" else message.content) }
    
    LaunchedEffect(message.content, isStreaming) {
        if (isStreaming) {
            while (displayedText.length < message.content.length) {
                displayedText = message.content.take(displayedText.length + 1)
                delay(TEXT_STREAM_DELAY)
            }
        } else {
            displayedText = message.content
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val thinkingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(THINKING_ANIMATION_DURATION),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinking_alpha"
    )

    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CURSOR_ANIMATION_DURATION),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.animateContentSize(),
            color = if (isUser) BubbleBackground else ElevatedSurface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (isStreaming && displayedText.isEmpty()) {
                    Text(
                        text = "...",
                        color = PrimaryText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        modifier = Modifier.alpha(thinkingAlpha)
                    )
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = displayedText,
                            color = PrimaryText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        )
                        if (isStreaming) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 2.dp, bottom = 2.dp)
                                    .size(width = 2.dp, height = 16.dp)
                                    .alpha(cursorAlpha)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionRow(
    onAction: (String) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        "Summarize document",
        "Extract key points",
        "Explain main ideas"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(actions) { action ->
                SuggestionChip(
                    onClick = { onAction(action) },
                    label = {
                        Text(
                            text = action,
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryText
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = ElevatedSurface,
                        labelColor = PrimaryText
                    ),
                    border = BorderStroke(1.dp, StrokeColor),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Surface(
        color = ElevatedSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 12.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedDot(delay = 0)
            AnimatedDot(delay = 200)
            AnimatedDot(delay = 400)
        }
    }
}

@Composable
private fun AnimatedDot(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(TYPING_DOT_ANIMATION_DURATION, delayMillis = delay),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(Color.White)
    )
}

@Composable
fun SidebarItem(
    session: ChatSession,
    isActive: Boolean,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AppMotion.PressScaleTarget else 1f,
        animationSpec = if (isPressed) AppMotion.MicroInteractionSpec else AppMotion.ReleaseSpring,
        label = "press"
    )

    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember(session.title) { mutableStateOf(session.title) }
    var menuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            color = if (isActive) ElevatedSurface else Color.Transparent,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onLongClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            menuExpanded = true
                        },
                        onClick = { if (!isEditing) onClick() }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isActive) Color.White else SecondaryText
                )
                Spacer(Modifier.width(12.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        BasicTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PrimaryText),
                            cursorBrush = SolidColor(Color.White),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                onRename(editedTitle)
                                isEditing = false
                            })
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    } else {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                            ),
                            color = if (isActive) PrimaryText else SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            containerColor = ElevatedSurface
        ) {
            DropdownMenuItem(
                text = { Text("Rename", color = PrimaryText) },
                onClick = {
                    menuExpanded = false
                    isEditing = true
                },
                leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = PrimaryText, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = DestructiveRed) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = DestructiveRed, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onUpload: () -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val sendButtonScale by animateFloatAsState(
        targetValue = if (isPressed) AppMotion.PressScaleTarget else 1f,
        animationSpec = if (isPressed) AppMotion.MicroInteractionSpec else AppMotion.ReleaseSpring,
        label = "send_button_scale"
    )

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(56.dp),
        shape = DockShape,
        color = InputDockColor,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, StrokeColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onUpload,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(Icons.Default.Add, "Attach", tint = SecondaryText)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    Text(
                        "Ask Doc Analyzer",
                        color = HintText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = PrimaryText,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(Color.White)
                )
            }
            
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isSending,
                interactionSource = interactionSource,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = sendButtonScale
                        scaleY = sendButtonScale
                    }
                    .clip(CircleShape)
                    .background(if (text.isNotBlank() && !isSending) Color.White else ElevatedSurface)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = BackgroundBlack,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) BackgroundBlack else HintText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UploadCard(file: UploadFile, onCancel: () -> Unit) {
    val isUploading = file.status == UploadStatus.UPLOADING || file.status == UploadStatus.PROCESSING
    val isCompleted = file.status == UploadStatus.INDEXED
    val isFailed = file.status == UploadStatus.FAILED

    val animatedProgress by animateFloatAsState(
        targetValue = file.progress,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "upload_progress"
    )

    val checkmarkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = EaseOut),
        label = "checkmark_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = ElevatedSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isFailed) DestructiveRed.copy(alpha = 0.5f) else StrokeColor)
    ) {
        Box {
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .shimmerEffect()
                )
            }

            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = if (isFailed) DestructiveRed else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.CenterStart) {
                        if (isUploading) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(CircleShape),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        } else if (isFailed) {
                            Text(
                                "Upload failed",
                                style = MaterialTheme.typography.labelSmall,
                                color = DestructiveRed
                            )
                        } else if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = SuccessGreen,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer {
                                        scaleX = checkmarkScale
                                        scaleY = checkmarkScale
                                    }
                            )
                        }
                    }
                }
                
                if (!isCompleted) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = HintText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
