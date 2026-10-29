package com.dwarshb.ui.components.a2ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dwarshb.appfunctions.OrderResult
import com.dwarshb.appfunctions.ProductSearchResult
import com.dwarshb.auth.PayPalAuthManager
import com.dwarshb.ui.theme.AccentSuccess
import com.dwarshb.ui.theme.PayPalBlue
import com.dwarshb.ui.theme.PayPalGold
import com.dwarshb.ui.theme.PayPalLightBlue
import com.dwarshb.ui.theme.PayPalNavy

/**
 * Animated tool execution chip representing the Android AppFunction execution
 */
@Composable
fun AppFunctionExecutionBadge(
    functionName: String,
    params: String? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "appfn_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PayPalNavy.copy(alpha = 0.08f),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PayPalBlue.copy(alpha = pulseAlpha), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = PayPalBlue,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = PayPalBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Android AppFunction Invocation",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PayPalNavy
                    )
                }
                Text(
                    text = functionName,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Interactive A2UI Product Card generated by Gemini via Channel3 AppFunctions.
 * Lets the user view verified merchant offers and place an order directly from Gemini.
 */
@Composable
fun A2UIProductCard(
    product: ProductSearchResult,
    onPlaceOrder: (ProductSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    isProcessingOrder: Boolean = false
) {
    val currentUser by PayPalAuthManager.currentUser.collectAsState()
    var showOffersComparison by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PayPalBlue.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .testTag("a2ui_product_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar: A2UI Badge & Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PayPalBlue.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PayPalBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "A2UI • Channel3 AppFunction",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PayPalBlue
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentSuccess.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = AccentSuccess,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.confidenceScore,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSuccess
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title and Merchant
            Text(
                text = product.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Store, contentDescription = null, tint = PayPalNavy, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Merchant: ${product.merchant}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Price & Store Offers Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Verified Channel3 Price",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = product.price,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = PayPalNavy
                    )
                }

                if (product.offers.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showOffersComparison = !showOffersComparison },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (showOffersComparison) "Hide Offers" else "${product.offers.size} Store Offers",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            if (showOffersComparison) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Offers Accordion
            AnimatedVisibility(visible = showOffersComparison) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    product.offers.forEachIndexed { idx, offer ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (idx == 0) PayPalLightBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = offer.merchant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (idx == 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = PayPalNavy) {
                                            Text(
                                                text = "BEST",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = offer.price,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PayPalNavy
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PayPal Account 1-Click Bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PayPalNavy.copy(alpha = 0.05f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = PayPalNavy, modifier = Modifier.size(20.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("P", color = PayPalGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentUser.isSignedIn) "1-Click Ready: ${currentUser.name}" else "PayPal Sandbox 1-Click Buy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PayPalNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1-Click Place Order Button directly from A2UI
            Button(
                onClick = { onPlaceOrder(product) },
                enabled = !isProcessingOrder,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PayPalNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("a2ui_place_order_button")
            ) {
                if (isProcessingOrder) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing PayPal Order AppFunction...", fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = PayPalGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Place Order with PayPal • 1-Click", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * A2UI Order Confirmation Receipt
 */
@Composable
fun A2UIOrderConfirmationCard(
    product: ProductSearchResult,
    orderResult: OrderResult,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AccentSuccess.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .testTag("a2ui_order_confirmation_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = AccentSuccess, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Order Placed via AppFunction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "1-Click PayPal transaction authorized",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = product.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Amount Paid:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = orderResult.amount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PayPalNavy)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Transaction ID:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = orderResult.orderId,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
