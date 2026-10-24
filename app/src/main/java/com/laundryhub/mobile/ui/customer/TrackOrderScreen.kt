package com.laundryhub.mobile.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.data.DisputeRequest
import com.laundryhub.mobile.data.DriverInfo
import com.laundryhub.mobile.data.Order
import com.laundryhub.mobile.data.RatingRequest
import com.laundryhub.mobile.ui.common.Actions
import com.laundryhub.mobile.ui.common.Banner
import com.laundryhub.mobile.ui.common.BigButton
import com.laundryhub.mobile.ui.common.Chip
import com.laundryhub.mobile.ui.common.ErrorBox
import com.laundryhub.mobile.ui.common.InfoRow
import com.laundryhub.mobile.ui.common.LoadingBox
import com.laundryhub.mobile.ui.common.MapPoint
import com.laundryhub.mobile.ui.common.MutedText
import com.laundryhub.mobile.ui.common.OsmMap
import com.laundryhub.mobile.ui.common.Panel
import com.laundryhub.mobile.ui.common.StarPicker
import com.laundryhub.mobile.ui.common.StatusChip
import com.laundryhub.mobile.ui.common.Title
import com.laundryhub.mobile.ui.common.Tone
import com.laundryhub.mobile.ui.common.countdown
import com.laundryhub.mobile.ui.common.money
import com.laundryhub.mobile.ui.common.rememberActions
import com.laundryhub.mobile.ui.common.rememberApi
import com.laundryhub.mobile.ui.common.shortId
import com.laundryhub.mobile.ui.common.showDate
import com.laundryhub.mobile.ui.common.statusStep
import com.laundryhub.mobile.ui.theme.BlueSoft
import kotlinx.coroutines.delay

private val STEPS = listOf("Shop accepts", "Pickup", "Washing", "On the way", "Delivered")

/** FR-08: live order status and driver location (refreshes every 5 seconds). */
@Composable
fun TrackOrderScreen(orderId: String, onBack: () -> Unit) {
    val order = rememberApi(orderId, refreshMs = 5_000) { ApiClient.api.myOrder(orderId) }
    val disputes = rememberApi("disputes-$orderId") { ApiClient.api.myDisputes().filter { it.orderId == orderId } }

    TabsScaffold(title = "Order #${shortId(orderId)}", onBack = onBack) { snackbar ->
        val actions = rememberActions(snackbar)
        val o = order.data
        when {
            o == null && order.error != null -> ErrorBox(order.error) { order.reload() }
            o == null -> LoadingBox()
            else -> OrderDetails(
                o = o,
                disputeStatuses = disputes.data.orEmpty().map { it.status },
                actions = actions,
                onChanged = { order.reload(); disputes.reload() },
            )
        }
    }
}

@Composable
private fun OrderDetails(o: Order, disputeStatuses: List<String>, actions: Actions, onChanged: () -> Unit) {
    val context = LocalContext.current
    var confirmCancel by remember { mutableStateOf(false) }
    var showRating by remember { mutableStateOf(false) }
    var showDispute by remember { mutableStateOf(false) }

    // ticks every second so the "accept within" countdown moves
    val tick by produceState(0) { while (true) { delay(1000); value++ } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---- status (Deliverable 9 state machine) ----
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Title(o.shopName, modifier = Modifier.weight(1f))
                StatusChip(o.status)
            }
            if (o.status == "CANCELLED") {
                Banner("${o.cancelReason ?: "Cancelled"}. Your payment was refunded.", Tone.Red)
            } else {
                val step = statusStep(o.status)
                LinearProgressIndicator(
                    progress = { step / 5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    STEPS.forEachIndexed { i, name ->
                        Text(name, fontSize = 10.sp, fontWeight = if (i < step) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            if (o.status == "AWAITING_SHOP") {
                val left = remember(tick, o.acceptBy) { countdown(o.acceptBy) }
                MutedText(if (left != null) "The shop has $left to accept your order." else "Waiting for the shop…")
            }
        }

        // ---- delivery code for dropOff() [otpOk] ----
        if (o.deliveryOtp != null && o.status in listOf("PICKUP_SCHEDULED", "WASHING", "OUT_FOR_DELIVERY")) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BlueSoft, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MutedText("Delivery code")
                Text(o.deliveryOtp, fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
                MutedText("Tell this code to the driver when your clean laundry arrives.")
            }
        }

        // ---- driver + live map (FR-08) ----
        val driver = o.activeDriver
        if (driver != null) DriverCard(driver, o.status) { phone ->
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        } else if (o.status == "PICKUP_SCHEDULED" || o.status == "OUT_FOR_DELIVERY") {
            Banner("Looking for the nearest available driver…", Tone.Blue)
        }

        val points = buildList {
            if (o.shopLat != null && o.shopLng != null) add(MapPoint(o.shopLat, o.shopLng, "🏪 ${o.shopName}"))
            if (o.pickupLat != null && o.pickupLng != null) add(MapPoint(o.pickupLat, o.pickupLng, "🏠 You"))
            val driverLat = driver?.lat
            val driverLng = driver?.lng
            if (driverLat != null && driverLng != null) add(MapPoint(driverLat, driverLng, "🛵 ${driver?.name ?: "Driver"}"))
        }
        if (points.isNotEmpty() && o.status != "CANCELLED" && o.status != "DELIVERED") {
            val center = points.lastOrNull { it.title.startsWith("🛵") }
            OsmMap(
                points = points,
                center = center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }

        // ---- order summary ----
        Panel {
            Title("Order summary")
            o.items.forEach { InfoRow("${it.service} · ${it.weightKg} kg", money(it.subtotal)) }
            InfoRow("Delivery fee", money(o.deliveryFee))
            InfoRow("Total", money(o.total), bold = true)
            InfoRow("Payment", paymentLabel(o.paymentStatus))
            InfoRow("Pickup", showDate(o.pickupSlot))
            MutedText(o.pickupAddress)
        }

        // ---- rating (FR-09) ----
        o.rating?.let { r ->
            Panel {
                Title("Your rating")
                Text("Shop " + "★".repeat(r.shopStars) + (r.driverStars?.let { "   Driver " + "★".repeat(it) } ?: ""))
                r.comment?.takeIf { it.isNotBlank() }?.let { MutedText("“$it”") }
            }
        }

        // ---- disputes (FR-10) ----
        disputeStatuses.forEach { st ->
            when (st) {
                "OPEN" -> Banner("Your refund request is being reviewed by LaundryHub.")
                "REFUNDED" -> Banner("Your refund request was approved. Money refunded.", Tone.Green)
                "REJECTED" -> Banner("Your refund request was rejected.", Tone.Red)
            }
        }

        // ---- actions ----
        if (o.status == "AWAITING_SHOP") {
            OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth()) { Text("Cancel order") }
        }
        if (o.status == "DELIVERED" && o.rating == null) {
            BigButton("⭐ Rate shop and driver") { showRating = true }
        }
        val canDispute = o.status != "CANCELLED" && o.paymentStatus != "REFUNDED" && o.paymentStatus != "CASH_DUE" && "OPEN" !in disputeStatuses
        if (canDispute && o.status in listOf("WASHING", "OUT_FOR_DELIVERY", "DELIVERED")) {
            TextButton(onClick = { showDispute = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Report a problem / ask for a refund")
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this order?") },
            text = { Text("You will get a full refund of ${money(o.total)}.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    actions.run("Order cancelled and refunded") { ApiClient.api.cancelOrder(o.orderId); onChanged() }
                }) { Text("Yes, cancel") }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep order") } },
        )
    }

    if (showRating) RatingDialog(
        hasDriver = o.returnDriver != null,
        onDismiss = { showRating = false },
        onSubmit = { shop, drv, comment ->
            showRating = false
            actions.run("Thank you for your rating!") {
                ApiClient.api.rateOrder(o.orderId, RatingRequest(shop, drv, comment))
                onChanged()
            }
        },
    )

    if (showDispute) DisputeDialog(
        onDismiss = { showDispute = false },
        onSubmit = { reason ->
            showDispute = false
            actions.run("Refund request sent to LaundryHub") {
                ApiClient.api.openDispute(o.orderId, DisputeRequest(reason))
                onChanged()
            }
        },
    )
}

@Composable
private fun DriverCard(d: DriverInfo, status: String, onCall: (String) -> Unit) {
    Panel {
        MutedText(if (status == "PICKUP_SCHEDULED") "Your pickup driver" else "Your delivery driver")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Title("🛵 ${d.name}")
                MutedText(d.vehiclePlate ?: "")
            }
            if (d.lat != null) Chip("Live", Tone.Green)
        }
        d.phone?.let { phone -> OutlinedButton(onClick = { onCall(phone) }) { Text("📞 Call $phone") } }
    }
}

@Composable
private fun RatingDialog(hasDriver: Boolean, onDismiss: () -> Unit, onSubmit: (Int, Int?, String) -> Unit) {
    var shopStars by remember { mutableIntStateOf(5) }
    var driverStars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate your order") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StarPicker("Laundry shop", shopStars) { shopStars = it }
                if (hasDriver) StarPicker("Driver", driverStars) { driverStars = it }
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comment (optional)") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(shopStars, if (hasDriver) driverStars else null, comment.trim()) }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } },
    )
}

@Composable
private fun DisputeDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report a problem") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MutedText("Tell us what went wrong. An admin will review it and can refund your payment.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("What happened?") },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(reason.trim()) }, enabled = reason.isNotBlank()) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun paymentLabel(status: String?): String = when (status) {
    "REFUNDED" -> "Refunded"
    "CASH_DUE" -> "Cash on delivery (not paid yet)"
    "CASH_PAID" -> "Paid in cash"
    "VOIDED" -> "Not charged"
    else -> "Paid"
}
