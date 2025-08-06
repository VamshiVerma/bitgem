package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.model.DeliveryStatus
import com.bitchat.android.mesh.BluetoothMeshService
import com.google.ai.edge.gallery.ui.common.chat.MarkdownText
import java.text.SimpleDateFormat
import java.util.*

/**
 * Message display components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */

@Composable
fun MessagesList(
    messages: List<BitchatMessage>,
    currentUserNickname: String,
    meshService: BluetoothMeshService,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Gallery-style auto-scroll: scroll on new messages and content changes with animation
    LaunchedEffect(
        messages.size,
        messages.lastOrNull()?.content
    ) {
        if (messages.isNotEmpty()) {
            // Use animateScrollToItem like Gallery with high scroll offset for smooth experience
            listState.animateScrollToItem(messages.lastIndex, scrollOffset = 10000)
        }
    }
    
    SelectionContainer(modifier = modifier) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), // Better spacing between messages
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = messages,
                key = { it.id } // Add key for better performance
            ) { message ->
                MessageItem(
                    message = message,
                    currentUserNickname = currentUserNickname,
                    meshService = meshService
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    message: BitchatMessage,
    currentUserNickname: String,
    meshService: BluetoothMeshService
) {
    val colorScheme = MaterialTheme.colorScheme
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    // Determine if this is a user message, AI message, or other
    val isUserMessage = message.sender == currentUserNickname || message.senderPeerID == meshService.myPeerID
    val isAiMessage = message.content.startsWith("🤖") || message.sender == "AI"
    val isSystemMessage = message.sender == "system"
    
    // Enhanced bubble-style layout with better alignment and spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start
    ) {
        // Message bubble with better sizing
        MessageBubble(
            message = message,
            isUserMessage = isUserMessage,
            isAiMessage = isAiMessage,
            isSystemMessage = isSystemMessage,
            currentUserNickname = currentUserNickname,
            meshService = meshService,
            timeFormatter = timeFormatter,
            modifier = Modifier
                .fillMaxWidth(0.85f) // Use percentage instead of fixed max width
                .padding(
                    start = if (isUserMessage) 48.dp else 0.dp, // More space on left for user messages
                    end = if (isUserMessage) 0.dp else 48.dp   // More space on right for other messages
                )
        )
    }
}

@Composable
fun MessageBubble(
    message: BitchatMessage,
    isUserMessage: Boolean,
    isAiMessage: Boolean,
    isSystemMessage: Boolean,
    currentUserNickname: String,
    meshService: BluetoothMeshService,
    timeFormatter: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Simplified bubble colors - no black, clear contrast
    val bubbleColor = when {
        isSystemMessage -> Color(0xFFEEEEEE) // Light gray
        isAiMessage -> Color(0xFFE8F5E8) // Light green for AI  
        isUserMessage -> Color(0xFF1976D2) // Blue for user
        else -> Color(0xFFF5F5F5) // Very light gray for others
    }
    
    val textColor = when {
        isSystemMessage -> Color(0xFF333333) // Dark gray text
        isAiMessage -> Color(0xFF2E7D32) // Dark green text for AI
        isUserMessage -> Color.White // White text on blue background
        else -> Color(0xFF333333) // Dark gray text for others
    }
    
    Column(modifier = modifier) {
        // Simplified message bubble
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(
                topStart = if (isUserMessage) 16.dp else 4.dp,
                topEnd = if (isUserMessage) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Message content - using simple text for now to ensure bubble visibility
                Column(modifier = Modifier.weight(1f)) {
                    // Show sender name if not user's own message and not system
                    if (!isUserMessage && !isSystemMessage) {
                        Text(
                            text = message.sender,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    
                    // Message content with markdown support for AI messages only
                    if (isAiMessage) {
                        // Extract just the AI response part (remove the emoji and model name prefix)
                        val aiContent = message.content.removePrefix("🤖 ").substringAfter(": ", message.content)
                        val cleanContent = aiContent.removeSuffix(" ▊").removeSuffix(" ✓").trim()
                        
                        if (cleanContent.isNotEmpty()) {
                            // Use CompositionLocalProvider to ensure proper text colors in markdown
                            CompositionLocalProvider(
                                LocalContentColor provides textColor
                            ) {
                                MarkdownText(
                                    text = cleanContent,
                                    modifier = Modifier,
                                    smallFontSize = false
                                )
                            }
                        } else {
                            // Fallback to regular text if cleaning fails
                            Text(
                                text = message.content,
                                color = textColor,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                softWrap = true
                            )
                        }
                    } else {
                        // Regular text for user messages and system messages (no markdown)
                        Text(
                            text = message.content,
                            color = textColor,
                            fontSize = if (isSystemMessage) 13.sp else 15.sp,
                            fontWeight = if (isSystemMessage) FontWeight.Normal else FontWeight.Normal,
                            lineHeight = 18.sp,
                            softWrap = true
                        )
                    }
                    
                    // Show timestamp
                    Text(
                        text = timeFormatter.format(message.timestamp),
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Delivery status for private messages (moved to right side)
                if (message.isPrivate && message.sender == currentUserNickname) {
                    message.deliveryStatus?.let { status ->
                        DeliveryStatusIcon(status = status, textColor = textColor)
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryStatusIcon(
    status: DeliveryStatus, 
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    when (status) {
        is DeliveryStatus.Sending -> {
            Text(
                text = "○",
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.6f)
            )
        }
        is DeliveryStatus.Sent -> {
            Text(
                text = "✓",
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.6f)
            )
        }
        is DeliveryStatus.Delivered -> {
            Text(
                text = "✓✓",
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
        is DeliveryStatus.Read -> {
            Text(
                text = "✓✓",
                fontSize = 10.sp,
                color = Color(0xFF007AFF), // Blue
                fontWeight = FontWeight.Bold
            )
        }
        is DeliveryStatus.Failed -> {
            Text(
                text = "⚠",
                fontSize = 10.sp,
                color = Color.Red.copy(alpha = 0.8f)
            )
        }
        is DeliveryStatus.PartiallyDelivered -> {
            Text(
                text = "✓${status.reached}/${status.total}",
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}
