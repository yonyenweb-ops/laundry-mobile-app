package com.laundryhub.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.data.Session
import com.laundryhub.mobile.data.userMessage
import com.laundryhub.mobile.ui.auth.AuthScreen
import com.laundryhub.mobile.ui.customer.CustomerApp
import com.laundryhub.mobile.ui.driver.DriverApp
import com.laundryhub.mobile.ui.shop.ShopApp

/**
 * One app, three roles (Deliverable 2 actors).
 * After login the backend tells us the role, and we show that role's screens.
 */
@Composable
fun LaundryHubApp() {
    // On start: check the saved token is still valid (also refreshes "verified").
    LaunchedEffect(Unit) {
        if (Session.token != null) {
            try {
                Session.login(ApiClient.api.me())
            } catch (e: Exception) {
                e.userMessage()   // a 401 logs out automatically
            }
        }
    }

    when (Session.role) {
        null -> AuthScreen()
        "CUSTOMER" -> CustomerApp()
        "SHOP" -> ShopApp()
        "DRIVER" -> DriverApp()
        else -> AdminNotHere()
    }
}

@Composable
private fun AdminNotHere() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Admins use the LaundryHub admin website (React), not the mobile app.")
        Button(onClick = { Session.logout() }) { Text("Log out") }
    }
}
