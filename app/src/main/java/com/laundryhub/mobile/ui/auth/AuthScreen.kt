package com.laundryhub.mobile.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundryhub.mobile.R
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.data.LoginRequest
import com.laundryhub.mobile.data.RegisterRequest
import com.laundryhub.mobile.data.Session
import com.laundryhub.mobile.ui.common.BigButton
import com.laundryhub.mobile.ui.common.MutedText
import com.laundryhub.mobile.ui.common.UseMyLocationButton
import com.laundryhub.mobile.ui.common.PickOnMapButton
import com.laundryhub.mobile.ui.common.Place
import com.laundryhub.mobile.ui.common.rememberActions
import com.laundryhub.mobile.ui.theme.Blue

/** FR-01: log in or create an account. */
@Composable
fun AuthScreen() {
    var registering by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.laundryhub_logo), contentDescription = "LaundryHub logo", modifier = Modifier.size(64.dp))
                Text("LaundryHub", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Blue)
            }
            MutedText("Door-to-door laundry with clear prices, tracked orders and cashless payment.")
            Spacer(Modifier.height(8.dp))
            if (registering) {
                RegisterForm(snackbar, onBack = { registering = false })
            } else {
                LoginForm(snackbar, onRegister = { registering = true })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginForm(snackbar: SnackbarHostState, onRegister: () -> Unit) {
    val actions = rememberActions(snackbar)
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable { mutableStateOf(Session.serverUrl) }
    var showServer by rememberSaveable { mutableStateOf(false) }

    Text("Log in", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

    MutedText("Demo accounts (password 123456):")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("customer", "shop1", "driver1").forEach { who ->
            FilterChip(
                selected = email == "$who@laundryhub.com",
                onClick = { email = "$who@laundryhub.com"; password = "123456" },
                label = { Text(who.removeSuffix("1").replaceFirstChar { it.uppercase() }) },
            )
        }
    }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )

    BigButton("Log in", busy = actions.busy, enabled = email.isNotBlank() && password.isNotBlank()) {
        actions.run {
            Session.saveServer(server)
            val auth = ApiClient.api.login(LoginRequest(email.trim(), password))
            if (auth.role == "ADMIN") {
                actions.message("Admins use the admin website")
            } else {
                Session.login(auth)   // the app switches to the role's home screen
            }
        }
    }

    TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
        Text("New here? Create an account")
    }

    TextButton(onClick = { showServer = !showServer }) {
        Text(if (showServer) "Hide server settings" else "⚙ Server settings")
    }
    if (showServer) {
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Backend URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        MutedText("Emulator: http://10.0.2.2:8080/   ·   Real phone: http://YOUR-PC-IP:8080/ (same Wi-Fi)")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterForm(snackbar: SnackbarHostState, onBack: () -> Unit) {
    val actions = rememberActions(snackbar)
    var role by rememberSaveable { mutableStateOf("CUSTOMER") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var shopName by rememberSaveable { mutableStateOf("") }
    var shopAddress by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lng by rememberSaveable { mutableStateOf<Double?>(null) }
    var plate by rememberSaveable { mutableStateOf("") }

    Text("Create account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    MutedText("I am a…")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("CUSTOMER" to "Customer", "SHOP" to "Laundry shop", "DRIVER" to "Driver").forEach { (value, label) ->
            FilterChip(selected = role == value, onClick = { role = value }, label = { Text(label) })
        }
    }

    Field("Full name", fullName) { fullName = it }
    Field("Email", email, KeyboardType.Email) { email = it }
    Field("Phone", phone, KeyboardType.Phone) { phone = it }
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password (min 6 characters)") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )

    if (role == "SHOP") {
        Field("Shop name", shopName) { shopName = it }
        Field("Shop address", shopAddress) { shopAddress = it }
        UseMyLocationButton(
            onLocation = { a, b -> lat = a; lng = b },
            onFail = { actions.message(it) },
        )
        PickOnMapButton(title = "Shop location", start = lat?.let { a -> lng?.let { b -> Place("", a, b) } }) { place ->
            lat = place.lat
            lng = place.lng
            shopAddress = place.name   // the address follows the pin; it can still be edited
        }
        if (lat != null) MutedText("Shop location saved: %.4f, %.4f".format(lat, lng))
        MutedText("A new shop is checked by an admin before customers can see it.")
    }
    if (role == "DRIVER") {
        Field("Vehicle plate (e.g. 2A-1234)", plate) { plate = it }
        MutedText("A new driver is checked by an admin before receiving jobs.")
    }

    BigButton("Create account", busy = actions.busy,
        enabled = fullName.isNotBlank() && email.isNotBlank() && phone.isNotBlank() && password.length >= 6) {
        actions.run {
            val auth = ApiClient.api.register(
                RegisterRequest(
                    fullName = fullName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    password = password,
                    role = role,
                    shopName = shopName.takeIf { role == "SHOP" },
                    shopAddress = shopAddress.takeIf { role == "SHOP" },
                    lat = lat,
                    lng = lng,
                    vehiclePlate = plate.takeIf { role == "DRIVER" },
                ),
            )
            Session.login(auth)
        }
    }
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("I already have an account") }
}

@Composable
private fun Field(label: String, value: String, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}
