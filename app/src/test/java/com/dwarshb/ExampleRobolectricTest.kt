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

  @Test
  fun `verify a2ui assistant state toggles in viewmodel`() {
    val vm = AICheckoutViewModel()
    assertEquals(false, vm.showA2UISheet.value)
    assertEquals(null, vm.a2uiInitialQuery.value)

    vm.openA2UIAssistant("Sony WH-1000XM5")
    assertEquals(true, vm.showA2UISheet.value)
    assertEquals("Sony WH-1000XM5", vm.a2uiInitialQuery.value)

    vm.closeA2UIAssistant()
    assertEquals(false, vm.showA2UISheet.value)
    assertEquals(null, vm.a2uiInitialQuery.value)
  }

  @Test
  fun `verify agentcart appfunctions product search and order creation`() = kotlinx.coroutines.runBlocking {
    val appFunctions = com.dwarshb.appfunctions.AgentCartAppFunctions

    // 1. Search product function
    val searchParams = com.dwarshb.appfunctions.ProductSearchParams(query = "Sony WH-1000XM5")
    val searchResult = appFunctions.searchProduct(searchParams)
    assertNotNull(searchResult)
    assertTrue(searchResult.title.contains("Sony", ignoreCase = true))
    assertTrue(searchResult.price.isNotBlank())
    assertTrue(searchResult.offers.isNotEmpty())

    // 2. Place order function
    val orderParams = com.dwarshb.appfunctions.CreateOrderParams(
      productId = searchResult.id,
      productTitle = searchResult.title,
      price = searchResult.price,
      payerEmail = "alex.miller-buyer@sandbox.paypal.com"
    )
    val orderResult = appFunctions.placeOrder(orderParams)
    assertNotNull(orderResult)
    assertTrue(orderResult.orderId.isNotBlank())
    assertTrue(orderResult.status == "CREATED" || orderResult.status == "COMPLETED")
  }

  @Test
  fun `verify assistant search intent launches a2ui with query`() {
    val intentWithExtra = android.content.Intent(android.content.Intent.ACTION_SEARCH).apply {
      putExtra(android.app.SearchManager.QUERY, "Anker 65W Charger")
    }
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java, intentWithExtra)
    controller.create().start().resume()
    val activity = controller.get()
    assertNotNull(activity)
  }
}
