package com.bitchat.android.ui

import androidx.lifecycle.LifecycleCoroutineScope
import com.bitchat.android.mesh.BluetoothMeshDelegate
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.model.DeliveryAck
import com.bitchat.android.model.DeliveryStatus
import com.bitchat.android.model.ReadReceipt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Handles all BluetoothMeshDelegate callbacks and routes them to appropriate managers
 */
class MeshDelegateHandler(
    private val state: ChatState,
    private val messageManager: MessageManager,
    private val channelManager: ChannelManager,
    private val privateChatManager: PrivateChatManager,
    private val notificationManager: NotificationManager,
    private val coroutineScope: CoroutineScope,
    private val onHapticFeedback: () -> Unit,
    private val getMyPeerID: () -> String,
    private val getMeshService: () -> Any,
    private val onTriggerAiResponse: ((String) -> Unit)? = null
) : BluetoothMeshDelegate {

    override fun didReceiveMessage(message: BitchatMessage) {
        coroutineScope.launch {
            // FIXED: Deduplicate messages from dual connection paths
            val messageKey = messageManager.generateMessageKey(message)
            if (messageManager.isMessageProcessed(messageKey)) {
                return@launch // Duplicate message, ignore
            }
            messageManager.markMessageProcessed(messageKey)
            
            // Check if sender is blocked
            message.senderPeerID?.let { senderPeerID ->
                if (privateChatManager.isPeerBlocked(senderPeerID)) {
                    return@launch
                }
            }
            
            // Trigger haptic feedback
            onHapticFeedback()

            if (message.isPrivate) {
                // Private message
                privateChatManager.handleIncomingPrivateMessage(message)
                
                // Reactive read receipts: Send immediately if user is currently viewing this chat
                message.senderPeerID?.let { senderPeerID ->
                    sendReadReceiptIfFocused(senderPeerID)
                }
                
                // Show notification with enhanced information - now includes senderPeerID 
                message.senderPeerID?.let { senderPeerID ->
                    // Use nickname if available, fall back to sender or senderPeerID
                    val senderNickname = message.sender.takeIf { it != senderPeerID } ?: senderPeerID
                    notificationManager.showPrivateMessageNotification(
                        senderPeerID = senderPeerID, 
                        senderNickname = senderNickname, 
                        messageContent = message.content
                    )
                }
            } else if (message.channel != null) {
                // Channel message
                if (state.getJoinedChannelsValue().contains(message.channel)) {
                    channelManager.addChannelMessage(message.channel, message, message.senderPeerID)
                }
            } else {
                // Public message
                messageManager.addMessage(message)
                
                // Handle special swarm protocol messages
                handleSwarmProtocolMessage(message)
            }
            
            // AI auto-response: If AI mode is enabled and this is not from me and not an AI message
            // Skip if message contains frustration indicators to prevent AI feedback loops
            val isFrustratedMessage = message.content.contains("wtf", ignoreCase = true) || 
                                    message.content.contains("timeout", ignoreCase = true) ||
                                    message.content.contains("kidding", ignoreCase = true)
            
            if (state.getAiModeEnabledValue() && 
                message.senderPeerID != getMyPeerID() && 
                !message.content.startsWith("🤖") &&
                !message.content.startsWith("/ai") &&
                message.sender != "system" &&
                !message.isPrivate &&
                !isFrustratedMessage) { // Skip frustrated messages to avoid loops
                
                // Use a race condition prevention mechanism  
                val messageKey = "ai_claim_${message.id}_${System.currentTimeMillis()/5000}" // 5 second window
                if (!messageManager.isMessageProcessed(messageKey)) {
                    messageManager.markMessageProcessed(messageKey)
                    
                    // IMMEDIATELY notify all users that AI is processing (critical for UX)
                    val meshService = getMeshService() as? com.bitchat.android.mesh.BluetoothMeshService
                    val statusMessage = "🤖 AI processing \"${message.content.take(50)}${if (message.content.length > 50) "..." else ""}\""
                    android.util.Log.d("MeshDelegateHandler", "Sending AI status message: '$statusMessage'")
                    meshService?.sendMessage(statusMessage, emptyList(), null)
                    android.util.Log.d("MeshDelegateHandler", "AI status message sent to mesh network")
                    
                    onTriggerAiResponse?.invoke("/ai ${message.content}")
                }
            }
            
            // Periodic cleanup
            if (messageManager.isMessageProcessed("cleanup_check_${System.currentTimeMillis()/30000}")) {
                messageManager.cleanupDeduplicationCaches()
            }
        }
    }
    
    override fun didConnectToPeer(peerID: String) {
        coroutineScope.launch {
            // FIXED: Deduplicate connection events from dual connection paths
            if (messageManager.isDuplicateSystemEvent("connect", peerID)) {
                return@launch
            }
            
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "$peerID connected",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    override fun didDisconnectFromPeer(peerID: String) {
        coroutineScope.launch {
            // FIXED: Deduplicate disconnection events from dual connection paths
            if (messageManager.isDuplicateSystemEvent("disconnect", peerID)) {
                return@launch
            }
            
            val systemMessage = BitchatMessage(
                sender = "system",
                content = "$peerID disconnected",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    override fun didUpdatePeerList(peers: List<String>) {
        coroutineScope.launch {
            state.setConnectedPeers(peers)
            state.setIsConnected(peers.isNotEmpty())
            
            // Clean up channel members who disconnected
            channelManager.cleanupDisconnectedMembers(peers, getMyPeerID())
            
            // Exit private chat if peer disconnected
            state.getSelectedPrivateChatPeerValue()?.let { currentPeer ->
                if (!peers.contains(currentPeer)) {
                    privateChatManager.cleanupDisconnectedPeer(currentPeer)
                }
            }
        }
    }
    
    override fun didReceiveChannelLeave(channel: String, fromPeer: String) {
        coroutineScope.launch {
            channelManager.removeChannelMember(channel, fromPeer)
        }
    }
    
    override fun didReceiveDeliveryAck(ack: DeliveryAck) {
        coroutineScope.launch {
            messageManager.updateMessageDeliveryStatus(ack.originalMessageID, DeliveryStatus.Delivered(ack.recipientNickname, ack.timestamp))
        }
    }
    
    override fun didReceiveReadReceipt(receipt: ReadReceipt) {
        coroutineScope.launch {
            messageManager.updateMessageDeliveryStatus(receipt.originalMessageID, DeliveryStatus.Read(receipt.readerNickname, receipt.timestamp))
        }
    }
    
    override fun decryptChannelMessage(encryptedContent: ByteArray, channel: String): String? {
        return channelManager.decryptChannelMessage(encryptedContent, channel)
    }
    
    override fun getNickname(): String? = state.getNicknameValue()
    
    override fun isFavorite(peerID: String): Boolean {
        return privateChatManager.isFavorite(peerID)
    }
    
    /**
     * Send read receipts reactively based on UI focus state.
     * Uses same logic as notification system - send read receipt if user is currently
     * viewing the private chat with this sender AND app is in foreground.
     */
    private fun sendReadReceiptIfFocused(senderPeerID: String) {
        // Get notification manager's focus state (mirror the notification logic)
        val isAppInBackground = notificationManager.getAppBackgroundState()
        val currentPrivateChatPeer = notificationManager.getCurrentPrivateChatPeer()
        
        // Send read receipt if user is currently focused on this specific chat
        val shouldSendReadReceipt = !isAppInBackground && currentPrivateChatPeer == senderPeerID
        
        if (shouldSendReadReceipt) {
            android.util.Log.d("MeshDelegateHandler", "Sending reactive read receipt for focused chat with $senderPeerID")
            privateChatManager.sendReadReceiptsForPeer(senderPeerID, getMeshService())
        } else {
            android.util.Log.d("MeshDelegateHandler", "Skipping read receipt - chat not focused (background: $isAppInBackground, current peer: $currentPrivateChatPeer, sender: $senderPeerID)")
        }
    }
    
    /**
     * Handle special swarm protocol messages for role coordination and emergency features
     */
    private fun handleSwarmProtocolMessage(message: BitchatMessage) {
        val content = message.content
        val senderPeerID = message.senderPeerID ?: return
        
        when {
            // Role update protocol: 🔄 ROLE_UPDATE:peerID:role:timestamp
            content.startsWith("🔄 ROLE_UPDATE:") -> {
                try {
                    val parts = content.removePrefix("🔄 ROLE_UPDATE:").split(":")
                    if (parts.size >= 3) {
                        val peerID = parts[0]
                        val role = parts[1]
                        val timestamp = parts.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()
                        
                        // Verify sender matches claimed peer ID (security)
                        if (peerID == senderPeerID) {
                            state.setSwarmMemberRole(peerID, role)
                            android.util.Log.d("SwarmProtocol", "Updated role for $peerID: $role")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SwarmProtocol", "Failed to parse role update", e)
                }
            }
            
            // Supply request protocol: 📦 SUPPLY_REQUEST:peerID:item:timestamp
            content.contains("SUPPLY_REQUEST:") -> {
                try {
                    val parts = content.substringAfter("SUPPLY_REQUEST:").split(":")
                    if (parts.size >= 3) {
                        val peerID = parts[0]
                        val item = parts[1]
                        // Could store supply requests in state for coordination
                        android.util.Log.d("SwarmProtocol", "Supply request from $peerID: $item")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SwarmProtocol", "Failed to parse supply request", e)
                }
            }
            
            // Emergency alert protocol: 🚨 EMERGENCY:peerID:role:message:timestamp
            content.startsWith("🚨 EMERGENCY:") -> {
                try {
                    val parts = content.removePrefix("🚨 EMERGENCY:").split(":", limit = 5)
                    if (parts.size >= 4) {
                        val peerID = parts[0]
                        val role = parts[1]
                        val alertMessage = parts[2]
                        
                        // Could trigger special emergency UI or notifications
                        android.util.Log.w("SwarmEmergency", "EMERGENCY from $peerID ($role): $alertMessage")
                        
                        // For now, just ensure it's visible in chat
                        // Emergency messages are already added to messageManager above
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SwarmProtocol", "Failed to parse emergency alert", e)
                }
            }
        }
    }
    
    // registerPeerPublicKey REMOVED - fingerprints now handled centrally in PeerManager
}
