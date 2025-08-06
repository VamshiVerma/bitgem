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

package com.google.ai.edge.gallery.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.ai.edge.gallery.ui.ViewModelProvider
import com.google.ai.edge.gallery.ui.home.HomeScreen
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModel
import com.google.ai.edge.gallery.ui.imagegeneration.ImageGenerationViewModel
import com.google.ai.edge.gallery.ui.textclassification.TextClassificationViewModel
// import com.google.ai.edge.gallery.ui.bridge.GalleryBitchatBridge // temporarily disabled
import com.bitchat.android.ui.ChatScreen
import com.bitchat.android.ui.ChatViewModel
import com.bitchat.android.mesh.BluetoothMeshService
import com.google.ai.edge.gallery.ui.swarm.SwarmScreen
import androidx.lifecycle.ViewModelProvider as AndroidViewModelProvider

sealed class MainTab(val title: String, val icon: @Composable () -> Unit) {
    object BitChat : MainTab("BitGem", { 
        Icon(Icons.Default.Chat, contentDescription = "BitGem") 
    })
    object Swarm : MainTab("Swarm", { 
        Icon(Icons.Default.Group, contentDescription = "AI Swarm") 
    })
    object Settings : MainTab("Settings", { 
        Icon(Icons.Default.Settings, contentDescription = "Settings") 
    })
}

@Composable
fun MainNavigationScreen(
    navController: NavHostController,
    meshService: BluetoothMeshService,
    modelManagerViewModel: ModelManagerViewModel = viewModel(factory = ViewModelProvider.Factory),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf<MainTab>(MainTab.BitChat) }
    val context = LocalContext.current
    
    val chatViewModel: ChatViewModel = viewModel(
        factory = object : AndroidViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(context.applicationContext as android.app.Application, meshService) as T
            }
        }
    )
    
    // Gallery AI ViewModels
    val llmChatViewModel: LlmChatViewModel = viewModel(factory = ViewModelProvider.Factory)
    val imageGenViewModel: ImageGenerationViewModel = viewModel(factory = ViewModelProvider.Factory)
    val textClassViewModel: TextClassificationViewModel = viewModel(factory = ViewModelProvider.Factory)
    
    // Initialize the bridge between Gallery AI and BitChat
    LaunchedEffect(Unit) {
        // Wait a bit for ViewModels to be fully ready
        kotlinx.coroutines.delay(1000)
        
        // Bridge temporarily disabled due to compilation issues
        // Will re-enable after fixing structural problems
    }
    
    Scaffold(
        bottomBar = {
            // Simplified NavigationBar - no custom height to prevent cutoff
            NavigationBar {
                NavigationBarItem(
                    icon = MainTab.BitChat.icon,
                    label = { Text(MainTab.BitChat.title) },
                    selected = selectedTab == MainTab.BitChat,
                    onClick = { selectedTab = MainTab.BitChat }
                )
                NavigationBarItem(
                    icon = MainTab.Swarm.icon,
                    label = { Text(MainTab.Swarm.title) },
                    selected = selectedTab == MainTab.Swarm,
                    onClick = { selectedTab = MainTab.Swarm }
                )
                NavigationBarItem(
                    icon = MainTab.Settings.icon,
                    label = { Text(MainTab.Settings.title) },
                    selected = selectedTab == MainTab.Settings,
                    onClick = { selectedTab = MainTab.Settings }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        when (selectedTab) {
            MainTab.BitChat -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    ChatScreen(viewModel = chatViewModel)
                }
            }
            MainTab.Swarm -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    SwarmScreen(
                        chatViewModel = chatViewModel,
                        llmViewModel = llmChatViewModel,
                        imageGenViewModel = imageGenViewModel,
                        modelManagerViewModel = modelManagerViewModel,
                        onNavigateToBitChat = { 
                            selectedTab = MainTab.BitChat 
                        }
                    )
                }
            }
            MainTab.Settings -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    GalleryNavHost(
                        navController = navController,
                        modelManagerViewModel = modelManagerViewModel
                    )
                }
            }
        }
    }
}