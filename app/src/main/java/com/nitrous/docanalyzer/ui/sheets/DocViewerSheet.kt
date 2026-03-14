@file:OptIn(ExperimentalMaterial3Api::class)

package com.nitrous.docanalyzer.ui.sheets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitrous.docanalyzer.network.dto.PdfDto
import com.nitrous.docanalyzer.ui.components.SkeletonDocRow
import com.nitrous.docanalyzer.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DocViewerSheet(
    documents: List<PdfDto>,
    isLoading: Boolean,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundBlack,
        dragHandle = { BottomSheetDefaults.DragHandle(color = StrokeColor) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Documents",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (isLoading) {
                Column {
                    repeat(3) { SkeletonDocRow() }
                }
            } else if (documents.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("No documents yet", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(documents, key = { _, doc -> doc.id ?: -1 }) { index, doc ->
                        var itemVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(index * 50L)
                            itemVisible = true
                        }
                        
                        AnimatedVisibility(
                            visible = itemVisible,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { 8 },
                            exit = fadeOut()
                        ) {
                            DocRow(doc = doc, onDelete = { onDelete(doc.id ?: -1) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocRow(doc: PdfDto, onDelete: () -> Unit) {
    var showSuccess by remember { mutableStateOf(doc.status == "indexed") }
    
    LaunchedEffect(doc.status) {
        if (doc.status == "indexed") {
            showSuccess = true
            delay(1500)
            showSuccess = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
                
                if (showSuccess) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = 4.dp, y = (-4).dp)
                            .animateContentSize()
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    doc.title ?: "Untitled",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = PrimaryText,
                    maxLines = 1
                )
                Text(
                    (doc.status ?: "Unknown").uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = if (doc.status == "failed") DestructiveRed else SecondaryText
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DestructiveRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
