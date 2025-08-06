/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.swarm

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.bitchat.android.ui.ChatViewModel
import com.google.ai.edge.gallery.ui.imagegeneration.ImageGenerationViewModel
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.data.TASK_LLM_ASK_IMAGE

/**
 * Smart Rescue Swarm - Visual Grid Interface for Emergency Coordination
 * 
 * Features:
 * - Visual grid of connected users with roles
 * - Real-time AI capability indicators  
 * - Auto role assignment and easy switching
 * - Emergency channel coordination
 * - Offline-first design with beautiful UI
 */

enum class SwarmRole(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
) {
    SCOUT("Scout", Icons.Default.Search, Color(0xFF2196F3), "Search & reconnaissance"),
    MEDIC("Medic", Icons.Default.LocalHospital, Color(0xFFE91E63), "Medical triage & treatment"),
    LEADER("Leader", Icons.Default.Star, Color(0xFFFF9800), "Command & coordination"),
    HELPER("Helper", Icons.Default.Handyman, Color(0xFF4CAF50), "Support & supplies"),
    ANALYST("Analyst", Icons.Default.Analytics, Color(0xFF9C27B0), "Data processing & AI"),
    UNASSIGNED("Available", Icons.Default.Person, Color(0xFF757575), "Ready for assignment")
}

data class SwarmMember(
    val peerId: String,
    val nickname: String,
    val role: SwarmRole,
    val hasAI: Boolean,
    val isProcessing: Boolean,
    val lastSeen: Long,
    val batteryLevel: Int? = null,
    val isMe: Boolean = false
)

@Composable
fun SwarmScreen(
    chatViewModel: ChatViewModel,
    llmViewModel: LlmChatViewModel,
    imageGenViewModel: ImageGenerationViewModel,
    modelManagerViewModel: ModelManagerViewModel,
    onNavigateToBitChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Dialog states for user input
    var showAnalyzeDialog by remember { mutableStateOf(false) }
    var showSupplyDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    val connectedPeers by chatViewModel.connectedPeers.observeAsState(emptyList())
    val currentNickname by chatViewModel.nickname.observeAsState("")
    val isConnected by chatViewModel.isConnected.observeAsState(false)
    val aiModeEnabled by chatViewModel.aiModeEnabled.observeAsState(false)
    
    // Get roles from actual ChatState (synced with mesh network)
    val myRoleString by chatViewModel.swarmRole.observeAsState("")
    val memberRoles by chatViewModel.swarmMemberRoles.observeAsState(emptyMap())
    
    // Convert string role to SwarmRole enum
    val myRole = remember(myRoleString) {
        when (myRoleString.lowercase()) {
            "scout" -> SwarmRole.SCOUT
            "medic" -> SwarmRole.MEDIC
            "leader" -> SwarmRole.LEADER
            "helper" -> SwarmRole.HELPER
            "analyst" -> SwarmRole.ANALYST
            else -> SwarmRole.UNASSIGNED
        }
    }
    
    // Convert member role strings to SwarmRole map
    val userRoles = remember(memberRoles) {
        memberRoles.mapValues { (_, roleString) ->
            when (roleString.lowercase()) {
                "scout" -> SwarmRole.SCOUT
                "medic" -> SwarmRole.MEDIC
                "leader" -> SwarmRole.LEADER
                "helper" -> SwarmRole.HELPER
                "analyst" -> SwarmRole.ANALYST
                else -> SwarmRole.UNASSIGNED
            }
        }
    }
    
    // Auto-detect AI capabilities  
    val hasAiCapability = remember { 
        try {
            llmViewModel?.task?.models?.isNotEmpty() == true
        } catch (e: Exception) {
            android.util.Log.w("SwarmScreen", "Could not detect AI capability", e)
            false
        }
    }
    
    // Create swarm members list
    val swarmMembers = remember(connectedPeers, userRoles, myRole) {
        val members = mutableListOf<SwarmMember>()
        
        // Add myself
        members.add(SwarmMember(
            peerId = "me",
            nickname = currentNickname.ifEmpty { "You" },
            role = myRole,
            hasAI = hasAiCapability,
            isProcessing = aiModeEnabled,
            lastSeen = System.currentTimeMillis(),
            isMe = true
        ))
        
        // Add connected peers
        connectedPeers.forEach { peerId ->
            members.add(SwarmMember(
                peerId = peerId,
                nickname = peerId.take(8), // Use first 8 chars as display name
                role = userRoles[peerId] ?: SwarmRole.UNASSIGNED,
                hasAI = true, // Assume others have AI capability
                isProcessing = false,
                lastSeen = System.currentTimeMillis()
            ))
        }
        members
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Your Role Card - Full Width
                MyRoleCard(
                    currentRole = myRole,
                    hasAI = hasAiCapability,
                    onRoleChange = { newRole ->
                        val roleCommand = "/role ${newRole.displayName.lowercase()}"
                        chatViewModel.sendMessage(roleCommand)
                    }
                )
            }
            
            item {
                // Emergency Swarm Status Card - Full Width
                GalleryStyleStatusCard(
                    currentRole = myRole,
                    isConnected = isConnected,
                    aiModeEnabled = aiModeEnabled,
                    memberCount = swarmMembers.size
                )
            }
            
            item {
                // Professional Emergency Actions Header
                Text(
                    text = "Emergency Actions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            item {
                // Action cards in 2x2 grid (first row)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GalleryStyleActionCard(
                        icon = Icons.Default.Psychology,
                        label = "AI Analyze",
                        description = "Get AI insights",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            showAnalyzeDialog = true
                        },
                        modifier = Modifier.weight(1f).height(140.dp)
                    )
                    
                    GalleryStyleActionCard(
                        icon = Icons.Default.Inventory,
                        label = "Request Supply",
                        description = "Coordinate resources",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            showSupplyDialog = true
                        },
                        modifier = Modifier.weight(1f).height(140.dp)
                    )
                }
            }
            
            item {
                // Action cards (second row)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GalleryStyleActionCard(
                        icon = Icons.Default.Warning,
                        label = "Emergency Alert",
                        description = "Broadcast urgent alert",
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            showEmergencyDialog = true
                        },
                        modifier = Modifier.weight(1f).height(140.dp)
                    )
                    
                    GalleryStyleActionCard(
                        icon = Icons.Default.Info,
                        label = "Show Status",
                        description = "Network overview",
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            chatViewModel.sendMessage("/status")
                        },
                        modifier = Modifier.weight(1f).height(140.dp)
                    )
                }
            }
            
            // Move Active Swarm Network right after Emergency Actions
            item {
                // Professional Active Swarm Network Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Network",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${swarmMembers.size} connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Swarm Members with proper spacing
            if (swarmMembers.isNotEmpty()) {
                items(items = swarmMembers.chunked(2)) { memberRow ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        memberRow.forEach { member ->
                            GalleryStyleSwarmMemberCard(
                                member = member,
                                onAssignRole = { newRole ->
                                    if (!member.isMe) {
                                        chatViewModel.sendMessage("ℹ️ Users must self-assign roles for security. Ask them to use: /role ${newRole.displayName.lowercase()}")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number of members
                        if (memberRow.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    EmptySwarmState()
                }
            }
            
            // Move feature guide to bottom
            item {
                SwarmFeatureGuide(
                    currentRole = myRole,
                    hasAI = hasAiCapability
                )
            }
        }
    }
    
    // Input Dialogs for Actions
    if (showAnalyzeDialog) {
        SmartAnalysisDialog(
            currentRole = myRole,
            onConfirm = { analysisText, imageUri ->
                // Use role-specific analysis prompt
                val rolePrompt = getRoleSpecificAnalysisPrompt(myRole)
                val fullPrompt = "$rolePrompt $analysisText"
                
                if (imageUri != null) {
                    // Direct AI image analysis using Gallery LLM
                    performDirectImageAnalysis(
                        context = context,
                        llmViewModel = llmViewModel,
                        modelManagerViewModel = modelManagerViewModel,
                        prompt = fullPrompt,
                        imageUri = imageUri,
                        chatViewModel = chatViewModel,
                        roleDisplayName = myRole.displayName
                    )
                } else {
                    // Direct text-only AI analysis using Gallery LLM
                    performDirectTextAnalysis(
                        llmViewModel = llmViewModel,
                        modelManagerViewModel = modelManagerViewModel,
                        prompt = fullPrompt,
                        chatViewModel = chatViewModel,
                        roleDisplayName = myRole.displayName,
                        context = context
                    )
                }
                showAnalyzeDialog = false
                // Navigate to BitChat tab after action
                onNavigateToBitChat()
            },
            onDismiss = { showAnalyzeDialog = false }
        )
    }
    
    if (showSupplyDialog) {
        SupplyRequestDialog(
            currentRole = myRole,
            onConfirm = { supplyType, description, urgency, aiCallback ->
                // Send supply request
                val message = "/supply [$urgency] $supplyType: ${description.ifEmpty { "No additional details" }}"
                chatViewModel.sendMessage(message)
                
                // Trigger AI suggestions in chat
                aiCallback()
                
                showSupplyDialog = false
                // Navigate to BitChat tab after action
                onNavigateToBitChat()
            },
            onDismiss = { showSupplyDialog = false },
            chatViewModel = chatViewModel,
            llmViewModel = llmViewModel,
            modelManagerViewModel = modelManagerViewModel
        )
    }
    
    if (showEmergencyDialog) {
        EmergencyAlertDialog(
            onConfirm = { emergencyType, description, location, aiCallback ->
                // Send emergency alert
                val message = "/emergency [$emergencyType] $description${if (location != "Location unknown") " at $location" else ""}"
                chatViewModel.sendMessage(message)
                
                // Debug: Confirm callback is being called
                android.util.Log.d("EmergencyAlert", "Emergency alert confirmed, calling AI callback")
                chatViewModel.sendMessage("🚨 DEBUG: Emergency alert sent, triggering AI analysis...")
                
                // Trigger AI analysis in chat
                try {
                    aiCallback()
                    android.util.Log.d("EmergencyAlert", "AI callback executed successfully")
                } catch (e: Exception) {
                    android.util.Log.e("EmergencyAlert", "AI callback failed", e)
                    chatViewModel.sendMessage("🚨 DEBUG: AI callback failed - ${e.message}")
                }
                
                showEmergencyDialog = false
                // Navigate to BitChat tab after action
                onNavigateToBitChat()
            },
            onDismiss = { showEmergencyDialog = false },
            chatViewModel = chatViewModel,
            llmViewModel = llmViewModel,
            modelManagerViewModel = modelManagerViewModel
        )
    }
}

@Composable
fun SwarmHeader(
    isConnected: Boolean,
    totalMembers: Int,
    aiCapableMembers: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2537)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(2.dp, Color(0xFFFF5722).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "🔗 AI Swarm Network",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722) // Emergency orange
                    )
                    Text(
                        text = if (isConnected) "🟢 Network Active" else "🔴 Offline Mode",
                        fontSize = 14.sp,
                        color = if (isConnected) Color(0xFF00E676) else Color(0xFFE53935)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$totalMembers",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676) // Bright green
                    )
                    Text(
                        text = "members",
                        fontSize = 12.sp,
                        color = Color(0xFFBBBBBB)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusChip(
                    icon = Icons.Default.SmartToy,
                    text = "$aiCapableMembers AI-Ready",
                    color = Color(0xFF2196F3)
                )
                
                StatusChip(
                    icon = Icons.Default.Group,
                    text = "${totalMembers} Connected",
                    color = Color(0xFF00E676)
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MyRoleCard(
    currentRole: SwarmRole,
    hasAI: Boolean,
    onRoleChange: (SwarmRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRoleDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = when (currentRole) {
                        SwarmRole.MEDIC -> listOf(Color(0xFFE17055), Color(0xFFD63031))
                        SwarmRole.SCOUT -> listOf(Color(0xFF00B894), Color(0xFF00A085))
                        SwarmRole.LEADER -> listOf(Color(0xFFFD79A8), Color(0xFFE84393))
                        SwarmRole.HELPER -> listOf(Color(0xFF74B9FF), Color(0xFF0984E3))
                        SwarmRole.ANALYST -> listOf(Color(0xFFA29BFE), Color(0xFF6C5CE7))
                        SwarmRole.UNASSIGNED -> listOf(Color(0xFFDDD6D6), Color(0xFFB2B2B2))
                    }
                )
            )
            .clickable { showRoleDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = currentRole.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${currentRole.displayName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = currentRole.description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                if (hasAI) {
                    Text(
                        text = "AI ENHANCED",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Change role",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
    
    if (showRoleDialog) {
        RoleSelectionDialog(
            currentRole = currentRole,
            onRoleSelected = { role ->
                onRoleChange(role)
                showRoleDialog = false
            },
            onDismiss = { showRoleDialog = false }
        )
    }
}

@Composable
fun SwarmMemberCard(
    member: SwarmMember,
    onAssignRole: (SwarmRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRoleDialog by remember { mutableStateOf(false) }
    val timeSinceLastSeen = (System.currentTimeMillis() - member.lastSeen) / 1000
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                if (!member.isMe) showRoleDialog = true 
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (member.isMe) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isMe) 
                Color(0xFF2D3748) // Darker for "me"
            else 
                Color(0xFF1E2537) // Regular dark
        ),
        border = if (member.isMe) 
            BorderStroke(2.dp, member.role.color.copy(alpha = 0.8f))
        else 
            BorderStroke(1.dp, Color(0xFF4A5568).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with role color
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(member.role.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = member.role.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Name and status
            Text(
                text = member.nickname,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            
            Text(
                text = member.role.displayName,
                fontSize = 11.sp,
                color = member.role.color,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Capability indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (member.hasAI) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "🤖",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
                
                if (member.isProcessing) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF9800).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "💭",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }
            
            // Last seen
            Text(
                text = if (timeSinceLastSeen < 60) "now" else "${timeSinceLastSeen/60}m ago",
                fontSize = 9.sp,
                color = Color(0xFFBBBBBB)
            )
        }
    }
    
    if (showRoleDialog) {
        RoleSelectionDialog(
            currentRole = member.role,
            onRoleSelected = { role ->
                onAssignRole(role)
                showRoleDialog = false
            },
            onDismiss = { showRoleDialog = false }
        )
    }
}

@Composable
fun RoleSelectionDialog(
    currentRole: SwarmRole,
    onRoleSelected: (SwarmRole) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Role",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(SwarmRole.values()) { role ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRoleSelected(role) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (role == currentRole) 
                                role.color.copy(alpha = 0.3f) 
                            else 
                                Color(0xFF74B9FF).copy(alpha = 0.1f)
                        ),
                        border = if (role == currentRole) 
                            BorderStroke(2.dp, role.color) 
                        else null
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = role.icon,
                                contentDescription = null,
                                tint = role.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = role.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SwarmActionButtons(
    currentRole: SwarmRole,
    onAnalyze: (String) -> Unit,
    onSupplyRequest: (String) -> Unit,
    onEmergencyAlert: (String) -> Unit,
    onShowStatus: () -> Unit
) {
    var showAnalyzeDialog by remember { mutableStateOf(false) }
    var showSupplyDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2537)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, Color(0xFF4A5568).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚡ EMERGENCY ACTIONS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5722), // Emergency orange
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Action buttons grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // AI Analyze Button
                ActionButton(
                    icon = Icons.Default.Psychology,
                    label = "AI Analyze",
                    color = Color(0xFF2196F3), // Blue for AI
                    onClick = { showAnalyzeDialog = true },
                    modifier = Modifier.weight(1f)
                )
                
                // Supply Request Button  
                ActionButton(
                    icon = Icons.Default.Inventory,
                    label = "Request Supply",
                    color = Color(0xFF00E676), // Green for supplies
                    onClick = { showSupplyDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Emergency Alert Button
                ActionButton(
                    icon = Icons.Default.Warning,
                    label = "Emergency",
                    color = Color(0xFFFF5722), // Bright emergency red
                    onClick = { showEmergencyDialog = true },
                    modifier = Modifier.weight(1f)
                )
                
                // Status Button
                ActionButton(
                    icon = Icons.Default.Info,
                    label = "Show Status",
                    color = Color(0xFFFFAB00), // Amber for status
                    onClick = onShowStatus,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    
    // Dialog handlers
    if (showAnalyzeDialog) {
        TextInputDialog(
            title = "AI Analysis Request",
            placeholder = when (currentRole) {
                SwarmRole.MEDIC -> "Describe medical situation..."
                SwarmRole.SCOUT -> "Describe terrain/hazard..."
                SwarmRole.LEADER -> "Describe tactical situation..."
                else -> "Describe situation for AI analysis..."
            },
            onConfirm = { input ->
                onAnalyze(input)
                showAnalyzeDialog = false
            },
            onDismiss = { showAnalyzeDialog = false }
        )
    }
    
    if (showSupplyDialog) {
        TextInputDialog(
            title = "Supply Request",
            placeholder = when (currentRole) {
                SwarmRole.MEDIC -> "Medical supplies needed (e.g., bandages, antiseptic)..."
                SwarmRole.SCOUT -> "Equipment needed (e.g., flashlight, rope)..."
                else -> "Item needed..."
            },
            onConfirm = { input ->
                onSupplyRequest(input)
                showSupplyDialog = false
            },
            onDismiss = { showSupplyDialog = false }
        )
    }
    
    if (showEmergencyDialog) {
        TextInputDialog(
            title = "🚨 EMERGENCY ALERT",
            placeholder = "Urgent situation description...",
            isEmergency = true,
            onConfirm = { input ->
                onEmergencyAlert(input)
                showEmergencyDialog = false
            },
            onDismiss = { showEmergencyDialog = false }
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 10.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    placeholder: String,
    isEmergency: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isEmergency) Color(0xFF2D1B1F) else Color(0xFF1E2537),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEmergency) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = if (isEmergency) Color(0xFFFF5722) else Color(0xFF2196F3)
                )
            }
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isEmergency) Color(0xFFFF5722) else Color(0xFF2196F3),
                    unfocusedBorderColor = Color(0xFF4A5568),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (text.trim().isNotEmpty()) {
                        onConfirm(text.trim())
                    }
                },
                enabled = text.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEmergency) Color(0xFFFF5722) else Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Text(if (isEmergency) "SEND ALERT" else "Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptySwarmState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2537)
        ),
        border = BorderStroke(1.dp, Color(0xFF4A5568).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = Color(0xFF4A5568),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "🔍 Searching for Swarm Members",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFAB00) // Amber
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Get other BitChat users nearby to join your emergency swarm network. They'll appear here automatically when in range.",
                fontSize = 14.sp,
                color = Color(0xFFBBBBBB),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2196F3).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f))
            ) {
                Text(
                    text = "💡 TIP: Make sure Bluetooth is enabled and you're close to other users",
                    fontSize = 12.sp,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun SmartAnalysisDialog(
    currentRole: SwarmRole,
    onConfirm: (String, android.net.Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    var analysisText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Analysis Request",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                Text(
                    text = getRoleSpecificAnalysisPrompt(currentRole),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                OutlinedTextField(
                    value = analysisText,
                    onValueChange = { analysisText = it },
                    placeholder = { Text(getRoleSpecificPlaceholder(currentRole)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Check if vision models are available (simplified)
                    val hasDownloadedVisionModels = true // Temporarily always enabled
                    
                    Button(
                        onClick = { 
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = hasDownloadedVisionModels,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasDownloadedVisionModels) Color(0xFF6C5CE7) else Color.Gray,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Attach Image")
                    }
                    
                    if (selectedImageUri != null) {
                        Text(
                            text = "✓ Image attached",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (!hasDownloadedVisionModels) {
                        Text(
                            text = "⚠️ Download vision model in Gallery for image analysis",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (analysisText.trim().isNotEmpty()) {
                        onConfirm(analysisText.trim(), selectedImageUri)
                    }
                },
                enabled = analysisText.trim().isNotEmpty()
            ) {
                Text("Analyze with AI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable  
fun SupplyRequestDialog(
    currentRole: SwarmRole,
    onConfirm: (String, String, String, () -> Unit) -> Unit, // Added callback parameter for AI suggestions
    onDismiss: () -> Unit,
    chatViewModel: ChatViewModel? = null,
    llmViewModel: LlmChatViewModel? = null,
    modelManagerViewModel: ModelManagerViewModel? = null
) {
    val context = LocalContext.current
    var supplyType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Medium") }
    
    val urgencyOptions = listOf("Low", "Medium", "High", "Critical")
    val roleSupplies = getRoleSpecificSupplies(currentRole)
    
    // Function to send AI suggestions to chat after supply request is broadcast
    fun sendAiSuggestionsToChat() {
        if (supplyType.trim().isEmpty() || chatViewModel == null) return
        
        val roleContext = when (currentRole) {
            SwarmRole.MEDIC -> "As a field medic in an emergency response scenario"
            SwarmRole.SCOUT -> "As a reconnaissance scout in a search and rescue operation"  
            SwarmRole.LEADER -> "As an incident commander coordinating emergency response"
            SwarmRole.HELPER -> "As a support specialist providing logistical assistance"
            SwarmRole.ANALYST -> "As a data analyst supporting emergency coordination"
            SwarmRole.UNASSIGNED -> "As an emergency responder"
        }
        
        val analysisPrompt = """$roleContext, provide suggestions and recommendations for this supply request:

Supply Type: $supplyType
Description: ${description.ifEmpty { "No additional details provided" }}
Urgency: $urgency
Role: ${currentRole.displayName}

Please provide:
1. Specific recommendations for this supply type
2. Alternative options if primary supply unavailable
3. Estimated quantities needed
4. Critical considerations for the ${currentRole.displayName} role
5. Coordination suggestions with other team members

Provide practical, actionable suggestions."""

        // Get available LLM models from TASK_LLM_CHAT
        val chatModels = com.google.ai.edge.gallery.data.TASK_LLM_CHAT.models
        
        if (chatModels.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel.sendMessage("🤖 AI Suggestions: No AI models available for supply recommendations.")
            }
            return
        }
        
        // Find a downloaded model
        val downloadedModel = chatModels.find { model ->
            val modelPath = model.getPath(context)
            java.io.File(modelPath).exists()
        }
        
        if (downloadedModel == null) {
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel.sendMessage("🤖 AI Suggestions: Models not downloaded. Please download a model in Gallery tab.")
            }
            return
        }
        
        // Send initial message
        CoroutineScope(Dispatchers.Main).launch {
            chatViewModel.sendMessage("🤖 Analyzing supply request for $supplyType...")
        }
        
        // Perform AI inference using LlmChatModelHelper
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if model is initialized before running inference
                if (downloadedModel.instance == null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        chatViewModel.sendMessage("🤖 AI Suggestions: Model not initialized. Please try again after model is loaded.")
                    }
                    return@launch
                }
                
                val accumulatedResponse = StringBuilder()
                
                com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper.runInference(
                    model = downloadedModel,
                    input = analysisPrompt,
                    resultListener = { partialResult: String, done: Boolean ->
                        try {
                            CoroutineScope(Dispatchers.Main).launch {
                                if (partialResult.isNotEmpty()) {
                                    accumulatedResponse.append(partialResult)
                                }
                                
                                if (done) {
                                    val finalResponse = accumulatedResponse.toString().trim()
                                    if (finalResponse.isNotEmpty()) {
                                        chatViewModel.sendMessage("🤖 AI Supply Suggestions:\n\n$finalResponse")
                                    } else {
                                        chatViewModel.sendMessage("🤖 AI Suggestions: Analysis completed but no specific recommendations available.")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            CoroutineScope(Dispatchers.Main).launch {
                                chatViewModel.sendMessage("🤖 AI Suggestions: Result processing failed - ${e.message}")
                            }
                        }
                    },
                    cleanUpListener = {
                        // Cleanup completed
                    }
                )
                
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    chatViewModel.sendMessage("🤖 AI Suggestions: Analysis failed - ${e.message}")
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Supply Request",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "What supplies do you need? This will be broadcast to all swarm members.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Quick supply buttons for current role
                Text(
                    text = "Quick Select (${currentRole.displayName}):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(80.dp)
                ) {
                    items(roleSupplies) { supply ->
                        Button(
                            onClick = { supplyType = supply },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (supplyType == supply) Color(0xFF00B894) else Color(0xFF74B9FF),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = supply,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = supplyType,
                    onValueChange = { supplyType = it },
                    label = { Text("Supply Type") },
                    placeholder = { Text("e.g., Medical kit, Water, Flashlight") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details") },
                    placeholder = { Text("Quantity needed, specific requirements...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Urgency:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    urgencyOptions.forEach { option ->
                        FilterChip(
                            onClick = { urgency = option },
                            label = { Text(option) },
                            selected = urgency == option,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when(option) {
                                    "Critical" -> Color(0xFFE74C3C)
                                    "High" -> Color(0xFFFF6B35)
                                    "Medium" -> Color(0xFF3498DB)
                                    else -> Color(0xFF27AE60)
                                },
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (supplyType.trim().isNotEmpty()) {
                        onConfirm(supplyType.trim(), description.trim(), urgency) {
                            // Callback to trigger AI suggestions after broadcast
                            sendAiSuggestionsToChat()
                        }
                    }
                },
                enabled = supplyType.trim().isNotEmpty()
            ) {
                Text("Broadcast Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmergencyAlertDialog(
    onConfirm: (String, String, String, () -> Unit) -> Unit, // Added callback parameter for AI analysis
    onDismiss: () -> Unit,
    chatViewModel: ChatViewModel? = null,
    llmViewModel: LlmChatViewModel? = null,
    modelManagerViewModel: ModelManagerViewModel? = null
) {
    val context = LocalContext.current
    var emergencyType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    
    val emergencyTypes = listOf(
        "Medical Emergency",
        "Fire/Hazard", 
        "Structural Damage",
        "Person Missing",
        "Rescue Needed",
        "General Emergency"
    )
    
    // Function to send AI emergency analysis to chat after alert is broadcast
    fun sendAiAnalysisToChat() {
        android.util.Log.d("EmergencyAlert", "sendAiAnalysisToChat called!")
        android.widget.Toast.makeText(context, "Emergency AI Analysis Started", android.widget.Toast.LENGTH_LONG).show()
        
        if (emergencyType.isEmpty() || description.trim().isEmpty() || chatViewModel == null) {
            android.util.Log.d("EmergencyAlert", "Missing emergency details or chat system unavailable")
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel?.sendMessage("🚨 AI Emergency Analysis: Missing emergency details or chat system unavailable.")
            }
            return
        }
        
        // Get list of active users and their roles from ChatState
        val activeUsers = mutableListOf<String>()
        val userRoles = mutableMapOf<String, String>()
        
        try {
            // Get connected peers and their nicknames from MeshService with safe casting
            val bitchatVM = chatViewModel as? com.bitchat.android.ui.ChatViewModel
            if (bitchatVM != null) {
                val connectedPeers = bitchatVM.getConnectedPeers()
                val peerNicknames = bitchatVM.meshService.getPeerNicknames()
                
                connectedPeers.forEach { peerID ->
                    val nickname = peerNicknames[peerID]
                    if (!nickname.isNullOrEmpty() && nickname != "Unknown") {
                        activeUsers.add(nickname)
                        // Get role if available (stored in SwarmRole state)
                        userRoles[nickname] = "helper" // Default role, could be enhanced to get actual roles
                    }
                }
                
                // Also include current user
                val myNickname = bitchatVM.getCurrentUserNickname()
                if (!myNickname.isNullOrEmpty()) {
                    activeUsers.add("$myNickname (you)")
                    userRoles["$myNickname (you)"] = "leader" // Current user is leader
                }
            }
            
        } catch (e: Exception) {
            // Fallback if we can't get peer info
            activeUsers.add("Team Member 1")
            activeUsers.add("Team Member 2")
            userRoles["Team Member 1"] = "helper"
            userRoles["Team Member 2"] = "helper"
        }
        
        val activeUsersText = if (activeUsers.isNotEmpty()) {
            activeUsers.joinToString(", ") { user -> 
                val role = userRoles[user] ?: "helper"
                "$user ($role)"
            }
        } else {
            "No active users connected"
        }
        
        val analysisPrompt = """You are an emergency incident commander. Analyze this emergency situation and assign specific tasks to available team members.

EMERGENCY DETAILS:
- Type: $emergencyType
- Description: $description
- Location: ${location.ifEmpty { "Location not specified" }}

AVAILABLE TEAM MEMBERS:
$activeUsersText

INSTRUCTIONS:
1. Analyze the emergency situation and immediate priorities
2. Assign specific tasks to each available team member based on their roles
3. Provide clear action items with @username mentions
4. Include timeline and coordination requirements
5. Identify any additional resources needed

Format your response with:
- **SITUATION ANALYSIS:** Brief assessment
- **TASK ASSIGNMENTS:** Specific @username tasks
- **COORDINATION:** Team coordination instructions
- **RESOURCES NEEDED:** Additional requirements

Provide actionable incident command response with clear task assignments."""

        
        // Use the existing /ai command functionality through the ChatViewModel
        val bitchatViewModel = chatViewModel as? com.bitchat.android.ui.ChatViewModel
        if (bitchatViewModel != null) {
            // Create CommandProcessor to access AI functionality without showing user prompt
            val commandProcessor = com.bitchat.android.ui.CommandProcessor(
                state = bitchatViewModel.getChatState(),
                messageManager = bitchatViewModel.getChatMessageManager(),
                channelManager = bitchatViewModel.getChatChannelManager(),
                privateChatManager = bitchatViewModel.getChatPrivateChatManager(),
                context = context
            )
            
            CoroutineScope(Dispatchers.Main).launch {
                // Use emergency AI analysis method that doesn't show user prompt
                commandProcessor.performEmergencyAiAnalysis(
                    prompt = analysisPrompt,
                    myPeerID = bitchatViewModel.getCurrentPeerID() ?: "emergency_commander",
                    onSendMessage = { message, _, _ ->
                        bitchatViewModel.sendMessage(message)
                    }
                )
            }
        } else {
            // Fallback to basic message if ChatViewModel type doesn't match
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel?.sendMessage("🚨 AI Emergency Analysis: Unable to perform streaming analysis. Chat system incompatible.")
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EMERGENCY ALERT",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "This will immediately alert ALL swarm members. Use only for genuine emergencies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text("Emergency Type:", fontWeight = FontWeight.Medium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(emergencyTypes) { type ->
                        Button(
                            onClick = { emergencyType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (emergencyType == type) Color(0xFFE74C3C) else Color(0xFF74B9FF),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = type,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Emergency Description") },
                    placeholder = { Text("What happened? How many people affected?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("Building, floor, landmark, GPS coordinates...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (emergencyType.isNotEmpty() && description.trim().isNotEmpty()) {
                        onConfirm(emergencyType, description.trim(), location.trim().ifEmpty { "Location unknown" }) {
                            // Callback to trigger AI analysis after alert is broadcast
                            sendAiAnalysisToChat()
                        }
                    }
                },
                enabled = emergencyType.isNotEmpty() && description.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("SEND EMERGENCY ALERT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getRoleSpecificAnalysisPrompt(role: SwarmRole): String = when (role) {
    SwarmRole.MEDIC -> "As a medic, describe the medical situation you need AI analysis for:"
    SwarmRole.SCOUT -> "As a scout, describe the area, hazard, or situation to analyze:"
    SwarmRole.LEADER -> "As a leader, describe the tactical situation requiring analysis:"
    SwarmRole.ANALYST -> "As an analyst, describe the data or situation to process:"
    SwarmRole.HELPER -> "As a helper, describe what you need analyzed to provide better support:"
    SwarmRole.UNASSIGNED -> "Describe the situation you need AI insights about:"
}

fun getRoleSpecificPlaceholder(role: SwarmRole): String = when (role) {
    SwarmRole.MEDIC -> "Patient symptoms, injury assessment, treatment options..."
    SwarmRole.SCOUT -> "Terrain conditions, obstacles, safe routes, hazards..."
    SwarmRole.LEADER -> "Team positioning, resource allocation, strategic decisions..."
    SwarmRole.ANALYST -> "Data patterns, correlation analysis, predictions..."
    SwarmRole.HELPER -> "Resource needs, logistics, coordination requirements..."
    SwarmRole.UNASSIGNED -> "Describe the situation you need analyzed..."
}

fun getRoleSpecificSupplies(role: SwarmRole): List<String> = when (role) {
    SwarmRole.MEDIC -> listOf("Bandages", "Antiseptic", "Pain Meds", "IV Fluids", "Splints", "Oxygen")
    SwarmRole.SCOUT -> listOf("Flashlight", "Rope", "Radio", "Map", "Compass", "Binoculars")
    SwarmRole.LEADER -> listOf("Radio", "Maps", "Tablet", "Power Bank", "Markers", "Clipboard")
    SwarmRole.HELPER -> listOf("Water", "Food", "Blankets", "Tools", "Batteries", "First Aid")
    SwarmRole.ANALYST -> listOf("Laptop", "Tablet", "Power Bank", "Network Hub", "Sensors", "Camera")
    SwarmRole.UNASSIGNED -> listOf("Water", "Food", "Flashlight", "Radio", "First Aid", "Tools")
}

/**
 * Perform direct AI image analysis without using the bridge
 * This is a temporary solution until the bridge compilation issues are resolved
 */
fun performDirectImageAnalysis(
    context: Context,
    llmViewModel: LlmChatViewModel,
    modelManagerViewModel: ModelManagerViewModel,
    prompt: String,
    imageUri: android.net.Uri,
    chatViewModel: ChatViewModel,
    roleDisplayName: String
) {
    try {
        // Send immediate status message
        CoroutineScope(Dispatchers.Main).launch {
            chatViewModel.sendMessage("🔍 AI Image Analysis starting (${roleDisplayName})...")
        }
        
        // Get vision-capable models from TASK_LLM_ASK_IMAGE
        val visionModels = TASK_LLM_ASK_IMAGE.models
        
        if (visionModels.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel.sendMessage("❌ No vision-capable AI models available. Please download a vision model in Gallery.")
            }
            return
        }
        
        // Find a downloaded vision model
        val downloadedVisionModel = visionModels.find { model ->
            val modelPath = model.getPath(context)
            java.io.File(modelPath).exists()
        }
        
        if (downloadedVisionModel == null) {
            val visionModelNames = visionModels.map { it.name }.joinToString(", ")
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel.sendMessage("📥 Vision models not downloaded: $visionModelNames\n\nGo to Gallery tab → Download a vision model → Return to try again.")
            }
            return
        }
        
        // Check if model actually supports vision
        if (!downloadedVisionModel.llmSupportImage) {
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel.sendMessage("⚠️ Model ${downloadedVisionModel.name} does not support image analysis.\n\nAvailable vision models: ${visionModels.filter { it.llmSupportImage }.map { it.name }.joinToString(", ").ifEmpty { "None" }}")
            }
            return
        }
        
        // Convert URI to Bitmap with proper ARGB_8888 config
        val bitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source) { decoder, info, source ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
                }
            } else {
                @Suppress("DEPRECATION")
                val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                if (originalBitmap.config != Bitmap.Config.ARGB_8888) {
                    originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    originalBitmap
                }
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel.sendMessage("❌ Failed to load image: ${e.message}")
            }
            return
        }
        
        // Ensure proper bitmap format
        val finalBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        
        // Initialize model if needed and perform inference
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (downloadedVisionModel.instance == null) {
                    chatViewModel.sendMessage("🔄 Initializing ${downloadedVisionModel.name} for image analysis...")
                    
                    com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper.initialize(
                        context = context,
                        model = downloadedVisionModel,
                        onDone = { errorMessage ->
                            if (errorMessage.isEmpty()) {
                                chatViewModel.sendMessage("✅ Model initialized successfully. Starting inference...")
                                // Model initialized successfully, perform inference
                                performImageInference(downloadedVisionModel, prompt, finalBitmap, chatViewModel, roleDisplayName)
                            } else {
                                chatViewModel.sendMessage("❌ AI Model initialization failed: $errorMessage")
                            }
                        }
                    )
                } else {
                    // Model already initialized
                    performImageInference(downloadedVisionModel, prompt, finalBitmap, chatViewModel, roleDisplayName)
                }
            } catch (e: Exception) {
                chatViewModel.sendMessage("❌ AI Image analysis failed: ${e.message}")
            }
        }
        
    } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
            chatViewModel.sendMessage("❌ Image analysis error: ${e.message}")
        }
    }
}

/**
 * Perform direct text-only AI analysis without using the bridge
 */
fun performDirectTextAnalysis(
    llmViewModel: LlmChatViewModel,
    modelManagerViewModel: ModelManagerViewModel,
    prompt: String,
    chatViewModel: ChatViewModel,
    roleDisplayName: String,
    context: Context? = null
) {
    try {
        // Send immediate status message
        CoroutineScope(Dispatchers.Main).launch {
            chatViewModel.sendMessage("🤖 AI Analysis starting (${roleDisplayName})...")
        }
        
        // Get available LLM models (text models work for text-only analysis)
        val availableModels = llmViewModel.task.models
        
        if (availableModels.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel.sendMessage("❌ No AI models available. Please download a model in Gallery.")
            }
            return
        }
        
        // Find a downloaded model
        val downloadedModel = if (context != null) {
            availableModels.find { model ->
                val modelPath = model.getPath(context)
                java.io.File(modelPath).exists()
            }
        } else {
            availableModels.firstOrNull() // Fallback if no context
        }
        
        if (downloadedModel == null) {
            val modelNames = availableModels.map { it.name }.joinToString(", ")
            CoroutineScope(Dispatchers.Main).launch {
                chatViewModel.sendMessage("📥 Models not downloaded: $modelNames\n\nGo to Gallery tab → Download a model → Return to try again.")
            }
            return
        }
        
        // Initialize model if needed and perform inference
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (downloadedModel.instance == null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        chatViewModel.sendMessage("🔄 Initializing ${downloadedModel.name}...")
                    }
                    
                    if (context != null) {
                        com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper.initialize(
                            context = context,
                            model = downloadedModel,
                            onDone = { errorMessage ->
                                if (errorMessage.isEmpty()) {
                                    // Model initialized successfully, perform inference
                                    performTextInference(downloadedModel, prompt, chatViewModel, roleDisplayName)
                                } else {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        chatViewModel.sendMessage("❌ AI Model initialization failed: $errorMessage")
                                    }
                                }
                            }
                        )
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            chatViewModel.sendMessage("⚠️ Context required for model initialization. Please try image analysis instead.")
                        }
                    }
                } else {
                    // Model already initialized
                    performTextInference(downloadedModel, prompt, chatViewModel, roleDisplayName)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    chatViewModel.sendMessage("❌ AI Text analysis failed: ${e.message}")
                }
            }
        }
        
    } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
            chatViewModel.sendMessage("❌ Text analysis error: ${e.message}")
        }
    }
}

/**
 * Perform the actual image inference with streaming results
 */
fun performImageInference(
    model: com.google.ai.edge.gallery.data.Model,
    prompt: String,
    bitmap: Bitmap,
    chatViewModel: ChatViewModel,
    roleDisplayName: String
) {
    try {
        val accumulatedResponse = StringBuilder()
        CoroutineScope(Dispatchers.Main).launch {
            chatViewModel.sendMessage("🎯 Running image inference with ${model.name}...")
        }
        
        com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper.runInference(
            model = model,
            input = prompt,
            image = bitmap,
            resultListener = { partialResult, done ->
                try {
                    // Accumulate response silently without spamming chat
                    if (partialResult.isNotEmpty()) {
                        accumulatedResponse.append(partialResult)
                    }
                    
                    if (done) {
                        CoroutineScope(Dispatchers.Main).launch {
                            val finalResponse = accumulatedResponse.toString().trim()
                            if (finalResponse.isNotEmpty()) {
                                chatViewModel.sendMessage("📸 AI Image Analysis Result (${roleDisplayName}):\n\n$finalResponse")
                            } else {
                                chatViewModel.sendMessage("🤖 AI could not analyze the image. Please try with a clearer image.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        chatViewModel.sendMessage("❌ Error in result processing: ${e.message}")
                    }
                }
            },
            cleanUpListener = {
                // Cleanup completed silently
            }
        )
    } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
            chatViewModel.sendMessage("❌ Error starting inference: ${e.message}")
        }
    }
}

/**
 * Perform the actual text inference with streaming results
 */
fun performTextInference(
    model: com.google.ai.edge.gallery.data.Model,
    prompt: String,
    chatViewModel: ChatViewModel,
    roleDisplayName: String
) {
    val accumulatedResponse = StringBuilder()
    
    com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper.runInference(
        model = model,
        input = prompt,
        resultListener = { partialResult, done ->
            if (partialResult.isNotEmpty()) {
                accumulatedResponse.append(partialResult)
            }
            
            if (done) {
                CoroutineScope(Dispatchers.Main).launch {
                    val finalResponse = accumulatedResponse.toString().trim()
                    if (finalResponse.isNotEmpty()) {
                        chatViewModel.sendMessage("🤖 AI Analysis Result (${roleDisplayName}):\n\n$finalResponse")
                    } else {
                        chatViewModel.sendMessage("🤖 AI generated an empty response. Please try rephrasing your request.")
                    }
                }
            }
        },
        cleanUpListener = {
            // Cleanup completed
        }
    )
}