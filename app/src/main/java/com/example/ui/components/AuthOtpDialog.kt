package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.VoiceCyanAccent
import com.example.ui.theme.VoiceGoldAccent
import com.example.ui.theme.VoiceVioletLight
import com.example.ui.theme.VoiceVioletPrimary
import kotlinx.coroutines.delay

enum class AuthRole {
    USER,
    PARTNER
}

@Composable
fun AuthOtpDialog(
    initialRole: AuthRole = AuthRole.USER,
    onDismiss: () -> Unit,
    onAuthenticated: (role: AuthRole, phone: String, token: String) -> Unit
) {
    var selectedRole by remember { mutableStateOf(initialRole) }
    var phoneNumber by remember {
        mutableStateOf(if (initialRole == AuthRole.USER) "+91 98765 43210" else "+91 91234 56789")
    }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var resendCountdown by remember { mutableIntStateOf(0) }
    var jwtToken by remember { mutableStateOf<String?>(null) }
    var isProfileComplete by remember { mutableStateOf(true) }

    // Onboarding form state for partners
    var partnerName by remember { mutableStateOf("P. Murugan") }
    var partnerRate by remember { mutableStateOf("5.00") }
    var showOnboardingStep by remember { mutableStateOf(false) }

    // Resend timer ticker
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000L)
            resendCountdown -= 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("auth_otp_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VoiceVioletPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = VoiceCyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Phone + OTP Authentication",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mobile-First Identity & Role JWT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Role Tabs: User vs Partner
                TabRow(
                    selectedTabIndex = if (selectedRole == AuthRole.USER) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedRole == AuthRole.USER,
                        onClick = {
                            selectedRole = AuthRole.USER
                            phoneNumber = "+91 98765 43210"
                            isOtpSent = false
                            otpCode = ""
                            jwtToken = null
                            showOnboardingStep = false
                        },
                        text = { Text("Customer Caller", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedRole == AuthRole.PARTNER,
                        onClick = {
                            selectedRole = AuthRole.PARTNER
                            phoneNumber = "+91 91234 56789"
                            isOtpSent = false
                            otpCode = ""
                            jwtToken = null
                            showOnboardingStep = false
                        },
                        text = { Text("Partner Host", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Endpoint indicator banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (!isOtpSent) {
                            if (selectedRole == AuthRole.USER) "POST /api/v1/auth/users/send-otp"
                            else "POST /api/v1/auth/partners/send-otp"
                        } else if (!showOnboardingStep) {
                            if (selectedRole == AuthRole.USER) "POST /api/v1/auth/users/verify-otp"
                            else "POST /api/v1/auth/partners/verify-otp"
                        } else {
                            "POST /api/v1/partners/onboarding"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = VoiceCyanAccent,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (jwtToken != null && !showOnboardingStep) {
                    // Success View: Display issued JWT Bearer token
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusOnlineGreen.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusOnlineGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusOnlineGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "200 OK — Authenticated",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOnlineGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Bearer ${jwtToken!!.take(32)}... (role: ${selectedRole.name.lowercase()})",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else if (showOnboardingStep) {
                    // Partner Onboarding Form
                    Text(
                        text = "Complete Partner Profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = partnerName,
                        onValueChange = { partnerName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = partnerRate,
                        onValueChange = { partnerRate = it },
                        label = { Text("Per-Minute Rate ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Phone Number Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number (E.164)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        enabled = !isOtpSent,
                        modifier = Modifier.fillMaxWidth().testTag("auth_phone_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isOtpSent) {
                        Button(
                            onClick = {
                                isSubmitting = true
                                isOtpSent = true
                                isSubmitting = false
                                resendCountdown = 30
                                otpCode = if (selectedRole == AuthRole.USER) "482910" else "938102"
                            },
                            modifier = Modifier.fillMaxWidth().testTag("send_otp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VoiceVioletPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send SMS OTP Code")
                        }
                    } else {
                        // OTP Input
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it },
                            label = { Text("6-Digit OTP Code") },
                            placeholder = { Text("e.g. 482910") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("auth_otp_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (resendCountdown > 0) "Resend in ${resendCountdown}s (Rate limited)" else "Didn't receive code?",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (resendCountdown == 0) {
                                OutlinedButton(
                                    onClick = { resendCountdown = 30 },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Resend", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.${System.currentTimeMillis()}"
                                jwtToken = token
                                if (selectedRole == AuthRole.PARTNER && !isProfileComplete) {
                                    showOnboardingStep = true
                                } else {
                                    onAuthenticated(selectedRole, phoneNumber, token)
                                }
                            },
                            enabled = otpCode.length >= 4,
                            modifier = Modifier.fillMaxWidth().testTag("verify_otp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusOnlineGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify & Issue JWT")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (jwtToken != null && !showOnboardingStep) {
                Button(
                    onClick = {
                        onAuthenticated(selectedRole, phoneNumber, jwtToken!!)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoiceVioletPrimary)
                ) {
                    Text("Done")
                }
            } else if (showOnboardingStep) {
                Button(
                    onClick = {
                        showOnboardingStep = false
                        onAuthenticated(selectedRole, phoneNumber, jwtToken!!)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoiceVioletPrimary)
                ) {
                    Text("Submit Profile")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
