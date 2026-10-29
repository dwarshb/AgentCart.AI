package com.dwarshb

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.dwarshb.auth.PayPalAuthManager
import com.dwarshb.network.DiscoveredProduct
import com.dwarshb.ui.AICheckoutUiState
import com.dwarshb.ui.AICheckoutViewModel
import com.dwarshb.ui.components.A2UIAssistantBottomSheet
import com.dwarshb.ui.components.CameraScannerView
import com.dwarshb.ui.components.OrderHistoryBottomSheet
import com.dwarshb.ui.components.PayPalSignInBottomSheet
import com.dwarshb.ui.components.SettingsBottomSheet
import com.dwarshb.ui.theme.AccentError
import com.dwarshb.ui.theme.AccentSuccess
import com.dwarshb.ui.theme.AgentCartTheme
import com.dwarshb.ui.theme.PayPalBlue
import com.dwarshb.ui.theme.PayPalGold
import com.dwarshb.ui.theme.PayPalLightBlue
import com.dwarshb.ui.theme.PayPalNavy
import com.dwarshb.utils.BiometricAuthenticator

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
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        Log.d("MainActivity", "handleIncomingIntent action=${intent.action} data=${intent.data} extras=${intent.extras?.keySet()}")

        // 1. Check for PayPal OAuth & capture callbacks
        val uri = intent.data
        if (uri != null && uri.scheme == "agentcart" && uri.host == "paypal") {
            when (uri.path) {
                "/return" -> {
                    val orderId = uri.getQueryParameter("token")
                    if (!orderId.isNullOrBlank()) {
                        viewModel.capturePayPalPayment(orderId)
                        return
                    }
                }
                "/cancel" -> {
                    viewModel.paypalCancelled()
                    return
                }
            }
        }

        // 2. Extract search query from all possible Assistant & System intent sources
        val query = extractSearchQuery(intent)
        if (!query.isNullOrBlank()) {
            Log.d("MainActivity", "Successfully resolved voice/assistant query: $query")
            viewModel.openA2UIAssistant(query)
            return
        }

        // 3. Handle general A2UI launch requests without query
        if (uri != null && uri.scheme == "agentcart" && uri.host == "a2ui") {
            viewModel.openA2UIAssistant(null)
            return
        }
        val featureExtra = intent.getStringExtra("feature")
        if (featureExtra != null && featureExtra.contains("a2ui", ignoreCase = true)) {
            viewModel.openA2UIAssistant(null)
            return
        }
    }

    private fun extractSearchQuery(intent: Intent): String? {
        // A. Google Assistant / System Search query extras
        val queryFromSearchManager = intent.getStringExtra(SearchManager.QUERY)
        if (!queryFromSearchManager.isNullOrBlank()) return queryFromSearchManager.trim()

        val queryFromQueryExtra = intent.getStringExtra("query")
        if (!queryFromQueryExtra.isNullOrBlank()) return queryFromQueryExtra.trim()

        val queryFromQExtra = intent.getStringExtra("q")
        if (!queryFromQExtra.isNullOrBlank()) return queryFromQExtra.trim()

        // B. Assistant App Actions parameters (from shortcuts.xml actions.intent.GET_PRODUCT / GET_THING)
        val queryFromProductName = intent.getStringExtra("product.name")
        if (!queryFromProductName.isNullOrBlank()) return queryFromProductName.trim()

        val queryFromThingName = intent.getStringExtra("thing.name")
        if (!queryFromThingName.isNullOrBlank()) return queryFromThingName.trim()

        val queryFromProduct = intent.getStringExtra("product")
        if (!queryFromProduct.isNullOrBlank()) return queryFromProduct.trim()

        val queryFromUserQuery = intent.getStringExtra(SearchManager.USER_QUERY)
        if (!queryFromUserQuery.isNullOrBlank()) return queryFromUserQuery.trim()

        val queryFromExtraText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!queryFromExtraText.isNullOrBlank()) return queryFromExtraText.trim()

        // C. Data URI queries (agentcart://search?query=... or https://agentcart.com/search?query=...)
        val uri = intent.data ?: return null
        val queryFromUri = uri.getQueryParameter("query")
            ?: uri.getQueryParameter("q")
            ?: uri.getQueryParameter("product")
            ?: uri.getQueryParameter("product.name")
            ?: uri.getQueryParameter("thing.name")
        if (!queryFromUri.isNullOrBlank()) return queryFromUri.trim()

        // Path segment support: e.g. /search/Sony+Headphones
        val path = uri.path
        if (path != null && (path.startsWith("/search/") || uri.host == "search")) {
            val segment = uri.lastPathSegment
            if (!segment.isNullOrBlank() && segment != "search") {
                return Uri.decode(segment).trim()
            }
        }

        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAgentWorkflowScreen(viewModel: AICheckoutViewModel, activity: FragmentActivity) {
    val uiState by viewModel.uiState.collectAsState()
    val backendUrl by viewModel.backendUrl.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showPayPalSignInSheet by remember { mutableStateOf(false) }

    val currentUser by PayPalAuthManager.currentUser.collectAsState()

    // Fallback confirmation dialog if biometric hardware unavailable on emulator
    var showBiometricFallbackDialog by remember { mutableStateOf<DiscoveredProduct?>(null) }
    
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        PayPalAuthManager.init(context)
        viewModel.refreshGeminiStatus()
    }

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
                    // Gemini A2UI button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PayPalNavy.copy(alpha = 0.08f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { viewModel.openA2UIAssistant() }
                            .testTag("action_a2ui_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Gemini A2UI Assistant",
                                tint = PayPalNavy,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "A2UI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PayPalNavy
                            )
                        }
                    }

                    // PayPal Sign-In / Account pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (currentUser.isSignedIn) PayPalNavy.copy(alpha = 0.08f) else PayPalNavy,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { showPayPalSignInSheet = true }
                            .testTag("action_paypal_signin_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (currentUser.isSignedIn) PayPalNavy else PayPalGold,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "P",
                                        color = if (currentUser.isSignedIn) PayPalGold else PayPalNavy,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentUser.isSignedIn) currentUser.name.substringBefore(" ") else "PayPal Sign In",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentUser.isSignedIn) PayPalNavy else Color.White
                            )
                        }
                    }

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
                            onOpenA2UI = {
                                viewModel.openA2UIAssistant()
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
                            onBackToCamera = { viewModel.resetState() },
                            onOpenPayPalSignIn = { showPayPalSignInSheet = true }
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
                            text = "Payment Security: Encrypted PayPal Sandbox Order Authorization",
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
            onSaveUrl = { viewModel.updateBackendUrl(it) },
            onDismiss = { showSettingsSheet = false },
            viewModel = viewModel,
            onOpenPayPalSignIn = {
                showSettingsSheet = false
                showPayPalSignInSheet = true
            }
        )
    }

    if (showPayPalSignInSheet) {
        PayPalSignInBottomSheet(
            onDismiss = { showPayPalSignInSheet = false }
        )
    }

    if (showHistorySheet) {
        OrderHistoryBottomSheet(
            onDismiss = { showHistorySheet = false }
        )
    }

    val showA2UISheet by viewModel.showA2UISheet.collectAsState()
    val a2uiQuery by viewModel.a2uiInitialQuery.collectAsState()

    if (showA2UISheet) {
        A2UIAssistantBottomSheet(
            initialQuery = a2uiQuery,
            onDismiss = { viewModel.closeA2UIAssistant() },
            onOrderPlaced = { approvalUrl ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(approvalUrl))
                context.startActivity(intent)
            }
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
            text = "Google Gemini 3.5 Flash Multimodal AI & Channel3 Commerce Integration",
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
    onBackToCamera: () -> Unit,
    onOpenPayPalSignIn: (() -> Unit)? = null
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vendor: ${product.merchantName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        product.availability?.let { avail ->
                            Surface(
                                color = AccentSuccess.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = avail,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    product.brand?.takeIf { it.isNotBlank() }?.let { brandName ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Brand: $brandName",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Complete Channel3 Store Offers Breakdown
                    if (!product.offers.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Channel3 Verified Store Offers:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PayPalNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            product.offers.forEachIndexed { index, offer ->
                                Surface(
                                    color = if (index == 0) PayPalLightBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = offer.merchant,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (index == 0) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = PayPalNavy,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "BEST OFFER",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            offer.shipping?.let { ship ->
                                                Text(
                                                    text = ship,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(
                                            text = offer.price,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PayPalNavy
                                        )
                                    }
                                }
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

            val currentUser by PayPalAuthManager.currentUser.collectAsState()

            // PayPal Connected Account Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PayPalNavy.copy(alpha = 0.06f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clickable { onOpenPayPalSignIn?.invoke() }
                    .testTag("review_screen_paypal_account_bar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PayPalNavy,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("P", color = PayPalGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (currentUser.isSignedIn) "Paying as ${currentUser.name}" else "Sign In with PayPal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PayPalNavy
                            )
                            Text(
                                text = if (currentUser.isSignedIn) "${currentUser.email} • 1-Click Ready" else "Tap to connect your PayPal Sandbox account",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (currentUser.isSignedIn) "Change" else "Sign In",
                        fontSize = 12.sp,
                        color = PayPalBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                    text = if (currentUser.isSignedIn) "1-Click Pay with PayPal" else "Pay with PayPal",
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
