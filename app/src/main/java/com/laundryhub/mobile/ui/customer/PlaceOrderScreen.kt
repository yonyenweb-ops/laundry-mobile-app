package com.laundryhub.mobile.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.laundryhub.mobile.data.Address
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.data.ItemRequest
import com.laundryhub.mobile.data.PlaceOrderRequest
import com.laundryhub.mobile.data.httpCode
import com.laundryhub.mobile.data.userMessage
import com.laundryhub.mobile.ui.common.BigButton
import com.laundryhub.mobile.ui.common.ErrorBox
import com.laundryhub.mobile.ui.common.InfoRow
import com.laundryhub.mobile.ui.common.LoadingBox
import com.laundryhub.mobile.ui.common.MutedText
import com.laundryhub.mobile.ui.common.Panel
import com.laundryhub.mobile.ui.common.Title
import com.laundryhub.mobile.ui.common.UseMyLocationButton
import com.laundryhub.mobile.ui.common.PickOnMapButton
import com.laundryhub.mobile.ui.common.Place
import com.laundryhub.mobile.ui.common.money
import com.laundryhub.mobile.ui.common.rememberActions
import com.laundryhub.mobile.ui.common.rememberApi
import com.laundryhub.mobile.ui.common.sendDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val DELIVERY_FEE = 1.0   // same as Order.DELIVERY_FEE on the backend

/** FR-03 + FR-04: choose services, pickup slot and address, then pay. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceOrderScreen(
    shopId: Long,
    addresses: List<Address>,
    defaultAddress: Address?,
    onBack: () -> Unit,
    onPlaced: (String) -> Unit,
) {
    val shop = rememberApi(shopId) { ApiClient.api.shop(shopId, defaultAddress?.lat, defaultAddress?.lng) }

    // weight typed for each service id
    val kg = remember { mutableStateMapOf<Long, String>() }
    var addressId by rememberSaveable { mutableStateOf(defaultAddress?.addressId) }
    var manualAddress by rememberSaveable { mutableStateOf("") }
    var manualLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var manualLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var slotIndex by rememberSaveable { mutableStateOf(0) }
    var payCash by rememberSaveable { mutableStateOf(false) }
    var simulateDecline by rememberSaveable { mutableStateOf(false) }
    var declinedMessage by remember { mutableStateOf<String?>(null) }

    val slots = remember { pickupSlots() }

    TabsScaffold(title = shop.data?.name ?: "Place order", onBack = onBack) { snackbar ->
        val actions = rememberActions(snackbar)
        val s = shop.data
        when {
            s == null && shop.error != null -> ErrorBox(shop.error) { shop.reload() }
            s == null -> LoadingBox()
            else -> {
                val services = s.services.filter { it.active }
                val chosen = services.mapNotNull { svc ->
                    val w = kg[svc.serviceId]?.toDoubleOrNull()
                    if (w != null && w > 0) svc to w else null
                }
                val total = chosen.sumOf { (svc, w) -> svc.pricePerKg * w } + DELIVERY_FEE
                val address = addresses.firstOrNull { it.addressId == addressId }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    MutedText("${s.address}  ·  ★ ${s.rating}")
                    if (!s.open) MutedText("⚠ This shop is closed right now.")

                    // ---- 1. services ----
                    Panel {
                        Title("1. What should we wash?")
                        MutedText("Enter an estimated weight. The shop weighs it again at pickup.")
                        services.forEach { svc ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(svc.name, fontWeight = FontWeight.SemiBold)
                                    MutedText("${money(svc.pricePerKg)} per kg")
                                }
                                OutlinedTextField(
                                    value = kg[svc.serviceId] ?: "",
                                    onValueChange = { v -> kg[svc.serviceId] = v.filter { it.isDigit() || it == '.' }.take(5) },
                                    label = { Text("kg") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(90.dp),
                                )
                            }
                        }
                    }

                    // ---- 2. pickup address (FR-01 saved addresses) ----
                    Panel {
                        Title("2. Pickup address")
                        addresses.forEach { a ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = addressId == a.addressId, onClick = { addressId = a.addressId })
                                Column {
                                    Text(a.label, fontWeight = FontWeight.SemiBold)
                                    MutedText(a.addressLine)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = addressId == null, onClick = { addressId = null })
                            Text("Another address")
                        }
                        if (addressId == null) {
                            OutlinedTextField(
                                value = manualAddress,
                                onValueChange = { manualAddress = it },
                                label = { Text("Street, district, city") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            UseMyLocationButton(
                                onLocation = { a, b -> manualLat = a; manualLng = b; actions.message("Location added") },
                                onFail = { actions.message(it) },
                                onAddress = { if (manualAddress.isBlank()) manualAddress = it },
                            )
                            PickOnMapButton(
                                title = "Pickup address",
                                start = manualLat?.let { a -> manualLng?.let { b -> Place("", a, b) } },
                            ) { place ->
                                manualLat = place.lat
                                manualLng = place.lng
                                manualAddress = place.name   // the address follows the pin; it can still be edited
                                actions.message("Location added")
                            }
                            MutedText("Tip: save addresses in the Account tab.")
                        }
                    }

                    // ---- 3. pickup time ----
                    Panel {
                        Title("3. Pickup time")
                        slots.forEachIndexed { i, (label, _) ->
                            FilterChip(selected = slotIndex == i, onClick = { slotIndex = i }, label = { Text(label) })
                        }
                    }

                    // ---- 4. payment (FR-04) ----
                    Panel {
                        Title("4. Payment")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !payCash, onClick = { payCash = false })
                            Text("💳 KHQR / card (demo payment gateway)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = payCash, onClick = { payCash = true })
                            Column {
                                Text("💵 Cash on delivery")
                                MutedText("Pay the driver when your clean laundry is returned.")
                            }
                        }
                        if (!payCash) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = simulateDecline, onCheckedChange = { simulateDecline = it })
                                MutedText("Test: simulate a declined payment")
                            }
                        }
                        chosen.forEach { (svc, w) -> InfoRow("${svc.name} · $w kg", money(svc.pricePerKg * w)) }
                        InfoRow("Delivery fee", money(DELIVERY_FEE))
                        InfoRow("Total", money(total), bold = true)
                    }

                    val pickupText = address?.addressLine ?: manualAddress.trim()
                    BigButton(
                        text = if (payCash) "Place order · pay ${money(total)} in cash" else "Place order and pay ${money(total)}",
                        busy = actions.busy,
                        enabled = s.open && chosen.isNotEmpty() && pickupText.isNotBlank(),
                    ) {
                        actions.run(onError = { e ->
                            if (e.httpCode() == 402) {          // [payment declined] branch
                                declinedMessage = e.userMessage()
                                true
                            } else false
                        }) {
                            val order = ApiClient.api.placeOrder(
                                PlaceOrderRequest(
                                    shopId = s.shopId,
                                    pickupSlot = sendDate(slots[slotIndex].second),
                                    pickupAddress = pickupText,
                                    pickupLat = address?.lat ?: manualLat,
                                    pickupLng = address?.lng ?: manualLng,
                                    items = chosen.map { (svc, w) -> ItemRequest(svc.serviceId, w) },
                                    paymentMethod = if (payCash) "CASH" else "KHQR",
                                    paymentToken = when {
                                        payCash -> null
                                        simulateDecline -> "DECLINE"
                                        else -> "demo-khqr-token"
                                    },
                                ),
                            )
                            onPlaced(order.orderId)
                        }
                    }
                }
            }
        }
    }

    declinedMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { declinedMessage = null },
            confirmButton = { TextButton(onClick = { declinedMessage = null }) { Text("OK") } },
            title = { Text("Payment declined") },
            text = { Text("$msg\n\nYour order was not placed. Please try another payment method.") },
        )
    }
}

/** Pickup options: in about 2 hours, tomorrow 09:00, tomorrow 14:00. */
private fun pickupSlots(): List<Pair<String, LocalDateTime>> {
    val soon = LocalDateTime.now().plusHours(2).withMinute(0).withSecond(0)
    val tomorrow = LocalDate.now().plusDays(1)
    return listOf(
        "In about 2 hours (${soon.hour}:00)" to soon,
        "Tomorrow 09:00" to LocalDateTime.of(tomorrow, LocalTime.of(9, 0)),
        "Tomorrow 14:00" to LocalDateTime.of(tomorrow, LocalTime.of(14, 0)),
    )
}
