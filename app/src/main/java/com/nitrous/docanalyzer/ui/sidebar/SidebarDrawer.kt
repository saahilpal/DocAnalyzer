package com.nitrous.docanalyzer.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitrous.docanalyzer.ui.components.SidebarItem
import com.nitrous.docanalyzer.ui.theme.*
import com.nitrous.docanalyzer.viewmodel.ChatViewModel

@Composable
fun SidebarDrawer(
    viewModel: ChatViewModel,
    onClose: () -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(BackgroundBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Search & New Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(16.dp),
                color = ElevatedSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StrokeColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = HintText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.onSearchQueryChanged(it)
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = PrimaryText),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = HintText
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = {
                    viewModel.createNewSession()
                    onClose()
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Create,
                    contentDescription = "New Chat",
                    tint = BackgroundBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ############################
        // 6 — SIDEBAR LABEL
        // ############################
        Text(
            text = "Chat History",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            ),
            color = SecondaryText.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // History List
        val sessions = uiState.searchResults ?: uiState.sessions
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                SidebarItem(
                    session = session,
                    isActive = session.id == uiState.activeSessionId,
                    onClick = {
                        viewModel.selectSession(session.id)
                        onClose()
                    },
                    onRename = { newTitle ->
                        viewModel.renameSession(session.id, newTitle)
                    },
                    onDelete = {
                        viewModel.deleteSession(session.id)
                    }
                )
            }
        }

        // Profile Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = ElevatedSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StrokeColor),
            onClick = onProfileClick
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initial = (uiState.currentUser?.name ?: "U").take(1).uppercase()
                        Text(
                            text = initial,
                            color = BackgroundBlack,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.currentUser?.name ?: "User",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = HintText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
