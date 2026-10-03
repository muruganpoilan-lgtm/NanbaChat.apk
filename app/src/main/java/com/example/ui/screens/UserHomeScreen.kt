package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Partner
import com.example.ui.MatchState
import com.example.ui.components.CircularRadarPulse
import com.example.ui.components.LanguageSelectorChips
import com.example.ui.components.PartnerCard
import com.example.ui.theme.VoiceCoralAccent
import com.example.ui.theme.VoiceCyanAccent
import com.example.ui.theme.VoiceGoldAccent
import com.example.ui.theme.VoiceVioletDark
import com.example.ui.theme.VoiceVioletLight
import com.example.ui.theme.VoiceVioletPrimary

@Composable
fun UserHomeScreen(
    partners: List<Partner>,
    languages: List<String>,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    matchState: MatchState,
    onStartMatchmaking: () -> Unit,
    onDismissMatch: () -> Unit,
    onInitiateCall: (Partner) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("user_home_screen"),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Hero Matchmaking Radar Section
            item {
                QuickMatchHeroCard(
                    selectedLanguage = selectedLanguage,
                    matchState = matchState,
                    onStartMatchmaking = onStartMatchmaking
                )
            }

            // Language Selector Chips
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = VoiceVioletLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preferred Language",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LanguageSelectorChips(
                    languages = languages,
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }

            // Available Hosts Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Online Hosts & Experts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${partners.count { it.status == "online" }} Available",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = VoiceCyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Host Cards List
            if (partners.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hosts found for $selectedLanguage. Try another language!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(partners, key = { it.id }) { partner ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        PartnerCard(
                            partner = partner,
                            languages = listOf(selectedLanguage, "English"),
                            onCallClick = { onInitiateCall(partner) }
                        )
                    }
                }
            }
        }

        // Match Found Dialog
        if (matchState is MatchState.Found) {
            val matchedPartner = matchState.partner
            AlertDialog(
                onDismissRequest = onDismissMatch,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = VoiceGoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Best Match Found!",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Matched using NanbaChat SQL matchmaking query based on your preferred language: $selectedLanguage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PartnerCard(
                            partner = matchedPartner,
                            languages = listOf(selectedLanguage),
                            onCallClick = {
                                onDismissMatch()
                                onInitiateCall(matchedPartner)
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDismissMatch()
                            onInitiateCall(matchedPartner)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VoiceVioletPrimary),
                        modifier = Modifier.testTag("connect_matched_call_button")
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Now")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismissMatch) {
                        Text("Cancel")
                    }
                }
            )
        }

        // No Match Dialog
        if (matchState is MatchState.NoMatch) {
            AlertDialog(
                onDismissRequest = onDismissMatch,
                title = { Text("No Immediate Match", fontWeight = FontWeight.Bold) },
                text = { Text(matchState.reason) },
                confirmButton = {
                    Button(onClick = onDismissMatch) {
                        Text("Got it")
                    }
                }
            )
        }
    }
}

@Composable
fun QuickMatchHeroCard(
    selectedLanguage: String,
    matchState: MatchState,
    onStartMatchmaking: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("quick_match_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VoiceVioletDark.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surfaceVariant
                        ),
                        radius = 600f
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VoiceVioletPrimary.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = VoiceCyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Instant 1-Click Matchmaker",
                            style = MaterialTheme.typography.labelSmall,
                            color = VoiceCyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Connect Anonymously in $selectedLanguage",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "High-rated hosts ready for voice talk, language practice & real human connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive Radar & Match Button
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isSearching = matchState is MatchState.Searching
                    CircularRadarPulse(
                        isSearching = isSearching,
                        size = 140.dp
                    )

                    Button(
                        onClick = onStartMatchmaking,
                        enabled = !isSearching,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoiceVioletPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(96.dp)
                            .testTag("start_quick_match_button"),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Match",
                                    modifier = Modifier.size(28.dp),
                                    tint = VoiceCyanAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "MATCH",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (matchState is MatchState.Searching) "Running SQL Matchmaker..." else "Tap to find best available host",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (matchState is MatchState.Searching) VoiceCyanAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Infrastructure Telemetry Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(com.example.ui.theme.StatusOnlineGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Redis Pub/Sub Scaled • WebRTC P2P Direct Audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
