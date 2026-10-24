package com.laundryhub.mobile.ui.driver

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.data.AvailabilityRequest
import com.laundryhub.mobile.data.CashSentRequest
import com.laundryhub.mobile.data.DriverHistory
import com.laundryhub.mobile.data.DropOffRequest
import com.laundryhub.mobile.data.LocationRequest
import com.laundryhub.mobile.data.Order
import com.laundryhub.mobile.data.RejectRequest
import com.laundryhub.mobile.data.Session
import com.laundryhub.mobile.ui.common.Actions
import com.laundryhub.mobile.ui.common.Banner
import com.laundryhub.mobile.ui.common.ContactAdminButton
import com.laundryhub.mobile.ui.common.BigButton
import com.laundryhub.mobile.ui.common.Chip
import com.laundryhub.mobile.ui.common.EmptyBox
import com.laundryhub.mobile.ui.common.ErrorBox
import com.laundryhub.mobile.ui.common.InfoRow
import com.laundryhub.mobile.ui.common.LOCATION_PERMISSIONS
import com.laundryhub.mobile.ui.common.LoadingBox
import com.laundryhub.mobile.ui.common.MapPickerDialog
import com.laundryhub.mobile.ui.common.MutedText
import com.laundryhub.mobile.ui.common.Panel
import com.laundryhub.mobile.ui.common.PayoutPanel
import com.laundryhub.mobile.ui.common.Place
import com.laundryhub.mobile.ui.common.ScreenPadding
import com.laundryhub.mobile.ui.common.Title
import com.laundryhub.mobile.ui.common.Tone
import com.laundryhub.mobile.ui.common.decodeDataUrl
import com.laundryhub.mobile.ui.common.hasLocationPermission
import com.laundryhub.mobile.ui.common.money
import com.laundryhub.mobile.ui.common.rememberActions
import com.laundryhub.mobile.ui.common.rememberApi
import com.laundryhub.mobile.ui.common.shortId
import com.laundryhub.mobile.ui.common.showDate
import com.laundryhub.mobile.ui.common.startLocationUpdates
import com.laundryhub.mobile.ui.customer.TabsScaffold
import kotlinx.coroutines.launch
import com.laundryhub.mobile.ui.common.ProfilePanel
import com.laundryhub.mobile.ui.common.ChangePasswordPanel

/** Driver side: go online, share live GPS (FR-08), do pickup and return jobs (FR-06). */
@Composable
fun DriverApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = rememberApi("driver-me", refreshMs = 15_000) { ApiClient.api.driverMe() }
    val jobs = rememberApi("driver-jobs", refreshMs = 5_000) { ApiClient.api.driverJobs() }
    // Cash on delivery the driver still has to give LaundryHub (shown on the Jobs screen)
    val cash = rememberApi("driver-cash", refreshMs = 15_000) { ApiClient.api.driverHistory() }
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var lastSent by remember { mutableLongStateOf(0L) }
    var deliveringOrder by remember { mutableStateOf<Order?>(null) }
    var decliningOrder by remember { mutableStateOf<Order?>(null) }
    // A place the driver picked by hand; while set, GPS updates are paused so they don't overwrite it.
    // Saved per account (Session.chosenPlace), so it survives logging out and in on a shared emulator.
    var manualPlace by remember { mutableStateOf(Session.chosenPlace) }
    var choosingPlace by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val online = profile.data?.available == true
    LaunchedEffect(profile.data?.verified) { profile.data?.let { Session.updateVerified(it.verified) } }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        hasPermission = granted.values.any { it }
    }

    // ---- FR-08: while online, send GPS to the backend (at most every 5 seconds) ----
    val useGps = manualPlace == null
    DisposableEffect(online, hasPermission, useGps) {
        val stop = if (online && hasPermission && useGps) {
            lastSent = 0L   // send the first fix right away (e.g. after "Use my GPS again")
            startLocationUpdates(context) { lat, lng ->
                val now = System.currentTimeMillis()
                if (now - lastSent > 5_000) {
                    lastSent = now
                    scope.launch {
                        try {
                            ApiClient.api.sendLocation(LocationRequest(lat, lng))
                        } catch (e: Exception) {
                            // ignore: the next update will try again
                        }
                    }
                }
            }
        } else {
            {}
        }
        onDispose { stop() }
    }

    TabsScaffold(
        title = "Driver · ${Session.displayName}",
        bottomBar = {
            NavigationBar {
                listOf("🛵" to "Jobs", "💰" to "Earnings", "👤" to "Profile").forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(icon) }, label = { Text(label) })
                }
            }
        },
    ) { snackbar ->
        val actions = rememberActions(snackbar)
        if (tab == 1) {
            EarningsTab(actions)
            return@TabsScaffold
        }
        if (tab == 2) {
            DriverProfileTab(actions)
            return@TabsScaffold
        }
        val p = profile.data
        if (p == null) {
            if (profile.error != null) ErrorBox(profile.error) { profile.reload() } else LoadingBox()
            return@TabsScaffold
        }

        LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!p.verified) item { Banner("Your account is waiting for admin verification. You cannot go online yet.") }

            // ---- online switch ----
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Title(if (p.available) "🟢 Online" else "⚪ Offline")
                            MutedText(
                                when {
                                    p.busy -> "On a job"
                                    p.available -> "Waiting for the nearest job…"
                                    else -> "Go online to receive pickup and delivery jobs"
                                },
                            )
                        }
                        Switch(
                            checked = p.available,
                            enabled = p.verified && !actions.busy,
                            onCheckedChange = { on ->
                                if (on && !hasPermission) permissionLauncher.launch(LOCATION_PERMISSIONS)
                                actions.run(if (on) "You are online" else "You are offline") {
                                    ApiClient.api.setAvailability(AvailabilityRequest(on))
                                    profile.reload()
                                }
                            },
                        )
                    }
                    Text("${p.vehiclePlate ?: ""}   ·   " +
                        if (p.ratingCount > 0) "★ ${p.rating} (${p.ratingCount})" else "★ New")
                    if (p.available) {
                        val coords = if (p.lat != null) "%.4f, %.4f".format(p.lat, p.lng) else null
                        MutedText(
                            when {
                                manualPlace != null -> "📍 Chosen location · $manualPlace · $coords"
                                coords != null -> "📍 GPS location · $coords · ${showDate(p.locationUpdatedAt)}"
                                else -> "📍 Waiting for GPS…"
                            },
                        )
                        if (!hasPermission && manualPlace == null) {
                            OutlinedButton(onClick = { permissionLauncher.launch(LOCATION_PERMISSIONS) }) {
                                Text("Allow location access")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { choosingPlace = true }) { Text("📍 Change location") }
                            if (manualPlace != null) {
                                TextButton(onClick = {
                                    manualPlace = null
                                    Session.chosenPlace = null
                                    actions.message("Using your GPS again")
                                }) { Text("Use my GPS again") }
                            }
                        }
                    }
                }
            }

            // ---- cash on delivery: give the customers' cash to LaundryHub (right after the 4-digit code) ----
            cash.data?.takeIf { it.cashToHandOver > 0 || it.cashSentWaiting > 0 }?.let { h ->
                item { CashHandOverPanel(h, actions) { cash.reload() } }
            }

            // ---- jobs ----
            item { Title("My jobs") }
            val list = jobs.data
            when {
                list == null && jobs.error != null -> item { ErrorBox(jobs.error) { jobs.reload() } }
                list == null -> item { LoadingBox(Modifier.fillMaxWidth()) }
                list.isEmpty() -> item {
                    EmptyBox(if (p.available) "No jobs yet. New jobs appear here automatically." else "You are offline.")
                }
                else -> items(list, key = { it.orderId }) { o ->
                    JobCard(o, actions, onDeliver = { deliveringOrder = o }, onDecline = { decliningOrder = o },
                        onChanged = { jobs.reload(); profile.reload() })
                }
            }

            item { ContactAdminButton(onFail = { actions.message(it) }) }

            item {
                OutlinedButton(onClick = { Session.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
            }
        }

        deliveringOrder?.let { o ->
            OtpDialog(
                cashToCollect = if (o.paymentStatus == "CASH_DUE") o.total else null,
                onDismiss = { deliveringOrder = null },
                onConfirm = { otp ->
                    deliveringOrder = null
                    val cashOrder = o.paymentStatus == "CASH_DUE"
                    actions.run(
                        if (cashOrder) "Delivered! Now give the ${money(o.total)} cash to LaundryHub." else "Delivered! Job complete.",
                    ) {
                        ApiClient.api.delivered(o.orderId, DropOffRequest(otp))
                        jobs.reload()
                        profile.reload()
                        cash.reload()   // shows "Give $X cash to LaundryHub" at the top of Jobs
                    }
                },
            )
        }

        if (choosingPlace) {
            MapPickerDialog(
                title = "Choose your location",
                start = if (p.lat != null && p.lng != null) Place(manualPlace ?: "", p.lat, p.lng) else null,
                onDismiss = { choosingPlace = false },
                onPick = { place ->
                    choosingPlace = false
                    actions.run("Location set: ${place.name}") {
                        ApiClient.api.sendLocation(LocationRequest(place.lat, place.lng, manual = true))
                        manualPlace = place.name
                        Session.chosenPlace = place.name
                        profile.reload()
                        jobs.reload()   // the move may have given or taken a job - show it now
                    }
                },
            )
        }

        decliningOrder?.let { o ->
            DeclineDialog(
                o,
                onDismiss = { decliningOrder = null },
                onConfirm = { reason ->
                    decliningOrder = null
                    actions.run("Job declined. It goes to another driver.") {
                        ApiClient.api.declineJob(o.orderId, RejectRequest(reason.ifBlank { null }))
                        jobs.reload()
                        profile.reload()
                    }
                },
            )
        }
    }
}

@Composable
private fun JobCard(o: Order, actions: Actions, onDeliver: () -> Unit, onDecline: () -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    val isPickup = o.status == "PICKUP_SCHEDULED"

    fun navigate(lat: Double?, lng: Double?, label: String) {
        val uri = if (lat != null && lng != null) "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})"
        else "geo:0,0?q=${Uri.encode(label)}"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (e: Exception) {
            actions.message("No map app installed")
        }
    }

    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Title(if (isPickup) "📦 Pickup" else "🚚 Delivery")
                MutedText("Order #${shortId(o.orderId)} · ${money(o.total)}")
            }
            Chip(if (isPickup) "Collect dirty laundry" else "Return clean laundry", if (isPickup) Tone.Amber else Tone.Blue)
        }

        Text("Customer: ${o.customerName}", fontWeight = FontWeight.SemiBold)
        MutedText("🏠 ${o.pickupAddress}")
        if (isPickup) MutedText("⏰ Pickup ${showDate(o.pickupSlot)}")
        Text("Shop: ${o.shopName}", fontWeight = FontWeight.SemiBold)
        MutedText("🏪 ${o.shopAddress ?: ""}")
        MutedText(o.items.joinToString(" · ") { "${it.service} ${it.weightKg} kg" })
        if (o.paymentStatus == "CASH_DUE") {
            Text("💵 Cash on delivery: collect ${money(o.total)} at drop-off", fontWeight = FontWeight.SemiBold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { navigate(o.pickupLat, o.pickupLng, o.pickupAddress) }) { Text("🗺 Customer") }
            OutlinedButton(onClick = { navigate(o.shopLat, o.shopLng, o.shopName) }) { Text("🗺 Shop") }
            o.customerPhone?.let { phone ->
                OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }) {
                    Text("📞")
                }
            }
        }

        if (isPickup) {
            BigButton("✅ Picked up & bag tagged", busy = actions.busy) {
                actions.run("Picked up. Bring the laundry to ${o.shopName}.") {
                    ApiClient.api.pickedUp(o.orderId)
                    onChanged()
                }
            }
        } else if (!o.collectedFromShop) {
            // First collect the clean laundry at the shop; from then on the job can't move to another driver
            BigButton("📦 Collected from shop", busy = actions.busy) {
                actions.run("Collected. Now bring it to ${o.customerName}.") {
                    ApiClient.api.collectedFromShop(o.orderId)
                    onChanged()
                }
            }
        } else {
            MutedText("📦 Collected from ${o.shopName} - on the way to the customer")
            BigButton("✅ Delivered (enter code)", busy = actions.busy, onClick = onDeliver)
        }
        // Once the clean laundry is in the driver's hands, the job can't be given back
        if (isPickup || !o.collectedFromShop) {
            TextButton(onClick = onDecline, enabled = !actions.busy, modifier = Modifier.fillMaxWidth()) {
                Text("✖ Can't do this job", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Cash on delivery: after the 4-digit code the driver holds the customer's cash, which belongs to LaundryHub.
 * The driver sends it (ABA transfer or at the office), taps "I sent the cash" and the admin confirms it.
 */
@Composable
private fun CashHandOverPanel(h: DriverHistory, actions: Actions, onChanged: () -> Unit) {
    var sending by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (h.cashToHandOver > 0) {
            Panel {
                Title("💵 Give ${money(h.cashToHandOver)} cash to LaundryHub")
                MutedText(
                    "You collected this cash from customers. Send it to LaundryHub (ABA transfer or at the office), " +
                        "then tap the button. LaundryHub pays you, the other driver and the shop after confirming it.",
                )
                h.cashOrders.forEach { c -> InfoRow("#${shortId(c.orderId)} · ${c.customerName}", money(c.amount)) }
                BigButton("📤 I sent the cash", busy = actions.busy) { sending = true }
            }
        }
        if (h.cashSentWaiting > 0) {
            Banner("⏳ ${money(h.cashSentWaiting)} sent - waiting for LaundryHub to confirm.", Tone.Blue)
        }
    }

    if (sending) {
        var reference by remember { mutableStateOf("") }
        // LaundryHub's ABA account and QR code (set by the admin on the website)
        val aba = rememberApi("laundryhub-aba") { ApiClient.api.laundryHubAba() }
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { sending = false },
            title = { Text("Send ${money(h.cashToHandOver)} to LaundryHub") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val d = aba.data
                    when {
                        d == null && aba.error != null -> MutedText("Could not load LaundryHub's ABA: ${aba.error}")
                        d == null -> MutedText("Loading LaundryHub's ABA…")
                        d.qrImage == null && d.abaAccountNumber == null ->
                            MutedText("LaundryHub has not added its ABA account yet. Please give the cash at the office.")
                        else -> {
                            MutedText("1. Scan this QR with your ABA app, or send to the account below:")
                            val qr = remember(d.qrImage) { d.qrImage?.let { decodeDataUrl(it) } }
                            qr?.let {
                                Image(it.asImageBitmap(), contentDescription = "LaundryHub ABA QR code",
                                    modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Fit)
                            }
                            d.abaAccountName?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                            d.abaAccountNumber?.let { number ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(number, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        clipboard.setText(AnnotatedString(number))
                                        actions.message("Account number copied")
                                    }) { Text("Copy") }
                                }
                            }
                        }
                    }
                    MutedText("2. After sending, enter the ABA transaction number (or the receipt number if you paid at the office):")
                    OutlinedTextField(value = reference, onValueChange = { reference = it },
                        label = { Text("ABA / receipt number") }, singleLine = true)
                    MutedText("Questions about the cash? Message the admin:")
                    ContactAdminButton(onFail = { actions.message(it) })
                }
            },
            confirmButton = {
                TextButton(enabled = reference.isNotBlank(), onClick = {
                    sending = false
                    actions.run("Thanks! LaundryHub will confirm your ${money(h.cashToHandOver)}.") {
                        ApiClient.api.cashSent(CashSentRequest(reference.trim()))
                        onChanged()
                    }
                }) { Text("I sent it") }
            },
            dismissButton = { TextButton(onClick = { sending = false }) { Text("Cancel") } },
        )
    }
}

/** Profile tab: name, phone, vehicle plate, password, contact the admin, log out. */
@Composable
private fun DriverProfileTab(actions: Actions) {
    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProfilePanel(actions, isDriver = true) }
        item { ChangePasswordPanel(actions) }
        item { ContactAdminButton(onFail = { actions.message(it) }) }
        item { OutlinedButton(onClick = { Session.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Log out") } }
    }
}

/** Driver gives the job back; the backend offers it to the next nearest driver. */
@Composable
private fun DeclineDialog(o: Order, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val reasons = listOf("Too far away", "Vehicle problem", "Can't make the pickup time", "Emergency")
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decline job #${shortId(o.orderId)}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MutedText("The job goes to another driver. It won't be offered to you again.")
                reasons.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(r)
                    }
                }
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason (optional)") })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason) }) { Text("Decline job") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep job") } },
    )
}

// =====================================================================
// Earnings: $0.50 per completed pickup or return trip
// =====================================================================

@Composable
private fun EarningsTab(actions: Actions) {
    val history = rememberApi("driver-history", refreshMs = 30_000) { ApiClient.api.driverHistory() }
    val payout = rememberApi("driver-payout-details") { ApiClient.api.driverPayoutDetails() }
    val h = history.data
    if (h == null) {
        if (history.error != null) ErrorBox(history.error) { history.reload() } else LoadingBox()
        return
    }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Today" to h.totals.today, "This week" to h.totals.thisWeek, "All time" to h.totals.allTime).forEach { (label, amount) ->
                    Panel(Modifier.weight(1f)) {
                        MutedText(label)
                        Text(money(amount), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // Cash on delivery: the customers' cash this driver collected belongs to LaundryHub
        if (h.cashToHandOver > 0 || h.cashSentWaiting > 0) {
            item { CashHandOverPanel(h, actions) { history.reload() } }
        }
        item {
            Banner(
                if (h.waiting > 0) "⏳ ${money(h.waiting)} waiting to be paid by LaundryHub to your ABA."
                else "✔ LaundryHub has paid you for every trip.",
                if (h.waiting > 0) Tone.Amber else Tone.Green,
            )
        }
        item { PayoutPanel(payout, actions) { ApiClient.api.updateDriverPayoutDetails(it) } }
        item { ContactAdminButton(onFail = { actions.message(it) }) }

        item { Title("Completed trips") }
        if (h.legs.isEmpty()) item { EmptyBox("No completed trips yet. Finished pickups and returns appear here.") }
        items(h.legs, key = { it.orderId + it.type }) { leg ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Title(if (leg.type == "PICKUP") "📦 Pickup" else "🚚 Return")
                        MutedText("${showDate(leg.completedAt)} · #${shortId(leg.orderId)}")
                    }
                    leg.fee?.let { Chip(if (leg.paid) "+${money(it)} · Paid" else "+${money(it)} · Waiting", if (leg.paid) Tone.Green else Tone.Amber) }
                }
                // Pickup goes customer -> shop, return goes shop -> customer
                val customer = "customer ${leg.customerName}"
                val (from, to) = if (leg.type == "PICKUP") customer to leg.shopName else leg.shopName to customer
                Text("From $from")
                Text("→ to $to", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** dropOff() [otpOk]: the customer reads the 4-digit code from their app. */
@Composable
private fun OtpDialog(cashToCollect: Double?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var otp by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MutedText("Ask the customer for the 4-digit code shown in their app.")
                cashToCollect?.let { Text("💵 Collect ${money(it)} in cash before confirming.", fontWeight = FontWeight.SemiBold) }
                OutlinedTextField(
                    value = otp,
                    onValueChange = { v -> otp = v.filter { it.isDigit() }.take(4) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, letterSpacing = 8.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(otp) }, enabled = otp.length == 4) { Text("Confirm delivery") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
