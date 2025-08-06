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

package com.google.ai.edge.gallery

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.rememberNavController
import com.google.ai.edge.gallery.ui.navigation.MainNavigationScreen
import com.google.ai.edge.gallery.ui.theme.GalleryTheme
import com.bitchat.android.MainViewModel
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.onboarding.BluetoothCheckScreen
import com.bitchat.android.onboarding.BluetoothStatus
import com.bitchat.android.onboarding.BluetoothStatusManager
import com.bitchat.android.onboarding.BatteryOptimizationManager
import com.bitchat.android.onboarding.BatteryOptimizationScreen
import com.bitchat.android.onboarding.BatteryOptimizationStatus
import com.bitchat.android.onboarding.InitializationErrorScreen
import com.bitchat.android.onboarding.InitializingScreen
import com.bitchat.android.onboarding.LocationCheckScreen
import com.bitchat.android.onboarding.LocationStatus
import com.bitchat.android.onboarding.LocationStatusManager
import com.bitchat.android.onboarding.OnboardingCoordinator
import com.bitchat.android.onboarding.OnboardingState
import com.bitchat.android.onboarding.PermissionExplanationScreen
import com.bitchat.android.onboarding.PermissionManager
import com.bitchat.android.ui.ChatViewModel
import com.bitchat.android.ui.theme.BitchatTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IntegratedMainActivity : ComponentActivity() {
    
    private lateinit var permissionManager: PermissionManager
    private lateinit var onboardingCoordinator: OnboardingCoordinator
    private lateinit var bluetoothStatusManager: BluetoothStatusManager
    private lateinit var locationStatusManager: LocationStatusManager
    private lateinit var batteryOptimizationManager: BatteryOptimizationManager
    
    // Core mesh service - managed at app level
    private lateinit var meshService: BluetoothMeshService
    private val mainViewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels { 
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(application, meshService) as T
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize core mesh service first
        meshService = BluetoothMeshService(this)
        
        // Initialize permission management
        permissionManager = PermissionManager(this)
        bluetoothStatusManager = BluetoothStatusManager(
            activity = this,
            context = this,
            onBluetoothEnabled = ::handleBluetoothEnabled,
            onBluetoothDisabled = ::handleBluetoothDisabled
        )
        locationStatusManager = LocationStatusManager(
            activity = this,
            context = this,
            onLocationEnabled = ::handleLocationEnabled,
            onLocationDisabled = ::handleLocationDisabled
        )
        batteryOptimizationManager = BatteryOptimizationManager(
            activity = this,
            context = this,
            onBatteryOptimizationDisabled = ::handleBatteryOptimizationDisabled,
            onBatteryOptimizationFailed = ::handleBatteryOptimizationFailed
        )
        onboardingCoordinator = OnboardingCoordinator(
            activity = this,
            permissionManager = permissionManager,
            onOnboardingComplete = ::handleOnboardingComplete,
            onOnboardingFailed = ::handleOnboardingFailed
        )
        
        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IntegratedApp()
                }
            }
        }
        
        // Collect state changes in a lifecycle-aware manner
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.onboardingState.collect { state ->
                    handleOnboardingStateChange(state)
                }
            }
        }
        
        // Only start onboarding process if we're in the initial CHECKING state
        // This prevents restarting onboarding on configuration changes
        if (mainViewModel.onboardingState.value == OnboardingState.CHECKING) {
            checkOnboardingStatus()
        }
    }
    
    @Composable
    private fun IntegratedApp() {
        val onboardingState by mainViewModel.onboardingState.collectAsState()
        val bluetoothStatus by mainViewModel.bluetoothStatus.collectAsState()
        val locationStatus by mainViewModel.locationStatus.collectAsState()
        val batteryOptimizationStatus by mainViewModel.batteryOptimizationStatus.collectAsState()
        val errorMessage by mainViewModel.errorMessage.collectAsState()
        val isBluetoothLoading by mainViewModel.isBluetoothLoading.collectAsState()
        val isLocationLoading by mainViewModel.isLocationLoading.collectAsState()
        val isBatteryOptimizationLoading by mainViewModel.isBatteryOptimizationLoading.collectAsState()
        
        when (onboardingState) {
            OnboardingState.CHECKING -> {
                InitializingScreen()
            }
            
            OnboardingState.BLUETOOTH_CHECK -> {
                BluetoothCheckScreen(
                    status = bluetoothStatus,
                    onEnableBluetooth = {
                        mainViewModel.updateBluetoothLoading(true)
                        bluetoothStatusManager.requestEnableBluetooth()
                    },
                    onRetry = {
                        checkBluetoothAndProceed()
                    },
                    isLoading = isBluetoothLoading
                )
            }
            
            OnboardingState.LOCATION_CHECK -> {
                LocationCheckScreen(
                    status = locationStatus,
                    onEnableLocation = {
                        mainViewModel.updateLocationLoading(true)
                        locationStatusManager.requestEnableLocation()
                    },
                    onRetry = {
                        checkLocationAndProceed()
                    },
                    isLoading = isLocationLoading
                )
            }
            
            OnboardingState.BATTERY_OPTIMIZATION_CHECK -> {
                BatteryOptimizationScreen(
                    status = batteryOptimizationStatus,
                    onDisableBatteryOptimization = {
                        mainViewModel.updateBatteryOptimizationLoading(true)
                        batteryOptimizationManager.requestDisableBatteryOptimization()
                    },
                    onRetry = {
                        checkBatteryOptimizationAndProceed()
                    },
                    onSkip = {
                        // Skip battery optimization and proceed
                        proceedWithPermissionCheck()
                    },
                    isLoading = isBatteryOptimizationLoading
                )
            }
            
            OnboardingState.PERMISSION_EXPLANATION -> {
                PermissionExplanationScreen(
                    permissionCategories = permissionManager.getCategorizedPermissions(),
                    onContinue = {
                        mainViewModel.updateOnboardingState(OnboardingState.PERMISSION_REQUESTING)
                        onboardingCoordinator.requestPermissions()
                    }
                )
            }
            
            OnboardingState.PERMISSION_REQUESTING -> {
                InitializingScreen()
            }
            
            OnboardingState.INITIALIZING -> {
                InitializingScreen()
            }
            
            OnboardingState.COMPLETE -> {
                // Set up back navigation handling for the integrated app
                val backCallback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        // Let ChatViewModel handle navigation state
                        val handled = chatViewModel.handleBackPressed()
                        if (!handled) {
                            // If ChatViewModel doesn't handle it, disable this callback 
                            // and let the system handle it (which will exit the app)
                            this.isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                            this.isEnabled = true
                        }
                    }
                }
                
                // Add the callback - this will be automatically removed when the activity is destroyed
                onBackPressedDispatcher.addCallback(this@IntegratedMainActivity, backCallback)
                
                val navController = rememberNavController()
                MainNavigationScreen(
                    navController = navController,
                    meshService = meshService
                )
            }
            
            OnboardingState.ERROR -> {
                InitializationErrorScreen(
                    errorMessage = errorMessage,
                    onRetry = {
                        mainViewModel.updateOnboardingState(OnboardingState.CHECKING)
                        checkOnboardingStatus()
                    },
                    onOpenSettings = {
                        onboardingCoordinator.openAppSettings()
                    }
                )
            }
        }
    }
    
    // Implement all the BitChat onboarding methods from the original MainActivity
    // (This is a lot of code, so I'm including the key methods)
    
    private fun handleOnboardingStateChange(state: OnboardingState) {
        when (state) {
            OnboardingState.COMPLETE -> {
                Log.d("IntegratedMainActivity", "Onboarding completed - app ready")
            }
            OnboardingState.ERROR -> {
                Log.e("IntegratedMainActivity", "Onboarding error state reached")
            }
            else -> {}
        }
    }
    
    private fun checkOnboardingStatus() {
        Log.d("IntegratedMainActivity", "Checking onboarding status")
        
        lifecycleScope.launch {
            // Small delay to show the checking state
            delay(500)
            
            // First check Bluetooth status (always required)
            checkBluetoothAndProceed()
        }
    }
    
    private fun checkBluetoothAndProceed() {
        // For first-time users, skip Bluetooth check and go straight to permissions
        if (permissionManager.isFirstTimeLaunch()) {
            Log.d("IntegratedMainActivity", "First-time launch, skipping Bluetooth check - will check after permissions")
            proceedWithPermissionCheck()
            return
        }
        
        // For existing users, check Bluetooth status first
        bluetoothStatusManager.logBluetoothStatus()
        mainViewModel.updateBluetoothStatus(bluetoothStatusManager.checkBluetoothStatus())
        
        when (mainViewModel.bluetoothStatus.value) {
            BluetoothStatus.ENABLED -> {
                checkLocationAndProceed()
            }
            BluetoothStatus.DISABLED -> {
                Log.d("IntegratedMainActivity", "Bluetooth disabled, showing enable screen")
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
            }
            BluetoothStatus.NOT_SUPPORTED -> {
                Log.e("IntegratedMainActivity", "Bluetooth not supported")
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
            }
        }
    }
    
    private fun checkLocationAndProceed() {
        Log.d("IntegratedMainActivity", "Checking location services status")
        
        if (permissionManager.isFirstTimeLaunch()) {
            Log.d("IntegratedMainActivity", "First-time launch, skipping location check - will check after permissions")
            proceedWithPermissionCheck()
            return
        }
        
        locationStatusManager.logLocationStatus()
        mainViewModel.updateLocationStatus(locationStatusManager.checkLocationStatus())
        
        when (mainViewModel.locationStatus.value) {
            LocationStatus.ENABLED -> {
                checkBatteryOptimizationAndProceed()
            }
            LocationStatus.DISABLED -> {
                Log.d("IntegratedMainActivity", "Location services disabled, showing enable screen")
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
            LocationStatus.NOT_AVAILABLE -> {
                Log.e("IntegratedMainActivity", "Location services not available")
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
        }
    }
    
    private fun checkBatteryOptimizationAndProceed() {
        Log.d("IntegratedMainActivity", "Checking battery optimization status")
        
        if (permissionManager.isFirstTimeLaunch()) {
            Log.d("IntegratedMainActivity", "First-time launch, skipping battery optimization check - will check after permissions")
            proceedWithPermissionCheck()
            return
        }
        
        batteryOptimizationManager.logBatteryOptimizationStatus()
        val currentBatteryOptimizationStatus = when {
            !batteryOptimizationManager.isBatteryOptimizationSupported() -> BatteryOptimizationStatus.NOT_SUPPORTED
            batteryOptimizationManager.isBatteryOptimizationDisabled() -> BatteryOptimizationStatus.DISABLED
            else -> BatteryOptimizationStatus.ENABLED
        }
        mainViewModel.updateBatteryOptimizationStatus(currentBatteryOptimizationStatus)
        
        when (currentBatteryOptimizationStatus) {
            BatteryOptimizationStatus.DISABLED, BatteryOptimizationStatus.NOT_SUPPORTED -> {
                proceedWithPermissionCheck()
            }
            BatteryOptimizationStatus.ENABLED -> {
                Log.d("IntegratedMainActivity", "Battery optimization enabled, showing disable screen")
                mainViewModel.updateOnboardingState(OnboardingState.BATTERY_OPTIMIZATION_CHECK)
                mainViewModel.updateBatteryOptimizationLoading(false)
            }
        }
    }
    
    private fun proceedWithPermissionCheck() {
        Log.d("IntegratedMainActivity", "Proceeding with permission check")
        
        lifecycleScope.launch {
            delay(200) // Small delay for smooth transition
            
            if (permissionManager.isFirstTimeLaunch()) {
                Log.d("IntegratedMainActivity", "First time launch, showing permission explanation")
                mainViewModel.updateOnboardingState(OnboardingState.PERMISSION_EXPLANATION)
            } else if (permissionManager.areAllPermissionsGranted()) {
                Log.d("IntegratedMainActivity", "Existing user with permissions, initializing app")
                mainViewModel.updateOnboardingState(OnboardingState.INITIALIZING)
                initializeApp()
            } else {
                Log.d("IntegratedMainActivity", "Existing user missing permissions, showing explanation")
                mainViewModel.updateOnboardingState(OnboardingState.PERMISSION_EXPLANATION)
            }
        }
    }
    
    private fun handleBluetoothEnabled() {
        Log.d("IntegratedMainActivity", "Bluetooth enabled by user")
        mainViewModel.updateBluetoothLoading(false)
        mainViewModel.updateBluetoothStatus(BluetoothStatus.ENABLED)
        checkLocationAndProceed()
    }
    
    private fun handleBluetoothDisabled(message: String) {
        Log.w("IntegratedMainActivity", "Bluetooth disabled or failed: $message")
        mainViewModel.updateBluetoothLoading(false)
        mainViewModel.updateBluetoothStatus(bluetoothStatusManager.checkBluetoothStatus())
        
        when {
            mainViewModel.bluetoothStatus.value == BluetoothStatus.NOT_SUPPORTED -> {
                mainViewModel.updateErrorMessage(message)
                mainViewModel.updateOnboardingState(OnboardingState.ERROR)
            }
            message.contains("Permission") && permissionManager.isFirstTimeLaunch() -> {
                Log.d("IntegratedMainActivity", "Bluetooth enable requires permissions, proceeding to permission explanation")
                proceedWithPermissionCheck()
            }
            message.contains("Permission") -> {
                Log.d("IntegratedMainActivity", "Bluetooth enable requires permissions, showing permission explanation")
                mainViewModel.updateOnboardingState(OnboardingState.PERMISSION_EXPLANATION)
            }
            else -> {
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
            }
        }
    }
    
    private fun handleLocationEnabled() {
        Log.d("IntegratedMainActivity", "Location services enabled by user")
        mainViewModel.updateLocationLoading(false)
        mainViewModel.updateLocationStatus(LocationStatus.ENABLED)
        checkBatteryOptimizationAndProceed()
    }
    
    private fun handleLocationDisabled(message: String) {
        Log.w("IntegratedMainActivity", "Location services disabled or failed: $message")
        mainViewModel.updateLocationLoading(false)
        mainViewModel.updateLocationStatus(locationStatusManager.checkLocationStatus())

        when {
            mainViewModel.locationStatus.value == LocationStatus.NOT_AVAILABLE -> {
                mainViewModel.updateErrorMessage(message)
                mainViewModel.updateOnboardingState(OnboardingState.ERROR)
            }
            else -> {
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
            }
        }
    }
    
    private fun handleBatteryOptimizationDisabled() {
        Log.d("IntegratedMainActivity", "Battery optimization disabled by user")
        mainViewModel.updateBatteryOptimizationLoading(false)
        mainViewModel.updateBatteryOptimizationStatus(BatteryOptimizationStatus.DISABLED)
        proceedWithPermissionCheck()
    }
    
    private fun handleBatteryOptimizationFailed(message: String) {
        Log.w("IntegratedMainActivity", "Battery optimization disable failed: $message")
        mainViewModel.updateBatteryOptimizationLoading(false)
        val currentStatus = when {
            !batteryOptimizationManager.isBatteryOptimizationSupported() -> BatteryOptimizationStatus.NOT_SUPPORTED
            batteryOptimizationManager.isBatteryOptimizationDisabled() -> BatteryOptimizationStatus.DISABLED
            else -> BatteryOptimizationStatus.ENABLED
        }
        mainViewModel.updateBatteryOptimizationStatus(currentStatus)
        mainViewModel.updateOnboardingState(OnboardingState.BATTERY_OPTIMIZATION_CHECK)
    }
    
    private fun handleOnboardingComplete() {
        Log.d("IntegratedMainActivity", "Onboarding completed, checking Bluetooth and Location before initializing app")
        
        val currentBluetoothStatus = bluetoothStatusManager.checkBluetoothStatus()
        val currentLocationStatus = locationStatusManager.checkLocationStatus()
        val currentBatteryOptimizationStatus = when {
            !batteryOptimizationManager.isBatteryOptimizationSupported() -> BatteryOptimizationStatus.NOT_SUPPORTED
            batteryOptimizationManager.isBatteryOptimizationDisabled() -> BatteryOptimizationStatus.DISABLED
            else -> BatteryOptimizationStatus.ENABLED
        }
        
        when {
            currentBluetoothStatus != BluetoothStatus.ENABLED -> {
                Log.d("IntegratedMainActivity", "Permissions granted, but Bluetooth still disabled. Showing Bluetooth enable screen.")
                mainViewModel.updateBluetoothStatus(currentBluetoothStatus)
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
            }
            currentLocationStatus != LocationStatus.ENABLED -> {
                Log.d("IntegratedMainActivity", "Permissions granted, but Location services still disabled. Showing Location enable screen.")
                mainViewModel.updateLocationStatus(currentLocationStatus)
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
            currentBatteryOptimizationStatus == BatteryOptimizationStatus.ENABLED -> {
                Log.d("IntegratedMainActivity", "Permissions granted, but battery optimization still enabled. Showing battery optimization screen.")
                mainViewModel.updateBatteryOptimizationStatus(currentBatteryOptimizationStatus)
                mainViewModel.updateOnboardingState(OnboardingState.BATTERY_OPTIMIZATION_CHECK)
                mainViewModel.updateBatteryOptimizationLoading(false)
            }
            else -> {
                Log.d("IntegratedMainActivity", "All requirements met, proceeding to initialization")
                mainViewModel.updateOnboardingState(OnboardingState.INITIALIZING)
                initializeApp()
            }
        }
    }
    
    private fun handleOnboardingFailed(message: String) {
        Log.e("IntegratedMainActivity", "Onboarding failed: $message")
        mainViewModel.updateErrorMessage(message)
        mainViewModel.updateOnboardingState(OnboardingState.ERROR)
    }
    
    private fun initializeApp() {
        Log.d("IntegratedMainActivity", "Starting app initialization")
        
        lifecycleScope.launch {
            try {
                delay(1000) // Give the system time to process permission grants
                
                Log.d("IntegratedMainActivity", "Permissions verified, initializing chat system")
                
                if (!permissionManager.areAllPermissionsGranted()) {
                    val missing = permissionManager.getMissingPermissions()
                    Log.w("IntegratedMainActivity", "Permissions revoked during initialization: $missing")
                    handleOnboardingFailed("Some permissions were revoked. Please grant all permissions to continue.")
                    return@launch
                }
                
                // Set up mesh service delegate and start services
                meshService.delegate = chatViewModel
                meshService.startServices()
                
                Log.d("IntegratedMainActivity", "Mesh service started successfully")
                
                // Handle any notification intent
                handleNotificationIntent(intent)
                
                delay(500)
                Log.d("IntegratedMainActivity", "App initialization complete")
                mainViewModel.updateOnboardingState(OnboardingState.COMPLETE)
            } catch (e: Exception) {
                Log.e("IntegratedMainActivity", "Failed to initialize app", e)
                handleOnboardingFailed("Failed to initialize the app: ${e.message}")
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            handleNotificationIntent(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            meshService.connectionManager.setAppBackgroundState(false)
            chatViewModel.setAppBackgroundState(false)

            val currentBluetoothStatus = bluetoothStatusManager.checkBluetoothStatus()
            if (currentBluetoothStatus != BluetoothStatus.ENABLED) {
                Log.w("IntegratedMainActivity", "Bluetooth disabled while app was backgrounded")
                mainViewModel.updateBluetoothStatus(currentBluetoothStatus)
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
                return
            }
            
            val currentLocationStatus = locationStatusManager.checkLocationStatus()
            if (currentLocationStatus != LocationStatus.ENABLED) {
                Log.w("IntegratedMainActivity", "Location services disabled while app was backgrounded")
                mainViewModel.updateLocationStatus(currentLocationStatus)
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            meshService.connectionManager.setAppBackgroundState(true)
            chatViewModel.setAppBackgroundState(true)
        }
    }
    
    private fun handleNotificationIntent(intent: Intent) {
        val shouldOpenPrivateChat = intent.getBooleanExtra(
            com.bitchat.android.ui.NotificationManager.EXTRA_OPEN_PRIVATE_CHAT, 
            false
        )
        
        if (shouldOpenPrivateChat) {
            val peerID = intent.getStringExtra(com.bitchat.android.ui.NotificationManager.EXTRA_PEER_ID)
            val senderNickname = intent.getStringExtra(com.bitchat.android.ui.NotificationManager.EXTRA_SENDER_NICKNAME)
            
            if (peerID != null) {
                Log.d("IntegratedMainActivity", "Opening private chat with $senderNickname (peerID: $peerID) from notification")
                
                chatViewModel.startPrivateChat(peerID)
                chatViewModel.clearNotificationsForSender(peerID)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        try {
            locationStatusManager.cleanup()
            Log.d("IntegratedMainActivity", "Location status manager cleaned up successfully")
        } catch (e: Exception) {
            Log.w("IntegratedMainActivity", "Error cleaning up location status manager: ${e.message}")
        }
        
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            try {
                meshService.stopServices()
                Log.d("IntegratedMainActivity", "Mesh services stopped successfully")
            } catch (e: Exception) {
                Log.w("IntegratedMainActivity", "Error stopping mesh services in onDestroy: ${e.message}")
            }
        }
    }
}