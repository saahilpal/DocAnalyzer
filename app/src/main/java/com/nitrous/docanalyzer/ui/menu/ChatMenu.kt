package com.nitrous.docanalyzer.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nitrous.docanalyzer.viewmodel.ChatViewModel
import com.nitrous.docanalyzer.ui.theme.DestructiveRed

@Composable
fun ChatMenu(
    viewModel: ChatViewModel,
    onRename: () -> Unit,
    onViewDocs: () -> Unit,
    onDelete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.padding(end = 8.dp)) {
        IconButton(onClick = { viewModel.toggleMenu() }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
        }

        DropdownMenu(
            expanded = uiState.isMenuVisible,
            onDismissRequest = { viewModel.toggleMenu() },
            containerColor = Color(0xFF1A1A1A)
        ) {
            DropdownMenuItem(
                text = { Text("Rename chat", color = Color.White) },
                onClick = {
                    viewModel.toggleMenu()
                    onRename()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) }
            )
            DropdownMenuItem(
                text = { Text("View documents", color = Color.White) },
                onClick = {
                    viewModel.toggleMenu()
                    onViewDocs()
                },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color.White) }
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            DropdownMenuItem(
                text = { Text("Delete chat", color = DestructiveRed) },
                onClick = {
                    viewModel.toggleMenu()
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DestructiveRed) }
            )
        }
    }
}
