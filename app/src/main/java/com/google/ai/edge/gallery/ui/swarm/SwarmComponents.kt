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

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SwarmStatusCard(
    currentRole: SwarmRole,
    isConnected: Boolean,
    aiModeEnabled: Boolean,
    memberCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2537)
        ),
        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
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
                        text = "🚨 EMERGENCY STATUS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                    Text(
                        text = if (currentRole != SwarmRole.UNASSIGNED) {
                            "Active as ${currentRole.displayName}"
                        } else {
                            "⚠️ Choose your role to start"
                        },
                        fontSize = 16.sp,
                        color = if (currentRole != SwarmRole.UNASSIGNED) Color(0xFF00E676) else Color(0xFFFFAB00),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isConnected) "🟢 ONLINE" else "🔴 OFFLINE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF00E676) else Color(0xFFE53935)
                    )
                    if (aiModeEnabled) {
                        Text(
                            text = "🤖 AI ACTIVE",
                            fontSize = 10.sp,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusChip(
                    icon = Icons.Default.Group,
                    text = "$memberCount Connected",
                    color = Color(0xFF00E676)
                )
                
                if (currentRole != SwarmRole.UNASSIGNED) {
                    StatusChip(
                        icon = currentRole.icon,
                        text = "${currentRole.displayName} Mode",
                        color = currentRole.color
                    )
                }
            }
        }
    }
}

@Composable
fun SwarmFeatureGuide(
    currentRole: SwarmRole,
    hasAI: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "AI Swarm Intelligence",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Distributed Emergency Response Network",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            
            // Current Role Explanation
            RoleExplanationCard(
                title = "Your Current Role: ${currentRole.displayName}",
                role = currentRole,
                hasAI = hasAI
            )
            
            // How AI Mesh Works
            Text(
                text = "How AI Mesh Intelligence Works:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureBulletPoint(
                    icon = Icons.Default.AutoAwesome,
                    text = "AI agents share knowledge instantly across the mesh network",
                    color = MaterialTheme.colorScheme.primary
                )
                FeatureBulletPoint(
                    icon = Icons.Default.Hub,
                    text = "Each device contributes processing power for collective intelligence",
                    color = MaterialTheme.colorScheme.secondary
                )
                FeatureBulletPoint(
                    icon = Icons.Default.Speed,
                    text = "Context-aware responses based on your assigned role",
                    color = MaterialTheme.colorScheme.tertiary
                )
                FeatureBulletPoint(
                    icon = Icons.Default.CloudOff,
                    text = "Works completely offline using Bluetooth mesh networking",
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            // Role Assignment Benefits
            Text(
                text = "Why Role Assignment Matters:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureBulletPoint(
                    icon = Icons.Default.Tune,
                    text = "AI tailors responses to your specific expertise and needs",
                    color = MaterialTheme.colorScheme.primary
                )
                FeatureBulletPoint(
                    icon = Icons.Default.Group,
                    text = "Enables smart routing - supplies go to right people",
                    color = MaterialTheme.colorScheme.secondary
                )
                FeatureBulletPoint(
                    icon = Icons.Default.AccountTree,
                    text = "Creates coordination chains (Leader → Scout → Medic)",
                    color = MaterialTheme.colorScheme.tertiary
                )
                FeatureBulletPoint(
                    icon = Icons.Default.SmartToy,
                    text = "Activates role-specific AI analysis and suggestions",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun RoleExplanationCard(
    title: String,
    role: SwarmRole,
    hasAI: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = role.color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, role.color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(role.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = role.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = role.color
                )
            }
            
            Text(
                text = role.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Role-specific capabilities
            Text(
                text = "What you can do:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                getRoleCapabilities(role).forEach { capability ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = role.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = capability,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (hasAI) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "AI-ENHANCED: Your analysis requests will use specialized ${role.displayName} AI models",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureBulletPoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getRoleCapabilities(role: SwarmRole): List<String> = when (role) {
    SwarmRole.MEDIC -> listOf(
        "Receive priority medical supply requests",
        "Get AI-powered diagnosis assistance with symptoms/images", 
        "Access emergency medical protocols and drug interactions",
        "Coordinate with other medics for complex cases",
        "Provide triage guidance to non-medical responders"
    )
    SwarmRole.SCOUT -> listOf(
        "Report terrain conditions and hazards to the network",
        "Get AI analysis of images showing damage/obstacles",
        "Receive route optimization suggestions from AI",
        "Coordinate search patterns with other scouts",
        "Provide real-time situational awareness updates"
    )
    SwarmRole.LEADER -> listOf(
        "Access strategic AI analysis for resource allocation",
        "Coordinate between different role groups",
        "Receive priority status updates from all roles",
        "Make delegation decisions with AI recommendations", 
        "Monitor overall swarm network health and efficiency"
    )
    SwarmRole.HELPER -> listOf(
        "Receive supply requests from specialized roles",
        "Get AI guidance on logistics and resource distribution",
        "Support any role that needs additional manpower",
        "Coordinate supply chains with other helpers",
        "Provide backup communication relay services"
    )
    SwarmRole.ANALYST -> listOf(
        "Process complex data patterns with enhanced AI models",
        "Provide predictive analysis for the entire swarm",
        "Generate situation reports and risk assessments",
        "Coordinate AI resources across the network",
        "Analyze communication patterns for optimization"
    )
    SwarmRole.UNASSIGNED -> listOf(
        "Choose any role based on current situation needs",
        "Access basic AI assistance for general questions",
        "Receive broadcasts from all specialized roles",
        "Contribute to overall network resilience",
        "Ready to be assigned tasks by Leaders"
    )
}