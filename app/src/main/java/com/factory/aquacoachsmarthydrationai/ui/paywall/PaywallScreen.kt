package com.factory.aquacoachsmarthydrationai.ui.paywall

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.factory.aquacoachsmarthydrationai.data.billing.BillingConnectionState
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumProduct
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumProducts

private const val TERMS_URL = "https://example.com/aquacoach/terms"
private const val PRIVACY_URL = "https://example.com/aquacoach/privacy"

private val PREMIUM_FEATURES = listOf(
    "Smart Goal Calculator personalized to your body & activity",
    "Custom hydration reminders with flexible scheduling",
    "Full history, streaks & analytics",
    "Support ongoing development of AquaCoach"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(viewModel: PaywallViewModel, onClose: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? Activity
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AquaCoach Premium", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClose()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isPremium) {
                PremiumActiveCard()
            } else {
                Text(
                    text = "Unlock the full AquaCoach experience",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                FeatureHighlights()
                Spacer(modifier = Modifier.height(20.dp))

                when (val connection = uiState.connectionState) {
                    is BillingConnectionState.Error -> {
                        ConnectionErrorCard(message = connection.message, onRetry = viewModel::retryConnection)
                    }
                    BillingConnectionState.Connecting, BillingConnectionState.Disconnected -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "Connecting to Google Play"
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Connecting to Google Play…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    BillingConnectionState.Connected -> {
                        SubscriptionTiers(
                            products = uiState.products,
                            isPurchasing = uiState.isPurchasing,
                            onSelect = { productId ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activity?.let { viewModel.purchase(it, productId) }
                            }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        SupportPackCard(
                            product = uiState.products[PremiumProducts.SMALL_IAP.productId],
                            isPurchasing = uiState.isPurchasing,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activity?.let { viewModel.purchase(it, PremiumProducts.SMALL_IAP.productId) }
                            }
                        )
                    }
                }

                uiState.message?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.restorePurchases()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore Purchases")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            LegalLinksRow()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PremiumActiveCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "You're a Premium member! 🎉",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "All premium features are unlocked. Thank you for supporting AquaCoach.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun FeatureHighlights() {
    Column {
        PREMIUM_FEATURES.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectionErrorCard(message: String, onRetry: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Unable to connect to Google Play",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.ifBlank { "Check your network connection and try again." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRetry()
            }) { Text("Retry") }
        }
    }
}

@Composable
private fun SubscriptionTiers(
    products: Map<String, ProductDetails>,
    isPurchasing: Boolean,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TierCard(
            product = PremiumProducts.YEARLY,
            details = products[PremiumProducts.YEARLY.productId],
            badge = "BEST VALUE",
            isPurchasing = isPurchasing,
            onClick = { onSelect(PremiumProducts.YEARLY.productId) }
        )
        TierCard(
            product = PremiumProducts.LIFETIME,
            details = products[PremiumProducts.LIFETIME.productId],
            badge = "ONE-TIME",
            isPurchasing = isPurchasing,
            onClick = { onSelect(PremiumProducts.LIFETIME.productId) }
        )
    }
}

@Composable
private fun TierCard(
    product: PremiumProduct,
    details: ProductDetails?,
    badge: String,
    isPurchasing: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = product.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = priceLabelFor(product, details),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                enabled = !isPurchasing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isPurchasing) "Processing..." else "Choose ${product.displayName}")
            }
        }
    }
}

@Composable
private fun SupportPackCard(product: ProductDetails?, isPurchasing: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Buy us a coffee ☕",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "A small one-time tip to support AquaCoach's development.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onClick,
                enabled = !isPurchasing,
                modifier = Modifier.semantics {
                    contentDescription =
                        "Buy support pack for ${priceLabelFor(PremiumProducts.SMALL_IAP, product)}"
                }
            ) {
                Text(priceLabelFor(PremiumProducts.SMALL_IAP, product))
            }
        }
    }
}

@Composable
private fun LegalLinksRow() {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL))) }) {
            Text("Terms of Service", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))) }) {
            Text("Privacy Policy", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun priceLabelFor(product: PremiumProduct, details: ProductDetails?): String {
    if (details == null) return product.fallbackPriceLabel
    return when (product.billingProductType) {
        BillingClient.ProductType.SUBS -> details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
            ?: product.fallbackPriceLabel
        else -> details.oneTimePurchaseOfferDetails?.formattedPrice ?: product.fallbackPriceLabel
    }
}
