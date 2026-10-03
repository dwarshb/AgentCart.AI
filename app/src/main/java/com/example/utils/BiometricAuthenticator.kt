package com.example.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthenticator {
    fun promptBiometrics(
        activity: FragmentActivity,
        onAuthenticated: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If user cancelled, or hardware error
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Biometric recognized as invalid, system prompt stays open
                }
            }
        )

        // Try standard prompt
        try {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authorize Agent Payment")
                .setSubtitle("Confirming secure transaction via PayPal")
                .setDescription("AgentCart AI requires biometric approval to capture payment from your PayPal account.")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // If device credential not supported or negative button required
            try {
                val fallbackPromptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authorize Agent Payment")
                    .setSubtitle("Confirming secure transaction via PayPal")
                    .setNegativeButtonText("Cancel")
                    .build()
                biometricPrompt.authenticate(fallbackPromptInfo)
            } catch (ex: Exception) {
                // In headless/mock environments without biometric support, notify with error
                onError(ex.localizedMessage ?: "Biometric prompt unavailable on this device")
            }
        }
    }
}
