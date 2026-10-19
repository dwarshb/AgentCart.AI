package com.dwarshb.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dwarshb.ai.GeminiVisionService
import com.dwarshb.network.ConnectionTestResult
import com.dwarshb.network.PayPalSandboxClient
import com.dwarshb.ui.AICheckoutViewModel
import com.dwarshb.ui.theme.AccentError
import com.dwarshb.ui.theme.AccentSuccess
import com.dwarshb.ui.theme.PayPalBlue
import com.dwarshb.ui.theme.PayPalGold
import com.dwarshb.ui.theme.PayPalNavy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    currentUrl: String,
    onSaveUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: AICheckoutViewModel? = null,
    onOpenPayPalSignIn: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var urlInput by remember { mutableStateOf(currentUrl) }
    var geminiApiKeyInput by remember { mutableStateOf(GeminiVisionService.effectiveApiKey) }
    var showGeminiKey by remember { mutableStateOf(false) }

    var clientIdInput by remember { mutableStateOf(PayPalSandboxClient.effectiveClientId) }
    var clientSecretInput by remember { mutableStateOf(PayPalSandboxClient.effectiveClientSecret) }
    var showSecret by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ConnectionTestResult?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(PayPalNavy, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Settings & Configuration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Google Gemini AI, PayPal & Cloud Services",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==================== PAYPAL ACCOUNT & SIGN IN SECTION ====================
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PayPalNavy.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paypal_account_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PayPalNavy,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("P", color = PayPalGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PayPal Account Sign-In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Not signed in",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onOpenPayPalSignIn?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("manage_paypal_account_button")
                        ) {
                            Text("Sign In", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== GOOGLE GEMINI AI SECTION ====================
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PayPalBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Google Gemini AI Vision",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Multimodal Engine (gemini-3.5-flash)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentSuccess.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentSuccess, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ready", color = AccentSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Gemini API Key (Optional Override)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = geminiApiKeyInput,
                        onValueChange = {
                            geminiApiKeyInput = it
                            GeminiVisionService.customApiKey = it.trim().ifEmpty { null }
                            viewModel?.refreshGeminiStatus()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("AIzaSy... (leave blank to use cloud proxy)") },
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = PayPalBlue) },
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (geminiApiKeyInput.isNotBlank() && geminiApiKeyInput != "MY_GEMINI_API_KEY") {
                            "Direct Gemini 3.5 Flash REST API mode active."
                        } else {
                            "Using Render cloud Gemini vision proxy. Enter direct API key above if preferred."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== PAYPAL SANDBOX CREDENTIALS SECTION ====================
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Payment,
                            contentDescription = null,
                            tint = PayPalNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PayPal Sandbox Direct API",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Client ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = clientIdInput,
                        onValueChange = {
                            clientIdInput = it
                            testResult = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("paypal_client_id_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("AR2ia9WXJwvuSI...") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Client Secret",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = clientSecretInput,
                        onValueChange = {
                            clientSecretInput = it
                            testResult = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("paypal_client_secret_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("Enter PayPal Secret") },
                        visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showSecret = !showSecret }) {
                                Icon(
                                    if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Connection Button
                    Button(
                        onClick = {
                            isTestingConnection = true
                            testResult = null
                            coroutineScope.launch {
                                val res = PayPalSandboxClient.testConnection(
                                    overrideId = clientIdInput,
                                    overrideSecret = clientSecretInput
                                )
                                testResult = res
                                isTestingConnection = false
                            }
                        },
                        enabled = !isTestingConnection && clientIdInput.isNotBlank() && clientSecretInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("test_paypal_connection_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Credentials against PayPal...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test PayPal Connection Live")
                        }
                    }

                    // Diagnostic Test Output
                    testResult?.let { res ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = if (res.success) AccentSuccess.copy(alpha = 0.12f) else AccentError.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (res.success) AccentSuccess else AccentError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (res.success) "Connection Verified (HTTP ${res.httpCode})" else "Auth Failed (HTTP ${res.httpCode})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (res.success) AccentSuccess else AccentError
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = res.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== RENDER BACKEND URL INPUT ====================
            Text(
                text = "Backend Base URL",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.fillMaxWidth().testTag("backend_url_input"),
                placeholder = { Text("https://agentcart-ai.onrender.com") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        urlInput = "https://agentcart-ai.onrender.com"
                        onSaveUrl(urlInput)
                        PayPalSandboxClient.customClientId = null
                        PayPalSandboxClient.customClientSecret = null
                        clientIdInput = PayPalSandboxClient.effectiveClientId
                        clientSecretInput = PayPalSandboxClient.effectiveClientSecret
                        GeminiVisionService.customApiKey = null
                        geminiApiKeyInput = ""
                        testResult = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset")
                }

                Button(
                    onClick = {
                        onSaveUrl(urlInput)
                        PayPalSandboxClient.customClientId = clientIdInput.trim().ifEmpty { null }
                        PayPalSandboxClient.customClientSecret = clientSecretInput.trim().ifEmpty { null }
                        GeminiVisionService.customApiKey = geminiApiKeyInput.trim().ifEmpty { null }
                        viewModel?.refreshGeminiStatus()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).testTag("save_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Apply")
                }
            }
        }
    }
}
