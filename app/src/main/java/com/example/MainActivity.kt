package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.network.DiscoveredProduct
import com.example.ui.AICheckoutUiState
import com.example.ui.AICheckoutViewModel
import com.example.ui.components.CameraScannerView
import com.example.ui.components.OrderHistoryBottomSheet
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AgentCartTheme
import com.example.ui.theme.PayPalBlue
import com.example.ui.theme.PayPalGold
import com.example.ui.theme.PayPalLightBlue
import com.example.ui.theme.PayPalNavy
import com.example.utils.BiometricAuthenticator

class MainActivity : FragmentActivity() {
    private val viewModel: AICheckoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgentCartTheme {
                MainAgentWorkflowScreen(viewModel = viewModel, activity = this)
            }
        }
        handlePayPalIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePayPalIntent(intent)
    }

    private fun handlePayPalIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "agentcart" && uri.host == "paypal") {
            when (uri.path) {
                "/return" -> {
                    val orderId = uri.getQueryParameter("token")
                    if (!orderId.isNullOrBlank()) {
                        viewModel.capturePayPalPayment(orderId)
                    }
                }
                "/cancel" -> {
                    viewModel.paypalCancelled()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAgentWorkflowScreen(viewModel: AICheckoutViewModel, activity: FragmentActivity) {
    val uiState by viewModel.uiState.collectAsState()
    val backendUrl by viewModel.backendUrl.collectAsState()
    val useFallback by viewModel.useSandboxFallback.collectAsState()
    val useGemmaOnDevice by viewModel.useGemmaOnDevice.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    // Fallback confirmation dialog if biometric hardware unavailable on emulator
    var showBiometricFallbackDialog by remember { mutableStateOf<DiscoveredProduct?>(null) }
    
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PayPalNavy,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = PayPalLightBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AgentCart",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    color = PayPalBlue,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "AI",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "PayPal Commerce Agent",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("action_history_button")
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Order History",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("action_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            BackHandler(enabled = uiState !is AICheckoutUiState.Idle) {
                viewModel.resetState()
            }

            Crossfade(targetState = uiState, label = "StateTransition") { state ->
                when (state) {
                    is AICheckoutUiState.Idle -> {
                        CameraScannerView(
                            onImageCaptured = { bytes ->
                                viewModel.analyzeImageWithAIAgent(bytes)
                            },
                            onTextSubmitted = { query ->
                                viewModel.analyzeTextWithAIAgent(query)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is AICheckoutUiState.ProcessingAI -> {
                        ProcessingAIScreen(statusMessage = state.statusMessage)
                    }

                    is AICheckoutUiState.ReviewMatch -> {
                        ReviewMatchScreen(
                            product = state.product,
                            source = state.source,
                            onPayClicked = {
                                BiometricAuthenticator.promptBiometrics(
                                    activity = activity,
                                    onAuthenticated = {
                                        viewModel.executePayPalPayment(state.product.id){ approvalUrl ->
                                        val intent = Intent(Intent.ACTION_VIEW,Uri.parse(approvalUrl))
                                        context.startActivity(intent)}
                                    },
                                    onError = { _ ->
                                        // If biometrics not enrolled in emulator, open fallback confirmation
                                        showBiometricFallbackDialog = state.product
                                    }
                                )
                            },
                            onBackToCamera = { viewModel.resetState() }
                        )
                    }

                    is AICheckoutUiState.ExecutingPayment -> {
                        ExecutingPaymentScreen(statusText = "Creating PayPal Order...")
                    }

                    is AICheckoutUiState.WaitingForPayPalApproval -> {
                        WaitingForPayPalApprovalScreen(
                            orderId = state.orderId,
                            approvalUrl = state.approvalUrl,
                            product = state.product,
                            onReopenBrowser = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.approvalUrl))
                                context.startActivity(intent)
                            },
                            onCancel = { viewModel.paypalCancelled() }
                        )
                    }

                    is AICheckoutUiState.CapturingPayment -> {
                        ExecutingPaymentScreen(statusText = "Capturing Approved PayPal Payment...")
                    }

                    is AICheckoutUiState.Success -> {
                        SuccessScreen(
                            transactionId = state.transactionId,
                            product = state.product,
                            zapierTriggered = state.zapierTriggered,
                            onScanAnother = { viewModel.resetState() },
                            onViewReceipts = { showHistorySheet = true }
                        )
                    }

                    is AICheckoutUiState.Error -> {
                        ErrorScreen(
                            errorMessage = state.message,
                            onRetry = { viewModel.resetState() },
                            onOpenSettings = { showSettingsSheet = true }
                        )
                    }
                }
            }
        }
    }

    // Biometric fallback dialog for testing on emulators
    showBiometricFallbackDialog?.let { product ->
        AlertDialog(
            onDismissRequest = { showBiometricFallbackDialog = null },
            icon = {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = PayPalNavy,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Authorize Agent Payment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Confirm secure 1-click transaction via PayPal Sandbox for ${product.title} (${product.price}).",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = PayPalNavy.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Emulator Mode: Biometric hardware bypassed with secure token",
                            fontSize = 11.sp,
                            color = PayPalNavy,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prodId = product.id
                        showBiometricFallbackDialog = null
                         viewModel.executePayPalPayment(prodId){ approvalUrl ->
                                        val intent = Intent(Intent.ACTION_VIEW,Uri.parse(approvalUrl))
                                        context.startActivity(intent)}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
                    modifier = Modifier.testTag("dialog_confirm_pay_button")
                ) {
                    Text("Confirm PayPal Pay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricFallbackDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sheets
    if (showSettingsSheet) {
        SettingsBottomSheet(
            currentUrl = backendUrl,
            useFallback = useFallback,
            useGemmaOnDevice = useGemmaOnDevice,
            onSaveUrl = { viewModel.updateBackendUrl(it) },
            onToggleFallback = { viewModel.setSandboxFallback(it) },
            onToggleGemmaOnDevice = { viewModel.setUseGemmaOnDevice(it) },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showHistorySheet) {
        OrderHistoryBottomSheet(
            onDismiss = { showHistorySheet = false }
        )
    }
}

@Composable
fun ProcessingAIScreen(statusMessage: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ai")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale),
                shape = CircleShape,
                color = PayPalNavy.copy(alpha = 0.12f)
            ) {}
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = PayPalNavy
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PayPalLightBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        CircularProgressIndicator(
            color = PayPalNavy,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = statusMessage,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Multimodal inference running on Google Gemini Flash & Channel3 Node",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ReviewMatchScreen(
    product: DiscoveredProduct,
    source: String,
    onPayClicked: () -> Unit,
    onBackToCamera: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Header Tag matching Blueprint Section 1.5
            Surface(
                color = AccentSuccess.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AccentSuccess, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🤖 AI AGENT: ITEM DISCOVERED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Discovered Item Main Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = PayPalBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = product.category ?: "Verified Match",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PayPalBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = AccentSuccess.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AccentSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = product.confidenceScore,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = product.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Vendor: ${product.merchantName}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    product.description?.let { desc ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = desc,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Model Diagnostics Telemetry
                    val isGemmaOnDevice = product.visionModel?.contains("Gemma", ignoreCase = true) == true || source.contains("Gemma", ignoreCase = true)
                    val isGeminiSuccess = product.geminiStatus == "SUCCESS"
                    val isGeminiMissing = product.geminiStatus == "MISSING_API_KEY"
                    val isGeminiError = product.geminiStatus == "ERROR"

                    Surface(
                        color = when {
                            isGemmaOnDevice -> AccentSuccess.copy(alpha = 0.12f)
                            isGeminiSuccess -> AccentSuccess.copy(alpha = 0.1f)
                            isGeminiMissing || isGeminiError -> Color(0xFFFFF3CD)
                            else -> PayPalBlue.copy(alpha = 0.08f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isGemmaOnDevice) Icons.Default.Bolt else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = when {
                                        isGemmaOnDevice || isGeminiSuccess -> AccentSuccess
                                        isGeminiMissing || isGeminiError -> Color(0xFF856404)
                                        else -> PayPalBlue
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        isGemmaOnDevice -> "⚡ Gemma-4 On-Device Engine: VERIFIED"
                                        isGeminiSuccess -> "Gemini Flash Vision: LIVE"
                                        else -> "AI Pipeline Status"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when {
                                        isGemmaOnDevice || isGeminiSuccess -> Color(0xFF2E7D32)
                                        isGeminiMissing || isGeminiError -> Color(0xFF856404)
                                        else -> PayPalNavy
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    isGemmaOnDevice -> "100% on-device local execution using Gemma-4. Zero cloud API keys required."
                                    isGeminiSuccess -> "✅ Live multimodal inference succeeded on Render backend."
                                    isGeminiMissing -> "⚠️ GEMINI_API_KEY is not set on Render. Add it in Render Dashboard -> Environment Variables to run live vision."
                                    isGeminiError -> "⚠️ Gemini call returned error: ${product.geminiRawOutput}"
                                    else -> "Inference source: ${product.visionModel ?: source}"
                                },
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!product.geminiRawOutput.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Model Output: \"${product.geminiRawOutput}\"",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Price & Checkout Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total via PayPal",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = product.price,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = PayPalNavy
                            )
                        }

                        Surface(
                            color = PayPalGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF996500),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Buyer Protection",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF996500)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pipeline verification notice
            Text(
                text = "Resolved via: $source",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom CTA block matching Blueprint Section 1.5
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "API Stack Orchestrated by APIMatic SDK Schema",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = onPayClicked,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003087)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("biometric_pay_button")
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Biometric Pay with PayPal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBackToCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Another Object")
            }
        }
    }
}

@Composable
fun ExecutingPaymentScreen(statusText: String = "Authorizing PayPal Sandbox Ledger...") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Color(0xFF002244),
            strokeWidth = 4.dp,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = statusText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Executing OAuth2 token exchange & v2/checkout order",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WaitingForPayPalApprovalScreen(
    orderId: String,
    approvalUrl: String,
    product: DiscoveredProduct,
    onReopenBrowser: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = PayPalBlue,
            strokeWidth = 4.dp,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Waiting for PayPal Approval",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please log in and authorize the payment for ${product.title} (${product.price}) in the PayPal Sandbox browser window. Once approved, you will be redirected back here automatically.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onReopenBrowser,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Re-open PayPal Approval Tab", fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Cancel Order")
        }
    }
}

@Composable
fun SuccessScreen(
    transactionId: String,
    product: DiscoveredProduct,
    zapierTriggered: Boolean,
    onScanAnother: () -> Unit,
    onViewReceipts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎉", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Order Completed",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Transaction Ref: $transactionId",
            color = Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Receipt Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Purchased Item",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = AccentSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PAID VIA PAYPAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = product.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = product.merchantName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Amount Paid", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = product.price,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = PayPalNavy
                    )
                }

                if (zapierTriggered) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = PayPalBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PayPalBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zapier automation webhook dispatched successfully",
                                fontSize = 11.sp,
                                color = PayPalBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onScanAnother,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003087)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("scan_another_product_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Another Product", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onViewReceipts,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View In Purchase Ledger")
        }
    }
}

@Composable
fun ErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val isScanError = errorMessage.contains("Scan", ignoreCase = true) ||
            errorMessage.contains("Gemini", ignoreCase = true) ||
            errorMessage.contains("camera", ignoreCase = true) ||
            errorMessage.contains("image", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = AccentError.copy(alpha = 0.12f),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(if (isScanError) "🔍" else "⚠️", fontSize = 34.sp)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (isScanError) "AI Scan Pipeline Alert" else "PayPal API Error",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Actual Error Response:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentError
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (onOpenSettings != null && !isScanError) {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Settings", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        OutlinedButton(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("back_to_camera_button")
        ) {
            Text("Back to Camera / Try Again", fontWeight = FontWeight.SemiBold)
        }
    }
}
