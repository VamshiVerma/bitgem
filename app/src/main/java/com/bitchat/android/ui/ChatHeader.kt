package com.bitchat.android.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.core.ui.utils.singleOrTripleClickable

/**
 * Header components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */

/**
 * Reactive helper to compute favorite state from fingerprint mapping
 * This eliminates the need for static isFavorite parameters and makes
 * the UI reactive to fingerprint manager changes
 */
@Composable
fun isFavoriteReactive(
    peerID: String,
    peerFingerprints: Map<String, String>,
    favoritePeers: Set<String>
): Boolean {
    return remember(peerID, peerFingerprints, favoritePeers) {
        val fingerprint = peerFingerprints[peerID]
        fingerprint != null && favoritePeers.contains(fingerprint)
    }
}

@Composable
fun NoiseSessionIcon(
    sessionState: String?,
    modifier: Modifier = Modifier
) {
    val (icon, color, contentDescription) = when (sessionState) {
        "uninitialized" -> Triple(
            Icons.Outlined.NoEncryption,
            Color(0x87878700), // Grey - ready to establish
            "Ready for handshake"
        )
        "handshaking" -> Triple(
            Icons.Outlined.Sync,
            Color(0x87878700), // Grey - in progress
            "Handshake in progress"
        )
        "established" -> Triple(
            Icons.Filled.Lock,
            Color(0xFFFF9500), // Orange - secure
            "End-to-end encrypted"
        )
        else -> { // "failed" or any other state
            Triple(
                Icons.Outlined.Warning,
                Color(0xFFFF4444), // Red - error
                "Handshake failed"
            )
        }
    }
    
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = color
    )
}

@Composable
fun NicknameEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "@",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.primary
            )
            
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = colorScheme.onPrimaryContainer,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
    }
}

@Composable
fun PeerCounter(
    connectedPeers: List<String>,
    joinedChannels: Set<String>,
    hasUnreadChannels: Map<String, Int>,
    hasUnreadPrivateMessages: Set<String>,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.secondaryContainer.copy(alpha = 0.6f),
        border = BorderStroke(
            width = 1.dp, 
            color = if (isConnected) 
                Color(0xFF00C851).copy(alpha = 0.4f) 
            else 
                Color.Red.copy(alpha = 0.4f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Connection status with animated colors
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isConnected) Color(0xFF00C851) else Color.Red,
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Peer count with icon
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Connected peers",
                modifier = Modifier.size(16.dp),
                tint = colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${connectedPeers.size}",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            
            // Channel count if any
            if (joinedChannels.isNotEmpty()) {
                Text(
                    text = " · ",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = "Joined channels",
                    modifier = Modifier.size(14.dp),
                    tint = colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${joinedChannels.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Notification badges
            if (hasUnreadChannels.values.any { it > 0 }) {
                Spacer(modifier = Modifier.width(6.dp))
                Badge(
                    containerColor = Color(0xFF0080FF)
                ) {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (hasUnreadPrivateMessages.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Badge(
                    containerColor = Color(0xFFFF9500)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = "Unread messages",
                        modifier = Modifier.size(10.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatHeaderContent(
    selectedPrivatePeer: String?,
    currentChannel: String?,
    nickname: String,
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    onSidebarClick: () -> Unit,
    onTripleClick: () -> Unit,
    onShowAppInfo: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    when {
        selectedPrivatePeer != null -> {
            // Private chat header - Fully reactive state tracking
            val favoritePeers by viewModel.favoritePeers.observeAsState(emptySet())
            val peerFingerprints by viewModel.peerFingerprints.observeAsState(emptyMap())
            val peerSessionStates by viewModel.peerSessionStates.observeAsState(emptyMap())
            
            // Reactive favorite computation - no more static lookups!
            val isFavorite = isFavoriteReactive(
                peerID = selectedPrivatePeer,
                peerFingerprints = peerFingerprints,
                favoritePeers = favoritePeers
            )
            val sessionState = peerSessionStates[selectedPrivatePeer]
            
            Log.d("ChatHeader", "Header recomposing: peer=$selectedPrivatePeer, isFav=$isFavorite, sessionState=$sessionState")
            
            PrivateChatHeader(
                peerID = selectedPrivatePeer,
                peerNicknames = viewModel.meshService.getPeerNicknames(),
                isFavorite = isFavorite,
                sessionState = sessionState,
                onBackClick = onBackClick,
                onToggleFavorite = { viewModel.toggleFavorite(selectedPrivatePeer) }
            )
        }
        currentChannel != null -> {
            // Channel header
            ChannelHeader(
                channel = currentChannel,
                onBackClick = onBackClick,
                onLeaveChannel = { viewModel.leaveChannel(currentChannel) },
                onSidebarClick = onSidebarClick
            )
        }
        else -> {
            // Main header
            MainHeader(
                nickname = nickname,
                onNicknameChange = viewModel::setNickname,
                onTitleClick = onShowAppInfo,
                onTripleTitleClick = onTripleClick,
                onSidebarClick = onSidebarClick,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun PrivateChatHeader(
    peerID: String,
    peerNicknames: Map<String, String>,
    isFavorite: Boolean,
    sessionState: String?,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val peerNickname = peerNicknames[peerID] ?: peerID
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern back button
            Surface(
                onClick = onBackClick,
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.secondaryContainer.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Center - User info with modern styling
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Private chat",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFFF9500)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = peerNickname,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NoiseSessionIcon(
                        sessionState = sessionState,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Modern favorite button
            Surface(
                onClick = {
                    Log.d("ChatHeader", "Header toggle favorite: peerID=$peerID, currentFavorite=$isFavorite")
                    onToggleFavorite()
                },
                shape = CircleShape,
                color = if (isFavorite) 
                    Color(0xFFFFD700).copy(alpha = 0.2f) 
                else 
                    colorScheme.surfaceVariant,
                border = if (isFavorite)
                    BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                else
                    BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp),
                    tint = if (isFavorite) Color(0xFFFFD700) else colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChannelHeader(
    channel: String,
    onBackClick: () -> Unit,
    onLeaveChannel: () -> Unit,
    onSidebarClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern back button
            Surface(
                onClick = onBackClick,
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.secondaryContainer.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Center - Channel info with modern styling
            Surface(
                onClick = { onSidebarClick() },
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, Color(0xFF0080FF).copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = "Channel",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF0080FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = channel,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Modern leave button
            Surface(
                onClick = onLeaveChannel,
                shape = RoundedCornerShape(20.dp),
                color = Color.Red.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "Leave channel",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Leave",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MainHeader(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onTitleClick: () -> Unit,
    onTripleTitleClick: () -> Unit,
    onSidebarClick: () -> Unit,
    viewModel: ChatViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val connectedPeers by viewModel.connectedPeers.observeAsState(emptyList())
    val joinedChannels by viewModel.joinedChannels.observeAsState(emptySet())
    val hasUnreadChannels by viewModel.unreadChannelMessages.observeAsState(emptyMap())
    val hasUnreadPrivateMessages by viewModel.unreadPrivateMessages.observeAsState(emptySet())
    val isConnected by viewModel.isConnected.observeAsState(false)
    val aiModeEnabled by viewModel.aiModeEnabled.observeAsState(false)
    
    // Simplified header layout
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Simple app name and username
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = "BitGem/",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                modifier = Modifier.singleOrTripleClickable(
                    onSingleClick = onTitleClick,
                    onTripleClick = onTripleTitleClick
                )
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Simplified nickname editor
            Text(
                text = "@",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary.copy(alpha = 0.8f)
            )
            
            BasicTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                modifier = Modifier.widthIn(max = 100.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // AI mode toggle button
        Surface(
            onClick = { viewModel.toggleAiMode() },
            shape = CircleShape,
            color = if (aiModeEnabled) 
                Color(0xFF4CAF50).copy(alpha = 0.2f) 
            else 
                colorScheme.surfaceVariant,
            border = BorderStroke(
                1.dp, 
                if (aiModeEnabled) 
                    Color(0xFF4CAF50).copy(alpha = 0.5f)
                else 
                    colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy, // Robot icon for AI
                contentDescription = if (aiModeEnabled) "Disable AI mode" else "Enable AI mode",
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp),
                tint = if (aiModeEnabled) Color(0xFF4CAF50) else colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Right side - Simplified peer counter
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onSidebarClick() }
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Connected peers",
                modifier = Modifier.size(16.dp),
                tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${connectedPeers.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
