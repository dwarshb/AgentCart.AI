package com.dwarshb.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PayPalUser(
    val email: String = "",
    val name: String = "",
    val payerId: String = "",
    val isSignedIn: Boolean = false,
    val balance: String = "$1,250.00",
    val linkedPaymentMethod: String = "PayPal Balance & Chase Visa •••• 4242",
    val authMethod: String = "PayPal Sandbox Account",
    val isBiometricLinked: Boolean = true
)

object PayPalAuthManager {
    private const val PREFS_NAME = "agentcart_paypal_auth"
    private const val KEY_SIGNED_IN = "key_signed_in"
    private const val KEY_EMAIL = "key_email"
    private const val KEY_NAME = "key_name"
    private const val KEY_PAYER_ID = "key_payer_id"
    private const val KEY_BALANCE = "key_balance"
    private const val KEY_PAYMENT_METHOD = "key_payment_method"

    // Demo Sandbox Profiles for instant testing
    val DEMO_PROFILES = listOf(
        PayPalUser(
            email = "alex.miller-buyer@sandbox.paypal.com",
            name = "Alex Miller",
            payerId = "PAYER-ALEX-88921",
            isSignedIn = true,
            balance = "$1,450.00",
            linkedPaymentMethod = "PayPal Balance (Primary) & Chase •••• 4242",
            authMethod = "PayPal Sandbox Buyer Account"
        ),
        PayPalUser(
            email = "sarah.tech-buyer@sandbox.paypal.com",
            name = "Sarah Chen",
            payerId = "PAYER-SARAH-55219",
            isSignedIn = true,
            balance = "$3,200.50",
            linkedPaymentMethod = "PayPal Balance & Wells Fargo •••• 9102",
            authMethod = "PayPal Sandbox Verified"
        )
    )

    private val _currentUser = MutableStateFlow(
        PayPalUser(
            email = "alex.miller-buyer@sandbox.paypal.com",
            name = "Alex Miller",
            payerId = "PAYER-ALEX-88921",
            isSignedIn = true,
            balance = "$1,450.00",
            linkedPaymentMethod = "PayPal Balance & Chase •••• 4242",
            authMethod = "PayPal Sandbox 1-Click Active"
        )
    )
    val currentUser: StateFlow<PayPalUser> = _currentUser.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val signedIn = prefs.getBoolean(KEY_SIGNED_IN, true) // Default to signed in for seamless demo
        if (signedIn) {
            val email = prefs.getString(KEY_EMAIL, "alex.miller-buyer@sandbox.paypal.com") ?: "alex.miller-buyer@sandbox.paypal.com"
            val name = prefs.getString(KEY_NAME, "Alex Miller") ?: "Alex Miller"
            val payerId = prefs.getString(KEY_PAYER_ID, "PAYER-ALEX-88921") ?: "PAYER-ALEX-88921"
            val balance = prefs.getString(KEY_BALANCE, "$1,450.00") ?: "$1,450.00"
            val paymentMethod = prefs.getString(KEY_PAYMENT_METHOD, "PayPal Balance & Chase •••• 4242") ?: "PayPal Balance & Chase •••• 4242"

            _currentUser.value = PayPalUser(
                email = email,
                name = name,
                payerId = payerId,
                isSignedIn = true,
                balance = balance,
                linkedPaymentMethod = paymentMethod,
                authMethod = "Saved PayPal Session"
            )
        } else {
            _currentUser.value = PayPalUser(isSignedIn = false)
        }
    }

    fun signIn(
        context: Context?,
        email: String,
        name: String = "PayPal User",
        payerId: String = "PAYER-${System.currentTimeMillis() % 100000}",
        balance: String = "$1,200.00",
        paymentMethod: String = "PayPal Balance & Visa •••• 8831"
    ) {
        val user = PayPalUser(
            email = email.trim(),
            name = name.trim().ifEmpty { email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() } },
            payerId = payerId,
            isSignedIn = true,
            balance = balance,
            linkedPaymentMethod = paymentMethod,
            authMethod = "PayPal Sandbox Sign-In"
        )
        _currentUser.value = user

        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_SIGNED_IN, true)
                .putString(KEY_EMAIL, user.email)
                .putString(KEY_NAME, user.name)
                .putString(KEY_PAYER_ID, user.payerId)
                .putString(KEY_BALANCE, user.balance)
                .putString(KEY_PAYMENT_METHOD, user.linkedPaymentMethod)
                .apply()
        }
    }

    fun signInWithDemoProfile(context: Context?, profileIndex: Int) {
        val profile = DEMO_PROFILES.getOrNull(profileIndex) ?: DEMO_PROFILES[0]
        _currentUser.value = profile
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_SIGNED_IN, true)
                .putString(KEY_EMAIL, profile.email)
                .putString(KEY_NAME, profile.name)
                .putString(KEY_PAYER_ID, profile.payerId)
                .putString(KEY_BALANCE, profile.balance)
                .putString(KEY_PAYMENT_METHOD, profile.linkedPaymentMethod)
                .apply()
        }
    }

    fun signOut(context: Context?) {
        _currentUser.value = PayPalUser(isSignedIn = false)
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().putBoolean(KEY_SIGNED_IN, false).apply()
        }
    }

    fun openPayPalWebLogin(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.sandbox.paypal.com/signin"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
