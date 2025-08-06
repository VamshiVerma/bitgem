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

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.common.getTaskBgColor
import com.google.ai.edge.gallery.ui.theme.titleMediumNarrow

@Composable
fun GalleryStyleActionCard(
    icon: ImageVector,
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Use Gallery's flat design - no cards, just colored backgrounds
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) {
                    when (label) {
                        "AI Analyze" -> Brush.linearGradient(
                            colors = listOf(Color(0xFF6C5CE7), Color(0xFF00B894))
                        )
                        "Request Supply" -> Brush.linearGradient(
                            colors = listOf(Color(0xFF00B894), Color(0xFF00CEC9))
                        )
                        "Emergency Alert" -> Brush.linearGradient(
                            colors = listOf(Color(0xFFE17055), Color(0xFFD63031))
                        )
                        "Show Status" -> Brush.linearGradient(
                            colors = listOf(Color(0xFFFD79A8), Color(0xFFE84393))
                        )
                        else -> Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.8f),
                                color.copy(alpha = 0.9f)
                            )
                        )
                    }
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            )
            .clickable(enabled = enabled) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clean icon without background circle
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Title with Gallery typography
            Text(
                text = label,
                color = Color.White,
                style = titleMediumNarrow.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            // Description
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun GalleryStyleSwarmMemberCard(
    member: SwarmMember,
    onAssignRole: (SwarmRole) -> Unit,
    modifier: Modifier = Modifier
) {
    // Vibrant member cards with proper gradients
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (member.isMe) {
                    Brush.linearGradient(
                        colors = when (member.role) {
                            SwarmRole.MEDIC -> listOf(Color(0xFFE17055), Color(0xFFD63031))
                            SwarmRole.SCOUT -> listOf(Color(0xFF00B894), Color(0xFF00A085))
                            SwarmRole.LEADER -> listOf(Color(0xFFFD79A8), Color(0xFFE84393))
                            SwarmRole.HELPER -> listOf(Color(0xFF74B9FF), Color(0xFF0984E3))
                            SwarmRole.ANALYST -> listOf(Color(0xFFA29BFE), Color(0xFF6C5CE7))
                            SwarmRole.UNASSIGNED -> listOf(Color(0xFF00BCD4), Color(0xFF0097A7))
                        }
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            )
            .clickable { if (!member.isMe) onAssignRole(SwarmRole.UNASSIGNED) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Clean role icon without circle background
            Icon(
                imageVector = member.role.icon,
                contentDescription = null,
                tint = if (member.isMe) Color.White else member.role.color,
                modifier = Modifier.size(24.dp)
            )
            
            // Name with Gallery typography
            Text(
                text = member.nickname,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (member.isMe) Color.White else MaterialTheme.colorScheme.onSurface
            )
            
            // Role with Gallery styling
            Text(
                text = member.role.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = if (member.isMe) Color.White.copy(alpha = 0.9f) else member.role.color,
                fontWeight = FontWeight.Medium
            )
            
            // Clean status indicators - professional text
            if (member.hasAI || member.isProcessing) {
                Text(
                    text = when {
                        member.hasAI && member.isProcessing -> "AI • Processing"
                        member.hasAI -> "AI"
                        member.isProcessing -> "Processing"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (member.isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun GalleryStyleStatusCard(
    currentRole: SwarmRole,
    isConnected: Boolean,
    aiModeEnabled: Boolean,
    memberCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF74B9FF), Color(0xFF0984E3))
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Emergency Network",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (currentRole != SwarmRole.UNASSIGNED) {
                            "Active as ${currentRole.displayName}"
                        } else {
                            "Choose your role to start"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isConnected) "🟢 ONLINE" else "🔴 OFFLINE",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (aiModeEnabled) {
                        Text(
                            text = "AI ACTIVE",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            // Clean status indicators
            Text(
                text = "$memberCount Connected" + 
                    if (currentRole != SwarmRole.UNASSIGNED) " • ${currentRole.displayName} Mode" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}