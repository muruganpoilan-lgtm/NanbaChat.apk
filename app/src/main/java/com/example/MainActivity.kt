package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.UserScreenTab
import com.example.ui.VoiceClubViewModel
import com.example.ui.components.AppMode
import com.example.ui.components.AuthOtpDialog
import com.example.ui.components.AuthRole
import com.example.ui.components.HeaderBar
import com.example.ui.screens.CallHistoryScreen
import com.example.ui.screens.CallScreen
import com.example.ui.screens.PartnerDashboardScreen
import com.example.ui.screens.UserHomeScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoiceCyanAccent
import com.example.ui.theme.VoiceVioletLight
import com.example.ui.theme.VoiceVioletPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VoiceClubApp()
            }
        }
    }
}

@Composable
fun VoiceClubApp(
    viewModel: VoiceClubViewModel = viewModel()
) {
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val userTab by viewModel.userTab.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    val partners by viewModel.partners.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()

    val userWallet by viewModel.userWallet.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val userCallLogs by viewModel.userCallLogs.collectAsStateWithLifecycle()

    val partnerProfile by viewModel.partnerProfile.collectAsStateWithLifecycle()
    val partnerWallet by viewModel.partnerWallet.collectAsStateWithLifecycle()
    val partnerLanguages by viewModel.partnerLanguages.collectAsStateWithLifecycle()

    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val callSummary by viewModel.callSummary.collectAsStateWithLifecycle()
    val matchState by viewModel.matchState.collectAsStateWithLifecycle()
    val incomingHostCall by viewModel.incomingHostCall.collectAsStateWithLifecycle()
    val isTtsSpeaking by viewModel.audioManager.isSpeaking.collectAsStateWithLifecycle()
    var showAuthDialog by remember { mutableStateOf(false) }

    // Handle back button
    BackHandler(enabled = activeCall != null || userTab != UserScreenTab.DISCOVER) {
        if (activeCall != null) {
            viewModel.endCall()
        } else if (userTab != UserScreenTab.DISCOVER) {
            viewModel.setUserTab(UserScreenTab.DISCOVER)
        }
    }

    // If an active call is ongoing, display the CallScreen full-frame
    if (activeCall != null) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            CallScreen(
                callData = activeCall!!,
                callSummary = callSummary,
                isTtsSpeaking = isTtsSpeaking,
                onSendMessage = { viewModel.sendMessageInCall(it) },
                onToggleMute = { viewModel.toggleMute() },
                onToggleSpeaker = { viewModel.toggleSpeaker() },
                onEndCall = { viewModel.endCall() },
                onDismissSummary = { viewModel.dismissCallSummary() },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    // Normal App Scaffold
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HeaderBar(
                currentMode = appMode,
                onModeChange = { viewModel.setAppMode(it) },
                userBalance = userWallet?.balance ?: 0.0,
                onWalletClick = {
                    viewModel.setAppMode(AppMode.USER)
                    viewModel.setUserTab(UserScreenTab.WALLET)
                },
                onAuthClick = { showAuthDialog = true }
            )
        },
        bottomBar = {
            if (appMode == AppMode.USER) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = userTab == UserScreenTab.DISCOVER,
                        onClick = { viewModel.setUserTab(UserScreenTab.DISCOVER) },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
                        label = { Text("Discover") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoiceVioletPrimary,
                            indicatorColor = VoiceVioletPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_discover")
                    )

                    NavigationBarItem(
                        selected = userTab == UserScreenTab.WALLET,
                        onClick = { viewModel.setUserTab(UserScreenTab.WALLET) },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
                        label = { Text("Wallet") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoiceVioletPrimary,
                            indicatorColor = VoiceVioletPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_wallet")
                    )

                    NavigationBarItem(
                        selected = userTab == UserScreenTab.HISTORY,
                        onClick = { viewModel.setUserTab(UserScreenTab.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoiceVioletPrimary,
                            indicatorColor = VoiceVioletPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_history")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = appMode, label = "ModeCrossfade") { mode ->
                when (mode) {
                    AppMode.USER -> {
                        when (userTab) {
                            UserScreenTab.DISCOVER -> {
                                UserHomeScreen(
                                    partners = partners,
                                    languages = languages.map { it.name },
                                    selectedLanguage = selectedLanguage,
                                    onLanguageSelected = { viewModel.selectLanguage(it) },
                                    matchState = matchState,
                                    onStartMatchmaking = { viewModel.startMatchmaking() },
                                    onDismissMatch = { viewModel.clearMatchState() },
                                    onInitiateCall = { viewModel.initiateCall(it) }
                                )
                            }
                            UserScreenTab.WALLET -> {
                                WalletScreen(
                                    wallet = userWallet,
                                    transactions = userTransactions,
                                    onTopUp = { viewModel.topUpUserWallet(it) }
                                )
                            }
                            UserScreenTab.HISTORY -> {
                                CallHistoryScreen(callLogs = userCallLogs)
                            }
                        }
                    }
                    AppMode.PARTNER -> {
                        PartnerDashboardScreen(
                            partnerProfile = partnerProfile,
                            partnerWallet = partnerWallet,
                            allLanguages = languages,
                            partnerLanguages = partnerLanguages,
                            incomingCall = incomingHostCall,
                            onUpdateStatus = { viewModel.updateHostStatus(it) },
                            onUpdateProfile = { bio, rate -> viewModel.updateHostProfile(bio, rate) },
                            onToggleLanguage = { viewModel.toggleHostLanguage(it) },
                            onSimulateIncomingCall = { viewModel.simulateIncomingCallForHost() },
                            onAcceptIncomingCall = { viewModel.acceptIncomingCall() },
                            onRejectIncomingCall = { viewModel.rejectIncomingCall() },
                            onRequestPayout = { viewModel.requestPayout(it) }
                        )
                    }
                }
            }
        }
    }

    if (showAuthDialog) {
        AuthOtpDialog(
            initialRole = if (appMode == AppMode.USER) AuthRole.USER else AuthRole.PARTNER,
            onDismiss = { showAuthDialog = false },
            onAuthenticated = { role, phone, token ->
                showAuthDialog = false
                if (role == AuthRole.PARTNER) {
                    viewModel.setAppMode(AppMode.PARTNER)
                } else {
                    viewModel.setAppMode(AppMode.USER)
                }
            }
        )
    }
}
