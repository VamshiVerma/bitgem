package com.bitchat.android.ui

import com.bitchat.android.model.BitchatMessage
// Bridge temporarily disabled - will re-enable after fixing compilation issues
import java.util.*
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles processing of IRC-style commands
 */
class CommandProcessor(
    private val state: ChatState,
    private val messageManager: MessageManager,
    private val channelManager: ChannelManager,
    private val privateChatManager: PrivateChatManager,
    private val context: Context
) {
    
    // AI command processor temporarily disabled - will re-enable after fixing compilation issues
    
    // Available commands list
    private val baseCommands = listOf(
        CommandSuggestion("/block", emptyList(), "[nickname]", "block or list blocked peers"),
        CommandSuggestion("/channels", emptyList(), null, "show all discovered channels"),
        CommandSuggestion("/clear", emptyList(), null, "clear chat messages"),
        CommandSuggestion("/hug", emptyList(), "<nickname>", "send someone a warm hug"),
        CommandSuggestion("/j", listOf("/join"), "<channel>", "join or create a channel"),
        CommandSuggestion("/m", listOf("/msg"), "<nickname> [message]", "send private message"),
        CommandSuggestion("/slap", emptyList(), "<nickname>", "slap someone with a trout"),
        CommandSuggestion("/unblock", emptyList(), "<nickname>", "unblock a peer"),
        CommandSuggestion("/w", emptyList(), null, "see who's online"),
        // Emergency Swarm Commands
        CommandSuggestion("/role", emptyList(), "<scout|medic|leader|helper|analyst>", "set your emergency role"),
        CommandSuggestion("/analyze", emptyList(), "[text/image]", "AI analysis of situation"),
        CommandSuggestion("/supply", emptyList(), "<item>", "broadcast supply need/availability"),
        CommandSuggestion("/status", emptyList(), null, "show swarm status and capabilities"),
        CommandSuggestion("/emergency", emptyList(), "<message>", "broadcast emergency alert")
    )
    
    // AI commands from Gallery integration
    private val aiCommands = listOf(
        CommandSuggestion("/ai", emptyList(), "[message]", "chat with AI assistant"),
        CommandSuggestion("/generate", emptyList(), "[description]", "generate an image"),
        CommandSuggestion("/classify", emptyList(), "[text]", "classify text content"),
        CommandSuggestion("/ai-help", emptyList(), null, "show AI command help"),
        CommandSuggestion("/ai-capabilities", emptyList(), null, "show AI capabilities"),
        CommandSuggestion("/ai-status", emptyList(), null, "check emergency AI readiness"),
        CommandSuggestion("/collaborate", emptyList(), "[topic] [prompt]", "start AI collaboration")
    )
    
    // MARK: - Command Processing
    
    fun processCommand(command: String, meshService: Any, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit): Boolean {
        if (!command.startsWith("/")) return false
        
        // AI command processing temporarily disabled
        
        val parts = command.split(" ")
        val cmd = parts.first()
        
        when (cmd) {
            "/j", "/join" -> handleJoinCommand(parts, myPeerID)
            "/m", "/msg" -> handleMessageCommand(parts, meshService)
            "/w" -> handleWhoCommand(meshService)
            "/clear" -> handleClearCommand()
            "/pass" -> handlePassCommand(parts, myPeerID)
            "/block" -> handleBlockCommand(parts, meshService)
            "/unblock" -> handleUnblockCommand(parts, meshService)
            "/hug" -> handleActionCommand(parts, "gives", "a warm hug 🫂", meshService, myPeerID, onSendMessage)
            "/slap" -> handleActionCommand(parts, "slaps", "around a bit with a large trout 🐟", meshService, myPeerID, onSendMessage)
            "/channels" -> handleChannelsCommand()
            // Emergency Swarm Commands
            "/role" -> handleRoleCommand(parts, myPeerID, onSendMessage)
            "/analyze" -> handleAnalyzeCommand(parts, myPeerID)
            "/supply" -> handleSupplyCommand(parts, myPeerID, onSendMessage)
            "/status" -> handleStatusCommand(meshService)
            "/emergency" -> handleEmergencyCommand(parts, myPeerID, onSendMessage)
            // AI Commands
            "/ai" -> handleAiCommand(parts, myPeerID, onSendMessage)
            "/generate" -> handleGenerateCommand(parts, myPeerID)
            "/classify" -> handleClassifyCommand(parts, myPeerID)
            "/ai-help" -> handleAiHelpCommand()
            "/ai-capabilities" -> handleAiCapabilitiesCommand()
            "/ai-status" -> handleAiStatusCommand()
            "/collaborate" -> handleCollaborateCommand(parts, myPeerID)
            else -> handleUnknownCommand(cmd)
        }
        
        return true
    }
    
    private fun handleJoinCommand(parts: List<String>, myPeerID: String) {
        if (parts.size > 1) {
            val channelName = parts[1]
            val channel = if (channelName.startsWith("#")) channelName else "#$channelName"
            val password = if (parts.size > 2) parts[2] else null
            val success = channelManager.joinChannel(channel, password, myPeerID)
            if (success) {
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "joined channel $channel",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
            }
        } else {
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "usage: /join <channel>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleMessageCommand(parts: List<String>, meshService: Any) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            val peerID = getPeerIDForNickname(targetName, meshService)
            
            if (peerID != null) {
                val success = privateChatManager.startPrivateChat(peerID, meshService)
                
                if (success) {
                    if (parts.size > 2) {
                        val messageContent = parts.drop(2).joinToString(" ")
                        val recipientNickname = getPeerNickname(peerID, meshService)
                        privateChatManager.sendPrivateMessage(
                            messageContent, 
                            peerID, 
                            recipientNickname,
                            state.getNicknameValue(),
                            getMyPeerID(meshService)
                        ) { content, peerIdParam, recipientNicknameParam, messageId ->
                            // This would trigger the actual mesh service send
                            sendPrivateMessageVia(meshService, content, peerIdParam, recipientNicknameParam, messageId)
                        }
                    } else {
                        val systemMessage = BitchatMessage(
                            sender = "system",
                            content = "started private chat with $targetName",
                            timestamp = Date(),
                            isRelay = false
                        )
                        messageManager.addMessage(systemMessage)
                    }
                }
            } else {
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "user '$targetName' not found. they may be offline or using a different nickname.",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
            }
        } else {
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "usage: /msg <nickname> [message]",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleWhoCommand(meshService: Any) {
        val connectedPeers = state.getConnectedPeersValue()
        val peerList = connectedPeers.joinToString(", ") { peerID ->
            // Convert peerID to nickname using the mesh service
            getPeerNickname(peerID, meshService)
        }
        
        val systemMessage = BitchatMessage(
            sender = "system",
            content = if (connectedPeers.isEmpty()) {
                "no one else is online right now."
            } else {
                "online users: $peerList"
            },
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    private fun handleClearCommand() {
        when {
            state.getSelectedPrivateChatPeerValue() != null -> {
                // Clear private chat
                val peerID = state.getSelectedPrivateChatPeerValue()!!
                messageManager.clearPrivateMessages(peerID)
            }
            state.getCurrentChannelValue() != null -> {
                // Clear channel messages
                val channel = state.getCurrentChannelValue()!!
                messageManager.clearChannelMessages(channel)
            }
            else -> {
                // Clear main messages
                messageManager.clearMessages()
            }
        }
    }

    private fun handlePassCommand(parts: List<String>, peerID: String) {
        val currentChannel = state.getCurrentChannelValue()

        if (currentChannel == null) {
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "you must be in a channel to set a password.",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
            return
        }

        if (parts.size == 2){
            if(!channelManager.isChannelCreator(channel = currentChannel, peerID = peerID)){
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "you must be the channel creator to set a password.",
                    timestamp = Date(),
                    isRelay = false
                )
                channelManager.addChannelMessage(currentChannel,systemMessage,null)
                return
            }
            val newPassword = parts[1]
            channelManager.setChannelPassword(currentChannel, newPassword)
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "password changed for channel $currentChannel",
                timestamp = Date(),
                isRelay = false
            )
            channelManager.addChannelMessage(currentChannel,systemMessage,null)
        }
        else{
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "usage: /pass <password>",
                timestamp = Date(),
                isRelay = false
            )
            channelManager.addChannelMessage(currentChannel,systemMessage,null)
        }
    }
    
    private fun handleBlockCommand(parts: List<String>, meshService: Any) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            privateChatManager.blockPeerByNickname(targetName, meshService)
        } else {
            // List blocked users
            val blockedInfo = privateChatManager.listBlockedUsers()
            val systemMessage = BitchatMessage(
                sender = "system",
                content = blockedInfo,
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleUnblockCommand(parts: List<String>, meshService: Any) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            privateChatManager.unblockPeerByNickname(targetName, meshService)
        } else {
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "usage: /unblock <nickname>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleActionCommand(
        parts: List<String>, 
        verb: String, 
        object_: String, 
        meshService: Any,
        myPeerID: String,
        onSendMessage: (String, List<String>, String?) -> Unit
    ) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            val actionMessage = "* ${state.getNicknameValue() ?: "someone"} $verb $targetName $object_ *"
            
            // Send as regular message
            if (state.getSelectedPrivateChatPeerValue() != null) {
                val peerID = state.getSelectedPrivateChatPeerValue()!!
                privateChatManager.sendPrivateMessage(
                    actionMessage,
                    peerID,
                    getPeerNickname(peerID, meshService),
                    state.getNicknameValue(),
                    myPeerID
                ) { content, peerIdParam, recipientNicknameParam, messageId ->
                    sendPrivateMessageVia(meshService, content, peerIdParam, recipientNicknameParam, messageId)
                }
            } else {
                val message = BitchatMessage(
                    sender = state.getNicknameValue() ?: myPeerID,
                    content = actionMessage,
                    timestamp = Date(),
                    isRelay = false,
                    senderPeerID = myPeerID,
                    channel = state.getCurrentChannelValue()
                )
                
                if (state.getCurrentChannelValue() != null) {
                    channelManager.addChannelMessage(state.getCurrentChannelValue()!!, message, myPeerID)
                    onSendMessage(actionMessage, emptyList(), state.getCurrentChannelValue())
                } else {
                    messageManager.addMessage(message)
                    onSendMessage(actionMessage, emptyList(), null)
                }
            }
        } else {
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "usage: /${parts[0].removePrefix("/")} <nickname>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleChannelsCommand() {
        val allChannels = channelManager.getJoinedChannelsList()
        val channelList = if (allChannels.isEmpty()) {
            "no channels joined"
        } else {
            "joined channels: ${allChannels.joinToString(", ")}"
        }
        
        val systemMessage = BitchatMessage(
            sender = "system",
            content = channelList,
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    private fun handleUnknownCommand(cmd: String) {
        val systemMessage = BitchatMessage(
            sender = "system",
            content = "unknown command: $cmd. type / to see available commands.",
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    // MARK: - Command Autocomplete
    
    fun updateCommandSuggestions(input: String) {
        if (!input.startsWith("/") || input.length < 1) {
            state.setShowCommandSuggestions(false)
            state.setCommandSuggestions(emptyList())
            return
        }
        
        // Get all available commands based on context
        val allCommands = getAllAvailableCommands()
        
        // Filter commands based on input
        val filteredCommands = filterCommands(allCommands, input.lowercase())
        
        if (filteredCommands.isNotEmpty()) {
            state.setCommandSuggestions(filteredCommands)
            state.setShowCommandSuggestions(true)
        } else {
            state.setShowCommandSuggestions(false)
            state.setCommandSuggestions(emptyList())
        }
    }
    
    private fun getAllAvailableCommands(): List<CommandSuggestion> {
        // Add channel-specific commands if in a channel
        val channelCommands = if (state.getCurrentChannelValue() != null) {
            listOf(
                CommandSuggestion("/pass", emptyList(), "[password]", "change channel password"),
                CommandSuggestion("/save", emptyList(), null, "save channel messages locally"),
                CommandSuggestion("/transfer", emptyList(), "<nickname>", "transfer channel ownership")
            )
        } else {
            emptyList()
        }
        
        return baseCommands + aiCommands + channelCommands
    }
    
    private fun filterCommands(commands: List<CommandSuggestion>, input: String): List<CommandSuggestion> {
        return commands.filter { command ->
            // Check primary command
            command.command.startsWith(input) ||
            // Check aliases
            command.aliases.any { it.startsWith(input) }
        }.sortedBy { it.command }
    }
    
    fun selectCommandSuggestion(suggestion: CommandSuggestion): String {
        state.setShowCommandSuggestions(false)
        state.setCommandSuggestions(emptyList())
        return "${suggestion.command} "
    }
    
    // MARK: - Utility Functions (would access mesh service)
    
    private fun getPeerIDForNickname(nickname: String, meshService: Any): String? {
        return try {
            val method = meshService::class.java.getDeclaredMethod("getPeerNicknames")
            val peerNicknames = method.invoke(meshService) as? Map<String, String>
            peerNicknames?.entries?.find { it.value == nickname }?.key
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getPeerNickname(peerID: String, meshService: Any): String {
        return try {
            val method = meshService::class.java.getDeclaredMethod("getPeerNicknames")
            val peerNicknames = method.invoke(meshService) as? Map<String, String>
            peerNicknames?.get(peerID) ?: peerID
        } catch (e: Exception) {
            peerID
        }
    }
    
    private fun getMyPeerID(meshService: Any): String {
        return try {
            val field = meshService::class.java.getDeclaredField("myPeerID")
            field.isAccessible = true
            field.get(meshService) as? String ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun sendPrivateMessageVia(meshService: Any, content: String, peerID: String, recipientNickname: String, messageId: String) {
        try {
            val method = meshService::class.java.getDeclaredMethod(
                "sendPrivateMessage", 
                String::class.java, 
                String::class.java, 
                String::class.java, 
                String::class.java
            )
            method.invoke(meshService, content, peerID, recipientNickname, messageId)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // MARK: - Emergency Swarm Command Handlers
    
    private fun handleRoleCommand(parts: List<String>, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit) {
        if (parts.size > 1) {
            val role = parts[1].lowercase()
            val validRoles = mapOf(
                "scout" to "🔍",
                "medic" to "🏥",
                "leader" to "⭐",
                "helper" to "🔧",
                "analyst" to "📊"
            )
            
            if (validRoles.containsKey(role)) {
                val emoji = validRoles[role]
                // Store role locally in state (persistent storage comes next)
                state.setSwarmRole(role)
                
                // Broadcast role to mesh network with special protocol
                val roleMessage = "🔄 ROLE_UPDATE:$myPeerID:$role:${System.currentTimeMillis()}"
                onSendMessage(roleMessage, emptyList(), null)
                
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "$emoji Your role is now: ${role.uppercase()}. Broadcasted to swarm network.",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
                
                // Auto-join role-specific channel
                val roleChannel = "#${role}-network"
                channelManager.joinChannel(roleChannel, null, myPeerID)
                
            } else {
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "Invalid role. Use: scout, medic, leader, helper, analyst",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
            }
        } else {
            val currentRole = state.getSwarmRoleValue()
            val systemMessage = BitchatMessage(
                sender = "system",
                content = if (currentRole.isNotEmpty()) "Current role: $currentRole" else "Usage: /role <scout|medic|leader|helper|analyst>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleAnalyzeCommand(parts: List<String>, myPeerID: String) {
        if (parts.size > 1) {
            val input = parts.drop(1).joinToString(" ")
            val role = state.getSwarmRoleValue()
            
            // Enhance prompt based on role
            val contextPrompt = when (role) {
                "medic" -> "As a field medic, analyze this medical situation: $input"
                "scout" -> "As a reconnaissance scout, analyze this terrain/hazard: $input"
                "leader" -> "As incident commander, provide tactical analysis: $input"
                "analyst" -> "As data analyst, provide detailed technical analysis: $input"
                else -> "Analyze this emergency situation: $input"
            }
            
            // AI analysis temporarily disabled - will re-enable after fixing compilation issues
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "🤖 AI analysis temporarily unavailable. Working to restore functionality.",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
            
        } else {
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "Usage: /analyze <situation description>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleSupplyCommand(parts: List<String>, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit) {
        if (parts.size > 1) {
            val item = parts.drop(1).joinToString(" ")
            val role = state.getSwarmRoleValue()
            val roleIcon = when (role) {
                "medic" -> "🏥"
                "scout" -> "🔍"
                "leader" -> "⭐"
                "helper" -> "🔧"
                "analyst" -> "📊"
                else -> "📦"
            }
            
            val supplyMessage = "$roleIcon SUPPLY_REQUEST:$myPeerID:$item:${System.currentTimeMillis()}"
            onSendMessage(supplyMessage, emptyList(), null)
            
            // Also broadcast to supply channel
            onSendMessage(supplyMessage, emptyList(), "#supply-network")
            
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "📦 Supply request '$item' broadcast to swarm network",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
            
        } else {
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "Usage: /supply <item needed>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleStatusCommand(meshService: Any) {
        val connectedPeers = state.getConnectedPeersValue()
        val myRole = state.getSwarmRoleValue()
        val aiCapable = try {
            // Check if AI is available
            true // Simplified for now
        } catch (e: Exception) {
            false
        }
        
        val statusBuilder = StringBuilder()
        statusBuilder.appendLine("🔥 SWARM STATUS REPORT")
        statusBuilder.appendLine("━━━━━━━━━━━━━━━━━━━━")
        statusBuilder.appendLine("👤 Your Role: ${myRole.uppercase().ifEmpty { "UNASSIGNED" }}")
        statusBuilder.appendLine("🤖 AI Capable: ${if (aiCapable) "✅ YES" else "❌ NO"}")
        statusBuilder.appendLine("📡 Connected Peers: ${connectedPeers.size}")
        statusBuilder.appendLine("🔗 Network Status: ${if (connectedPeers.isNotEmpty()) "ACTIVE" else "OFFLINE"}")
        statusBuilder.appendLine("━━━━━━━━━━━━━━━━━━━━")
        
        val systemMessage = BitchatMessage(
            sender = "system",
            content = statusBuilder.toString().trim(),
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    private fun handleEmergencyCommand(parts: List<String>, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit) {
        if (parts.size > 1) {
            val message = parts.drop(1).joinToString(" ")
            val role = state.getSwarmRoleValue()
            val roleIcon = when (role) {
                "medic" -> "🏥"
                "scout" -> "🔍"
                "leader" -> "⭐"
                "helper" -> "🔧"
                "analyst" -> "📊"
                else -> "🚨"
            }
            
            val emergencyMessage = "🚨 EMERGENCY:$myPeerID:$role:$message:${System.currentTimeMillis()}"
            onSendMessage(emergencyMessage, emptyList(), null)
            
            // Also broadcast to emergency channel
            onSendMessage("$roleIcon EMERGENCY ALERT: $message", emptyList(), "#emergency-network")
            
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "🚨 EMERGENCY ALERT broadcasted to all connected devices",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
            
        } else {
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "Usage: /emergency <urgent message>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    // MARK: - AI Command Handlers
    
    private fun handleAiCommand(parts: List<String>, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit) {
        if (parts.size > 1) {
            val prompt = parts.drop(1).joinToString(" ")
            performAiChat(prompt, myPeerID, onSendMessage)
        } else {
            val systemMessage = BitchatMessage(
                sender = "system", 
                content = "Usage: /ai <message>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    // Public method for emergency alert to use the same AI streaming functionality
    fun performEmergencyAiAnalysis(prompt: String, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit) {
        performAiChatSilent(prompt, myPeerID, onSendMessage)
    }
    
    // Silent AI chat that doesn't show the user's question in chat
    private fun performAiChatSilent(prompt: String, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit) {
        try {
            // Skip showing user's question - go directly to AI processing
            
            // Get available LLM models from TASK_LLM_CHAT
            val chatModels = com.google.ai.edge.gallery.data.TASK_LLM_CHAT.models
            
            if (chatModels.isEmpty()) {
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "❌ ⚠️  CRITICAL: Emergency AI system unavailable!\n\n🚨 NO AI MODELS CONFIGURED\n\n💡 IMMEDIATE ACTION REQUIRED:\n1. Go to Gallery tab → AI Chat\n2. Download any chat model (recommended: Gemma)\n3. Emergency AI will auto-activate\n\n⚠️ Emergency functions disabled until model installed!",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
                
                // Critical broadcast - emergency AI unavailable
                onSendMessage("🚨 CRITICAL: Emergency AI unavailable - no models configured on this device", emptyList(), state.getCurrentChannelValue())
                return
            }
            
            // Find a downloaded model
            val downloadedModel = chatModels.find { model ->
                val modelPath = model.getPath(context)
                java.io.File(modelPath).exists()
            }
            
            if (downloadedModel == null) {
                val modelNames = chatModels.map { it.name }.joinToString(", ")
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "📥 ⚠️  EMERGENCY AI UNAVAILABLE: No chat models downloaded!\n\n💡 Quick Fix:\n1. Go to Gallery tab → AI Chat → Download Model\n2. Or import a model file in Gallery tab → Model Manager\n3. Return to try emergency analysis again\n\nAvailable models: $modelNames",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
                
                // Broadcast to network so others know emergency AI is unavailable
                onSendMessage("⚠️ Emergency AI unavailable - no models downloaded on this device", emptyList(), state.getCurrentChannelValue())
                return
            }
            
            // Check if model instance is initialized (same as regular AI chat)
            if (downloadedModel.instance == null) {
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "🔄 AI model not initialized. Initializing now...",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
                
                // Initialize the model first
                com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper.initialize(
                    context = context,
                    model = downloadedModel,
                    onDone = { error ->
                        if (error.isEmpty() && downloadedModel.instance != null) {
                            performAiInference(downloadedModel, prompt, myPeerID, onSendMessage)
                        } else {
                            val errorMessage = BitchatMessage(
                                sender = "system",
                                content = "❌ Emergency AI model initialization failed: ${error.ifEmpty { "Unknown error" }}\n\n💡 Try:\n1. Restart the app\n2. Go to Gallery → AI Chat to test model\n3. Re-download model if corrupted\n4. Check device storage space",
                                timestamp = Date(),
                                isRelay = false
                            )
                            messageManager.addMessage(errorMessage)
                            
                            // Broadcast initialization failure
                            onSendMessage("⚠️ Emergency AI initialization failed on this device", emptyList(), state.getCurrentChannelValue())
                        }
                    }
                )
            } else {
                // Model already initialized, proceed directly
                performAiInference(downloadedModel, prompt, myPeerID, onSendMessage)
            }
            
        } catch (e: Exception) {
            val systemMessage = BitchatMessage(
                sender = "system", 
                content = "❌ AI analysis failed: ${e.message}",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun performAiInference(model: com.google.ai.edge.gallery.data.Model, prompt: String, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit) {
        // Initialize model if needed and perform inference
        // This is the core AI processing logic extracted from performAiChat
        
        val myNickname = state.getNicknameValue() ?: "AI Assistant"
        
        // Create initial AI response message for streaming
        val aiMessage = BitchatMessage(
            sender = "🤖 AI Assistant",
            content = "🤖 AI is analyzing...",
            timestamp = Date(),
            isRelay = false,
            id = "ai_response_${System.currentTimeMillis()}"
        )
        messageManager.addMessage(aiMessage)
        
        var accumulatedResponse = ""
        
        try {
            // Add instruction to keep emergency analysis concise
            val instructedPrompt = "Please provide emergency analysis in 300 words or less. Be concise and direct.\n\n$prompt"
            
            com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper.runInference(
                model = model,
                input = instructedPrompt,
                resultListener = { partialResult: String, done: Boolean ->
                    accumulatedResponse += partialResult
                    val streamingContent = "🤖 AI Assistant: $accumulatedResponse${if (!done) " ▊" else ""}"
                    
                    // Switch to main thread for UI updates
                    CoroutineScope(Dispatchers.Main).launch {
                        messageManager.updateMessage(aiMessage.id, streamingContent)
                        
                        if (done && accumulatedResponse.isNotEmpty()) {
                            // Broadcast final response to mesh network
                            onSendMessage("🤖 Emergency Analysis Complete: $accumulatedResponse", emptyList(), state.getCurrentChannelValue())
                        }
                    }
                },
                cleanUpListener = {
                    // Cleanup completed
                }
            )
        } catch (e: Exception) {
            val errorMessage = BitchatMessage(
                sender = "system",
                content = "❌ AI inference failed: ${e.message}",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(errorMessage)
        }
    }
    
    private fun performAiChat(prompt: String, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit) {
        try {
            // First, show the user's question in chat
            val myNickname = state.getNicknameValue() ?: "User"
            val userMessage = BitchatMessage(
                sender = myNickname,
                content = prompt,
                timestamp = Date(),
                isRelay = false,
                senderPeerID = myPeerID
            )
            messageManager.addMessage(userMessage)
            
            // Broadcast to mesh network that AI is processing
            onSendMessage("🤖 $myNickname is asking AI: \"$prompt\"", emptyList(), state.getCurrentChannelValue())
            
            // Get available LLM models from TASK_LLM_CHAT
            val chatModels = com.google.ai.edge.gallery.data.TASK_LLM_CHAT.models
            
            if (chatModels.isEmpty()) {
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "❌ No AI chat models available. Please download a chat model in Gallery tab first.",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
                return
            }
            
            // Find a downloaded model
            val downloadedModel = chatModels.find { model ->
                val modelPath = model.getPath(context)
                java.io.File(modelPath).exists()
            }
            
            if (downloadedModel == null) {
                val modelNames = chatModels.map { it.name }.joinToString(", ")
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "📥 Chat models not downloaded: $modelNames\n\nGo to Gallery tab → Download a chat model → Return to try /ai again.",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
                return
            }
            
            // Check if model instance is initialized
            if (downloadedModel.instance == null) {
                val systemMessage = BitchatMessage(
                    sender = "system",
                    content = "🔄 AI model not initialized. Initializing now...",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
                
                // Try to initialize the model
                try {
                    val modelHelper = com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
                    modelHelper.initialize(
                        context = context,
                        model = downloadedModel,
                        onDone = { error ->
                            if (error.isEmpty() && downloadedModel.instance != null) {
                                // Model initialized successfully, retry AI inference
                                CoroutineScope(Dispatchers.Main).launch {
                                    val retryMessage = BitchatMessage(
                                        sender = "system",
                                        content = "✅ Model ready! Processing your request...",
                                        timestamp = Date(),
                                        isRelay = false
                                    )
                                    messageManager.addMessage(retryMessage)
                                    
                                    // Retry the AI processing after a short delay
                                    CoroutineScope(Dispatchers.IO).launch {
                                        kotlinx.coroutines.delay(500)
                                        try {
                                            performAiInferenceWithModel(downloadedModel, prompt, myPeerID, onSendMessage)
                                        } catch (e: Exception) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                val errorMessage = BitchatMessage(
                                                    sender = "system",
                                                    content = "❌ AI inference failed after initialization: ${e.message}",
                                                    timestamp = Date(),
                                                    isRelay = false
                                                )
                                                messageManager.addMessage(errorMessage)
                                            }
                                        }
                                    }
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val errorMessage = BitchatMessage(
                                        sender = "system",
                                        content = "❌ Model initialization failed: ${error.ifEmpty { "Unknown error" }}",
                                        timestamp = Date(),
                                        isRelay = false
                                    )
                                    messageManager.addMessage(errorMessage)
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    val errorMessage = BitchatMessage(
                        sender = "system",
                        content = "❌ Model initialization error: ${e.message}",
                        timestamp = Date(),
                        isRelay = false
                    )
                    messageManager.addMessage(errorMessage)
                }
                return
            }
            
            // Create initial AI response message for streaming
            val aiMessage = BitchatMessage(
                sender = "🤖 AI Assistant",
                content = "🤖 AI is thinking...",
                timestamp = Date(),
                isRelay = false,
                id = "ai_response_${System.currentTimeMillis()}"
            )
            messageManager.addMessage(aiMessage)
            
            // Perform AI inference using LlmChatModelHelper with streaming
            performAiInferenceWithModel(downloadedModel, prompt, myPeerID, onSendMessage)
            
        } catch (e: Exception) {
            val errorMessage = BitchatMessage(
                sender = "system",
                content = "❌ AI chat error: ${e.message}",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(errorMessage)
        }
    }
    
    private fun performAiInferenceWithModel(model: com.google.ai.edge.gallery.data.Model, prompt: String, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var accumulatedResponse = ""
                
                // Create initial AI response message for streaming
                val aiMessage = BitchatMessage(
                    sender = "🤖 AI Assistant",
                    content = "🤖 AI is thinking...",
                    timestamp = Date(),
                    isRelay = false,
                    id = "ai_response_${System.currentTimeMillis()}"
                )
                
                CoroutineScope(Dispatchers.Main).launch {
                    messageManager.addMessage(aiMessage)
                }
                
                // Add instruction to keep responses concise
                val instructedPrompt = "Please provide a helpful response in 300 words or less. Be concise and direct.\n\n$prompt"
                
                com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper.runInference(
                    model = model,
                    input = instructedPrompt,
                    resultListener = { partialResult: String, done: Boolean ->
                        try {
                            // Update accumulated response like Gallery does
                            accumulatedResponse = "$accumulatedResponse$partialResult"
                            
                            // Update the message with streaming content on UI thread
                            CoroutineScope(Dispatchers.Main).launch {
                                if (accumulatedResponse.isNotEmpty()) {
                                    val streamingContent = "🤖 AI Assistant: $accumulatedResponse${if (!done) " ▊" else ""}"
                                    messageManager.updateMessage(aiMessage.id, streamingContent)
                                }
                                
                                // When done, broadcast final response to mesh network
                                if (done && accumulatedResponse.trim().isNotEmpty()) {
                                    val finalContent = "🤖 AI Assistant: ${accumulatedResponse.trim()}"
                                    messageManager.updateMessage(aiMessage.id, finalContent)
                                    
                                    // Broadcast AI response to mesh network as if sent by user
                                    val broadcastMessage = "🤖 ${accumulatedResponse.trim()}"
                                    onSendMessage(broadcastMessage, emptyList(), state.getCurrentChannelValue())
                                }
                            }
                        } catch (e: Exception) {
                            CoroutineScope(Dispatchers.Main).launch {
                                val errorMessage = BitchatMessage(
                                    sender = "system",
                                    content = "❌ Result processing failed: ${e.message}",
                                    timestamp = Date(),
                                    isRelay = false
                                )
                                messageManager.addMessage(errorMessage)
                            }
                        }
                    },
                    cleanUpListener = {
                        // Cleanup completed
                    }
                )
                
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    val errorMessage = BitchatMessage(
                        sender = "system",
                        content = "❌ AI inference failed: ${e.message}",
                        timestamp = Date(),
                        isRelay = false
                    )
                    messageManager.addMessage(errorMessage)
                }
            }
        }
    }
    
    private fun handleGenerateCommand(parts: List<String>, myPeerID: String) {
        val systemMessage = BitchatMessage(
            sender = "system",
            content = "🎨 Image generation temporarily unavailable. Go to Gallery tab → Image Generation for this feature.",
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    private fun handleClassifyCommand(parts: List<String>, myPeerID: String) {
        val systemMessage = BitchatMessage(
            sender = "system",
            content = "📊 Text classification temporarily unavailable. Go to Gallery tab → Text Classification for this feature.",
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    private fun handleAiHelpCommand() {
        val helpText = """
            🤖 AI COMMANDS HELP
            ━━━━━━━━━━━━━━━━━━━━
            /ai <message> - Chat with AI (temporarily unavailable)
            /generate <description> - Generate image (use Gallery tab)
            /classify <text> - Classify text (use Gallery tab)
            /analyze <text/image> - AI analysis in emergency context
            /collaborate <topic> <prompt> - Start AI collaboration
            /ai-capabilities - Show AI capabilities
            ━━━━━━━━━━━━━━━━━━━━
            💡 For full AI features, use Gallery tab or SwarmScreen AI Analysis
        """.trimIndent()
        
        val systemMessage = BitchatMessage(
            sender = "system",
            content = helpText,
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    private fun handleAiCapabilitiesCommand() {
        val capabilitiesText = """
            🚀 AI CAPABILITIES
            ━━━━━━━━━━━━━━━━━━━━
            ✅ Text Analysis (SwarmScreen)
            ✅ Image Analysis (SwarmScreen) 
            ✅ Emergency Context AI (SwarmScreen)
            ✅ Role-based AI Responses (SwarmScreen)
            ⚠️ Direct Chat (temporarily unavailable)
            ⚠️ Image Generation (use Gallery tab)
            ⚠️ Text Classification (use Gallery tab)
            ━━━━━━━━━━━━━━━━━━━━
            🔥 SwarmScreen provides full AI analysis for emergency coordination
        """.trimIndent()
        
        val systemMessage = BitchatMessage(
            sender = "system",
            content = capabilitiesText,
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    private fun handleCollaborateCommand(parts: List<String>, myPeerID: String) {
        if (parts.size > 2) {
            val topic = parts[1]
            val prompt = parts.drop(2).joinToString(" ")
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "🤝 AI Collaboration for '$topic' temporarily unavailable. Use SwarmScreen AI Analysis with role-specific prompts instead.",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        } else {
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "Usage: /collaborate <topic> <prompt>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleAiStatusCommand() {
        val statusMessage = checkEmergencyAiReadiness()
        
        val detailedStatus = """
            🤖 EMERGENCY AI STATUS CHECK
            ━━━━━━━━━━━━━━━━━━━━━━━━━
            
            $statusMessage
            
            📋 QUICK ACTIONS:
            • Gallery → AI Chat → Download Model
            • Gallery → Model Manager → Import Model  
            • /ai-help - See AI commands
            • /ai-capabilities - View AI features
        """.trimIndent()
        
        val systemMessage = BitchatMessage(
            sender = "system",
            content = detailedStatus,
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    // Helper function to check emergency AI readiness and notify user
    fun checkEmergencyAiReadiness(): String {
        val chatModels = com.google.ai.edge.gallery.data.TASK_LLM_CHAT.models
        
        return when {
            chatModels.isEmpty() -> "🚨 CRITICAL: No AI models configured! Emergency AI disabled."
            
            chatModels.none { model -> 
                val modelPath = model.getPath(context)
                java.io.File(modelPath).exists()
            } -> {
                val modelNames = chatModels.map { it.name }.joinToString(", ")
                "⚠️ WARNING: AI models not downloaded ($modelNames). Emergency AI unavailable."
            }
            
            chatModels.any { model -> 
                val modelPath = model.getPath(context)
                java.io.File(modelPath).exists() && model.instance != null
            } -> "✅ Emergency AI ready and initialized."
            
            chatModels.any { model -> 
                val modelPath = model.getPath(context)
                java.io.File(modelPath).exists() && model.instance == null
            } -> "⚡ Emergency AI available but not initialized (will auto-init on first use)."
            
            else -> "⚠️ Emergency AI status unknown."
        }
    }
}
