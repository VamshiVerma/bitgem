package com.bitchat.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.model.DeliveryStatus
import com.bitchat.android.mesh.BluetoothMeshService
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main ChatScreen - REFACTORED to use component-based architecture
 * This is now a coordinator that orchestrates the following UI components:
 * - ChatHeader: App bar, navigation, peer counter
 * - MessageComponents: Message display and formatting
 * - InputComponents: Message input and command suggestions
 * - SidebarComponents: Navigation drawer with channels and people
 * - DialogComponents: Password prompts and modals
 * - ChatUIUtils: Utility functions for formatting and colors
 */
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val messages by viewModel.messages.observeAsState(emptyList())
    val connectedPeers by viewModel.connectedPeers.observeAsState(emptyList())
    val nickname by viewModel.nickname.observeAsState("")
    val selectedPrivatePeer by viewModel.selectedPrivateChatPeer.observeAsState()
    val currentChannel by viewModel.currentChannel.observeAsState()
    val joinedChannels by viewModel.joinedChannels.observeAsState(emptySet())
    val hasUnreadChannels by viewModel.unreadChannelMessages.observeAsState(emptyMap())
    val hasUnreadPrivateMessages by viewModel.unreadPrivateMessages.observeAsState(emptySet())
    val privateChats by viewModel.privateChats.observeAsState(emptyMap())
    val channelMessages by viewModel.channelMessages.observeAsState(emptyMap())
    val showSidebar by viewModel.showSidebar.observeAsState(false)
    val showCommandSuggestions by viewModel.showCommandSuggestions.observeAsState(false)
    val commandSuggestions by viewModel.commandSuggestions.observeAsState(emptyList())
    val showAppInfo by viewModel.showAppInfo.observeAsState(false)
    
    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    
    // Show password dialog when needed
    LaunchedEffect(showPasswordPrompt) {
        showPasswordDialog = showPasswordPrompt
    }
    
    val isConnected by viewModel.isConnected.observeAsState(false)
    val passwordPromptChannel by viewModel.passwordPromptChannel.observeAsState(null)
    
    // Determine what messages to show
    val displayMessages = when {
        selectedPrivatePeer != null -> privateChats[selectedPrivatePeer] ?: emptyList()
        currentChannel != null -> channelMessages[currentChannel] ?: emptyList()
        else -> messages
    }
    
    val aiModeEnabled by viewModel.aiModeEnabled.observeAsState(false)
    
    // Simple Column layout - no complex Scaffold nonsense
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Header at top - fixed size
        ChatFloatingHeader(
            headerHeight = 60.dp,
            selectedPrivatePeer = selectedPrivatePeer,
            currentChannel = currentChannel,
            nickname = nickname,
            viewModel = viewModel,
            colorScheme = colorScheme,
            onSidebarToggle = { viewModel.showSidebar() },
            onShowAppInfo = { viewModel.showAppInfo() },
            onPanicClear = { viewModel.panicClearAllData() }
        )
        
        // Messages area - fills remaining space
        MessagesList(
            messages = displayMessages,
            currentUserNickname = nickname,
            meshService = viewModel.meshService,
            modifier = Modifier.weight(1f)
        )
        
        // Input section at bottom - includes command suggestions above input
        ChatInputSection(
            messageText = messageText,
            onMessageTextChange = { newText: TextFieldValue ->
                messageText = newText
                viewModel.updateCommandSuggestions(newText.text)
            },
            onSend = {
                if (messageText.text.trim().isNotEmpty()) {
                    val content = messageText.text.trim()
                    
                    // Simple AI mode - just add /ai prefix if enabled and not already a command
                    val finalMessage = if (aiModeEnabled && !content.startsWith("/")) {
                        "/ai $content"
                    } else {
                        content
                    }
                    
                    viewModel.sendMessage(finalMessage)
                    messageText = TextFieldValue("")
                }
            },
            showCommandSuggestions = showCommandSuggestions,
            commandSuggestions = commandSuggestions,
            onSuggestionClick = { suggestion: CommandSuggestion ->
                val commandText = viewModel.selectCommandSuggestion(suggestion)
                messageText = TextFieldValue(
                    text = commandText,
                    selection = TextRange(commandText.length)
                )
            },
            selectedPrivatePeer = selectedPrivatePeer,
            currentChannel = currentChannel,
            nickname = nickname,
            colorScheme = colorScheme
        )
    }
    
    // Sidebar overlay
    if (showSidebar) {
        SidebarOverlay(
            viewModel = viewModel,
            onDismiss = { viewModel.hideSidebar() },
            modifier = Modifier.fillMaxSize()
        )
    }
    
    // Dialogs
    ChatDialogs(
        showPasswordDialog = showPasswordDialog,
        passwordPromptChannel = passwordPromptChannel,
        passwordInput = passwordInput,
        onPasswordChange = { passwordInput = it },
        onPasswordConfirm = {
            if (passwordInput.isNotEmpty()) {
                val success = viewModel.joinChannel(passwordPromptChannel!!, passwordInput)
                if (success) {
                    showPasswordDialog = false
                    passwordInput = ""
                }
            }
        },
        onPasswordDismiss = {
            showPasswordDialog = false
            passwordInput = ""
        },
        showAppInfo = showAppInfo,
        onAppInfoDismiss = { viewModel.hideAppInfo() }
    )
}

@Composable
private fun ChatInputSection(
    messageText: TextFieldValue,
    onMessageTextChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    showCommandSuggestions: Boolean,
    commandSuggestions: List<CommandSuggestion>,
    onSuggestionClick: (CommandSuggestion) -> Unit,
    selectedPrivatePeer: String?,
    currentChannel: String?,
    nickname: String,
    colorScheme: ColorScheme
) {
    // Column with suggestions above input
    Column(modifier = Modifier.fillMaxWidth()) {
        // Command suggestions - appear above textbox when typing /
        if (showCommandSuggestions && commandSuggestions.isNotEmpty()) {
            CommandSuggestionsBox(
                suggestions = commandSuggestions,
                onSuggestionClick = onSuggestionClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Text input always at bottom
        MessageInput(
            value = messageText,
            onValueChange = onMessageTextChange,
            onSend = onSend,
            selectedPrivatePeer = selectedPrivatePeer,
            currentChannel = currentChannel,
            nickname = nickname,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChatFloatingHeader(
    headerHeight: Dp,
    selectedPrivatePeer: String?,
    currentChannel: String?,
    nickname: String,
    viewModel: ChatViewModel,
    colorScheme: ColorScheme,
    onSidebarToggle: () -> Unit,
    onShowAppInfo: () -> Unit,
    onPanicClear: () -> Unit
) {
    // Simple header bar
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight),
        color = colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        ChatHeaderContent(
            selectedPrivatePeer = selectedPrivatePeer,
            currentChannel = currentChannel,
            nickname = nickname,
            viewModel = viewModel,
            onBackClick = {
                when {
                    selectedPrivatePeer != null -> viewModel.endPrivateChat()
                    currentChannel != null -> viewModel.switchToChannel(null)
                }
            },
            onSidebarClick = onSidebarToggle,
            onTripleClick = onPanicClear,
            onShowAppInfo = onShowAppInfo
        )
    }
}

@Composable
private fun ChatDialogs(
    showPasswordDialog: Boolean,
    passwordPromptChannel: String?,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirm: () -> Unit,
    onPasswordDismiss: () -> Unit,
    showAppInfo: Boolean,
    onAppInfoDismiss: () -> Unit
) {
    // Password dialog
    PasswordPromptDialog(
        show = showPasswordDialog,
        channelName = passwordPromptChannel,
        passwordInput = passwordInput,
        onPasswordChange = onPasswordChange,
        onConfirm = onPasswordConfirm,
        onDismiss = onPasswordDismiss
    )
    
    // App info dialog
    AppInfoDialog(
        show = showAppInfo,
        onDismiss = onAppInfoDismiss
    )
}
