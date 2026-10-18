package com.dwarshb

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dwarshb.ai.GeminiVisionService
import com.dwarshb.auth.PayPalAuthManager
import com.dwarshb.ui.AICheckoutViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AgentCart AI", appName)
  }

  @Test
  fun `verify banner drawable exists and is loadable`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.agentcart_banner)
    assertNotNull(drawable)
  }

  @Test
  fun `verify gemini model status in viewmodel`() {
    val vm = AICheckoutViewModel()
    vm.refreshGeminiStatus()
    val info = vm.geminiInfo.value
    assertNotNull(info)
    assertEquals("gemini-3.5-flash", info.modelName)
    assertTrue(info.isBackendProxyActive)
  }

  @Test
  fun `verify paypal auth manager sign-in flow`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    PayPalAuthManager.init(context)
    val user = PayPalAuthManager.currentUser.value
    assertNotNull(user)
    assertTrue(user.isSignedIn)
    assertTrue(user.email.contains("paypal.com"))

    // Test sign in with custom demo profile
    PayPalAuthManager.signInWithDemoProfile(context, 1)
    val updatedUser = PayPalAuthManager.currentUser.value
    assertEquals("Sarah Chen", updatedUser.name)
    assertTrue(updatedUser.isSignedIn)
  }
}
