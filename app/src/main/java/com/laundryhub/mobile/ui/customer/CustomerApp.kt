package com.laundryhub.mobile.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.ui.common.rememberApi

/** Screens of the customer side. A simple list works as the "back stack". */
sealed interface CustomerScreen {
    data object Tabs : CustomerScreen
    data class PlaceOrder(val shopId: Long) : CustomerScreen
    data class Track(val orderId: String) : CustomerScreen
}

@Composable
fun CustomerApp() {
    var stack by remember { mutableStateOf(listOf<CustomerScreen>(CustomerScreen.Tabs)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedAddressId by rememberSaveable { mutableStateOf<Long?>(null) }

    // FR-01 saved addresses are used by several screens, so we load them here.
    val addresses = rememberApi("addresses") { ApiClient.api.addresses() }
    val addressList = addresses.data.orEmpty()
    val selectedAddress = addressList.firstOrNull { it.addressId == selectedAddressId } ?: addressList.firstOrNull()

    fun open(screen: CustomerScreen) { stack = stack + screen }
    fun back() { if (stack.size > 1) stack = stack.dropLast(1) }

    BackHandler(enabled = stack.size > 1) { back() }

    when (val screen = stack.last()) {
        CustomerScreen.Tabs -> TabsScaffold(
            title = listOf("Find a laundry", "My orders", "My account")[tab],
            bottomBar = {
                NavigationBar {
                    listOf("🧺" to "Shops", "📦" to "Orders", "👤" to "Account").forEachIndexed { i, (icon, label) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(icon) },
                            label = { Text(label) },
                        )
                    }
                }
            },
        ) { snackbar ->
            when (tab) {
                0 -> ShopsTab(
                    addresses = addressList,
                    selected = selectedAddress,
                    onSelectAddress = { selectedAddressId = it.addressId },
                    onOpenShop = { open(CustomerScreen.PlaceOrder(it)) },
                )
                1 -> OrdersTab(onOpenOrder = { open(CustomerScreen.Track(it)) })
                else -> AccountTab(addresses = addresses, snackbar = snackbar)
            }
        }

        is CustomerScreen.PlaceOrder -> PlaceOrderScreen(
            shopId = screen.shopId,
            addresses = addressList,
            defaultAddress = selectedAddress,
            onBack = { back() },
            onPlaced = { orderId ->
                tab = 1
                stack = listOf(CustomerScreen.Tabs, CustomerScreen.Track(orderId))
            },
        )

        is CustomerScreen.Track -> TrackOrderScreen(orderId = screen.orderId, onBack = { back() })
    }
}
