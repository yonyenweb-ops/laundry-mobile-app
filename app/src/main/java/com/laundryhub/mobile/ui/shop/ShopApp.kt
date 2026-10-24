package com.laundryhub.mobile.ui.shop

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.data.Order
import com.laundryhub.mobile.data.RejectRequest
import com.laundryhub.mobile.data.ServiceItem
import com.laundryhub.mobile.data.ServiceRequest
import com.laundryhub.mobile.data.Session
import com.laundryhub.mobile.data.Shop
import com.laundryhub.mobile.data.ShopUpdateRequest
import com.laundryhub.mobile.ui.common.Actions
import com.laundryhub.mobile.ui.common.Banner
import com.laundryhub.mobile.ui.common.BigButton
import com.laundryhub.mobile.ui.common.Chip
import com.laundryhub.mobile.ui.common.EmptyBox
import com.laundryhub.mobile.ui.common.ErrorBox
import com.laundryhub.mobile.ui.common.InfoRow
import com.laundryhub.mobile.ui.common.Loadable
import com.laundryhub.mobile.ui.common.LoadingBox
import com.laundryhub.mobile.ui.common.MutedText
import com.laundryhub.mobile.ui.common.Panel
import com.laundryhub.mobile.ui.common.PayoutPanel
import com.laundryhub.mobile.ui.common.ScreenPadding
import com.laundryhub.mobile.ui.common.StatusChip
import com.laundryhub.mobile.ui.common.Title
import com.laundryhub.mobile.ui.common.Tone
import com.laundryhub.mobile.ui.common.countdown
import com.laundryhub.mobile.ui.common.money
import com.laundryhub.mobile.ui.common.rememberActions
import com.laundryhub.mobile.ui.common.rememberApi
import com.laundryhub.mobile.ui.common.shortId
import com.laundryhub.mobile.ui.common.showDate
import com.laundryhub.mobile.ui.customer.TabsScaffold
import kotlinx.coroutines.delay
import com.laundryhub.mobile.ui.common.PickOnMapButton
import com.laundryhub.mobile.ui.common.Place
import com.laundryhub.mobile.ui.common.ChangePasswordPanel

private val NEW = listOf("AWAITING_SHOP")
private val IN_PROGRESS = listOf("PICKUP_SCHEDULED", "WASHING", "OUT_FOR_DELIVERY")

/** Laundry shop side: incoming orders, services/prices and shop settings. */
@Composable
fun ShopApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val shop = rememberApi("my-shop", refreshMs = 30_000) { ApiClient.api.myShop() }

    // keep the "verified" flag fresh (FR-10: admin may verify us while the app is open)
    LaunchedEffect(shop.data?.verified) { shop.data?.let { Session.updateVerified(it.verified) } }

    TabsScaffold(
        title = shop.data?.name ?: "My laundry",
        bottomBar = {
            NavigationBar {
                listOf("📥" to "Orders", "🧾" to "Services", "💰" to "Earnings", "🏪" to "Shop").forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(icon) }, label = { Text(label) })
                }
            }
        },
    ) { snackbar ->
        val actions = rememberActions(snackbar)
        when (tab) {
            0 -> ShopOrdersTab(actions, verified = shop.data?.verified ?: Session.verified)
            1 -> ServicesTab(shop, actions)
            2 -> EarningsTab(actions)
            else -> ShopSettingsTab(shop, actions)
        }
    }
}

// =====================================================================
// Orders (FR-05 accept/reject within 10 minutes, FR-07 Washing -> Ready)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopOrdersTab(actions: Actions, verified: Boolean) {
    val orders = rememberApi("shop-orders", refreshMs = 5_000) { ApiClient.api.shopOrders() }
    var filter by rememberSaveable { mutableStateOf("new") }
    var rejecting by remember { mutableStateOf<Order?>(null) }
    val tick by produceState(0) { while (true) { delay(1000); value++ } }

    val all = orders.data
    if (all == null) {
        if (orders.error != null) ErrorBox(orders.error) { orders.reload() } else LoadingBox()
        return
    }
    val newOnes = all.filter { it.status in NEW }
    val active = all.filter { it.status in IN_PROGRESS }
    val done = all.filter { it.status !in NEW && it.status !in IN_PROGRESS }
    val shown = when (filter) { "new" -> newOnes; "active" -> active; else -> done }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!verified) item { Banner("Your shop is waiting for admin verification. Customers cannot see it yet.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == "new", onClick = { filter = "new" }, label = { Text("New (${newOnes.size})") })
                FilterChip(selected = filter == "active", onClick = { filter = "active" }, label = { Text("In progress (${active.size})") })
                FilterChip(selected = filter == "done", onClick = { filter = "done" }, label = { Text("Done") })
            }
        }
        if (shown.isEmpty()) item { EmptyBox(if (filter == "new") "No new orders right now." else "Nothing here yet.") }
        items(shown, key = { it.orderId }) { o ->
            ShopOrderCard(
                o = o,
                tick = tick,
                actions = actions,
                onReject = { rejecting = o },
                onChanged = { orders.reload() },
            )
        }
    }

    rejecting?.let { o ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejecting = null },
            title = { Text("Reject order #${shortId(o.orderId)}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MutedText("The customer gets a full refund.")
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason (optional)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    rejecting = null
                    actions.run("Order rejected") {
                        ApiClient.api.rejectOrder(o.orderId, RejectRequest(reason.ifBlank { null }))
                        orders.reload()
                    }
                }) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("Back") } },
        )
    }
}

@Composable
private fun ShopOrderCard(o: Order, tick: Int, actions: Actions, onReject: () -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Title(o.customerName)
                MutedText("#${shortId(o.orderId)} · placed ${showDate(o.placedAt)}")
            }
            StatusChip(o.status)
        }
        o.items.forEach { InfoRow("${it.service} · ${it.weightKg} kg", money(it.subtotal)) }
        InfoRow("Total (paid)", money(o.total), bold = true)
        MutedText("Pickup ${showDate(o.pickupSlot)} · ${o.pickupAddress}")

        when (o.status) {
            "AWAITING_SHOP" -> {
                val left = remember(tick, o.acceptBy) { countdown(o.acceptBy) }
                Banner(if (left != null) "Accept within $left or the order is cancelled automatically." else "Time is up…")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        actions.run("Accepted. Finding the nearest driver for pickup.") {
                            ApiClient.api.acceptOrder(o.orderId)
                            onChanged()
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Accept") }
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Reject") }
                }
            }
            "PICKUP_SCHEDULED" -> Banner(
                o.pickupDriver?.let { "🛵 ${it.name} (${it.vehiclePlate}) is picking up the laundry." }
                    ?: "Looking for the nearest available driver…",
                Tone.Blue,
            )
            "WASHING" -> BigButton("✅ Laundry is ready (washing done)", busy = actions.busy) {
                actions.run("Marked as ready. A driver will deliver it.") {
                    ApiClient.api.markReady(o.orderId)
                    onChanged()
                }
            }
            "OUT_FOR_DELIVERY" -> Banner(
                o.returnDriver?.let { "🛵 ${it.name} is delivering it back to the customer." }
                    ?: "Looking for a driver for the return trip…",
                Tone.Blue,
            )
            "CANCELLED" -> o.cancelReason?.let { MutedText(it) }
            "DELIVERED" -> o.rating?.let { MutedText("Customer rated you " + "★".repeat(it.shopStars)) }
        }

        o.customerPhone?.let { phone ->
            if (o.status in IN_PROGRESS || o.status in NEW) {
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }) {
                    Text("📞 Call customer")
                }
            }
        }
    }
}

// =====================================================================
// Services and prices
// =====================================================================

@Composable
private fun ServicesTab(shop: Loadable<Shop>, actions: Actions) {
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<ServiceItem?>(null) }

    val s = shop.data
    if (s == null) {
        if (shop.error != null) ErrorBox(shop.error) { shop.reload() } else LoadingBox()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Panel {
            Title("Your services")
            val active = s.services.filter { it.active }
            if (active.isEmpty()) MutedText("Add at least one service so customers can order.")
            active.forEach { svc ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(svc.name, fontWeight = FontWeight.SemiBold)
                        MutedText("${money(svc.pricePerKg)} per kg")
                    }
                    TextButton(onClick = { editing = svc }) { Text("Edit") }
                    TextButton(onClick = {
                        actions.run("Service removed") { ApiClient.api.removeService(svc.serviceId); shop.reload() }
                    }) { Text("Remove") }
                }
            }
        }

        Panel {
            Title("Add a service")
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name, e.g. Wash & Iron") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price per kg ($)") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth())
            BigButton("Add service", busy = actions.busy,
                enabled = name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0) {
                actions.run("Service added") {
                    ApiClient.api.addService(ServiceRequest(name.trim(), price.toDouble()))
                    name = ""
                    price = ""
                    shop.reload()
                }
            }
        }
    }

    editing?.let { svc ->
        var newName by remember(svc.serviceId) { mutableStateOf(svc.name) }
        var newPrice by remember(svc.serviceId) { mutableStateOf(svc.pricePerKg.toString()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit service") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") })
                    OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, label = { Text("Price per kg ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank() && (newPrice.toDoubleOrNull() ?: 0.0) > 0,
                    onClick = {
                        editing = null
                        actions.run("Service updated") {
                            ApiClient.api.updateService(svc.serviceId, ServiceRequest(newName.trim(), newPrice.toDouble()))
                            shop.reload()
                        }
                    },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

// =====================================================================
// Earnings: delivered orders minus the 10% LaundryHub commission
// =====================================================================

@Composable
private fun EarningsTab(actions: Actions) {
    val earnings = rememberApi("shop-earnings", refreshMs = 30_000) { ApiClient.api.shopEarnings() }
    val payout = rememberApi("shop-payout-details") { ApiClient.api.shopPayoutDetails() }
    val e = earnings.data
    if (e == null) {
        if (earnings.error != null) ErrorBox(earnings.error) { earnings.reload() } else LoadingBox()
        return
    }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Today" to e.totals.today, "This week" to e.totals.thisWeek, "All time" to e.totals.allTime).forEach { (label, amount) ->
                    Panel(Modifier.weight(1f)) {
                        MutedText(label)
                        Text(money(amount), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Panel {
                Title("Totals")
                InfoRow("Delivered orders", e.orders.size.toString())
                InfoRow("Laundry amount", money(e.orders.sumOf { it.laundryAmount }))
                InfoRow("Commission (10%)", "-${money(e.orders.sumOf { it.commission })}")
                InfoRow("Net payout", money(e.totals.allTime), bold = true)
                InfoRow("Already paid to you", money(e.totals.allTime - e.waiting))
                InfoRow("Waiting to be paid", money(e.waiting), bold = true)
            }
        }
        item { PayoutPanel(payout, actions) { ApiClient.api.updateShopPayoutDetails(it) } }

        item { Title("Delivered orders") }
        if (e.orders.isEmpty()) item { EmptyBox("No delivered orders yet.") }
        items(e.orders, key = { it.orderId }) { o ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Title(o.customerName)
                        MutedText("#${shortId(o.orderId)} · delivered ${showDate(o.deliveredAt)}")
                    }
                    if (o.paid) Chip("Paid", Tone.Green) else Chip("Waiting", Tone.Amber)
                }
                InfoRow("Laundry amount", money(o.laundryAmount))
                InfoRow("Commission (10%)", "-${money(o.commission)}")
                InfoRow("Net payout", money(o.netPayout), bold = true)
            }
        }
    }
}

// =====================================================================
// Shop settings: open/closed, profile, rating
// =====================================================================

@Composable
private fun ShopSettingsTab(shop: Loadable<Shop>, actions: Actions) {
    val s = shop.data
    if (s == null) {
        if (shop.error != null) ErrorBox(shop.error) { shop.reload() } else LoadingBox()
        return
    }
    var name by remember(s.shopId) { mutableStateOf(s.name) }
    var address by remember(s.shopId) { mutableStateOf(s.address) }
    var phone by remember(s.shopId) { mutableStateOf(s.phone ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (s.verified) Banner("✔ Verified by LaundryHub. Customers can find your shop.", Tone.Green)
        else Banner("Waiting for admin verification. Customers cannot see your shop yet.")

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Title(if (s.open) "Open for orders" else "Closed")
                    MutedText("Closed shops cannot receive new orders.")
                }
                Switch(checked = s.open, onCheckedChange = { open ->
                    actions.run(if (open) "Shop is open" else "Shop is closed") {
                        ApiClient.api.updateMyShop(ShopUpdateRequest(open = open))
                        shop.reload()
                    }
                })
            }
        }

        Panel {
            Title("Rating")
            Text(if (s.ratingCount > 0) "★ ${s.rating} from ${s.ratingCount} reviews" else "No reviews yet")
        }

        Panel {
            Title("Shop profile")
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Shop name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
            com.laundryhub.mobile.ui.common.UseMyLocationButton(
                onLocation = { lat, lng ->
                    actions.run("Shop location updated") {
                        ApiClient.api.updateMyShop(ShopUpdateRequest(lat = lat, lng = lng))
                        shop.reload()
                    }
                },
                onFail = { actions.message(it) },
                onAddress = { line ->
                    address = line
                    actions.run("Shop address updated") {
                        ApiClient.api.updateMyShop(ShopUpdateRequest(address = line))
                        shop.reload()
                    }
                },
            )
            PickOnMapButton(
                title = "Shop location",
                start = if (s.lat != null && s.lng != null) Place("", s.lat, s.lng) else null,
            ) { place ->
                // The address customers see follows the pin too (it can still be edited and saved)
                address = place.name
                actions.run("Shop location updated: ${place.name}") {
                    ApiClient.api.updateMyShop(ShopUpdateRequest(address = place.name, lat = place.lat, lng = place.lng))
                    shop.reload()
                }
            }
            MutedText(
                if (s.lat != null && s.lng != null) "📍 Saved location: ${"%.4f, %.4f".format(s.lat, s.lng)}"
                else "📍 No location saved yet",
            )
            BigButton("Save profile", busy = actions.busy) {
                actions.run("Profile saved") {
                    ApiClient.api.updateMyShop(ShopUpdateRequest(name = name.trim(), address = address.trim(), phone = phone.trim()))
                    shop.reload()
                }
            }
        }

        ChangePasswordPanel(actions)

        OutlinedButton(onClick = { Session.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
    }
}
