package com.laundryhub.mobile.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laundryhub.mobile.data.Address
import com.laundryhub.mobile.data.AddressRequest
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.data.Session
import com.laundryhub.mobile.ui.common.BigButton
import com.laundryhub.mobile.ui.common.Loadable
import com.laundryhub.mobile.ui.common.MutedText
import com.laundryhub.mobile.ui.common.Panel
import com.laundryhub.mobile.ui.common.Title
import com.laundryhub.mobile.ui.common.UseMyLocationButton
import com.laundryhub.mobile.ui.common.PickOnMapButton
import com.laundryhub.mobile.ui.common.Place
import com.laundryhub.mobile.ui.common.rememberActions
import com.laundryhub.mobile.ui.common.ProfilePanel
import com.laundryhub.mobile.ui.common.ChangePasswordPanel

/** FR-01: profile and saved pickup addresses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTab(addresses: Loadable<List<Address>>, snackbar: SnackbarHostState) {
    val actions = rememberActions(snackbar)
    var label by rememberSaveable { mutableStateOf("Home") }
    var line by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lng by rememberSaveable { mutableStateOf<Double?>(null) }
    // Set while editing a saved address; null = adding a new one
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun clearForm() {
        editingId = null
        label = "Home"
        line = ""
        lat = null
        lng = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Name and phone can be edited; email is the login
        ProfilePanel(actions)

        Panel {
            Title("Saved pickup addresses")
            val list = addresses.data.orEmpty()
            if (list.isEmpty()) MutedText("No saved addresses yet.")
            list.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("📍 ${a.label}", fontWeight = FontWeight.SemiBold)
                        MutedText(a.addressLine)
                    }
                    TextButton(onClick = {
                        editingId = a.addressId
                        label = a.label
                        line = a.addressLine
                        lat = a.lat
                        lng = a.lng
                    }) { Text("Edit") }
                    TextButton(onClick = {
                        actions.run("Address removed") {
                            ApiClient.api.deleteAddress(a.addressId)
                            if (editingId == a.addressId) clearForm()
                            addresses.reload()
                        }
                    }) { Text("Remove") }
                }
            }
        }

        Panel {
            Title(if (editingId != null) "Edit address" else "Add an address")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Home", "Office", "Other").forEach { l ->
                    FilterChip(selected = label == l, onClick = { label = l }, label = { Text(l) })
                }
            }
            OutlinedTextField(
                value = line,
                onValueChange = { line = it },
                label = { Text("Street, district, city") },
                modifier = Modifier.fillMaxWidth(),
            )
            UseMyLocationButton(
                onLocation = { a, b -> lat = a; lng = b },
                onFail = { actions.message(it) },
                onAddress = { if (line.isBlank()) line = it },
            )
            PickOnMapButton(title = "Address on the map", start = lat?.let { a -> lng?.let { b -> Place("", a, b) } }) { place ->
                lat = place.lat
                lng = place.lng
                line = place.name   // the address follows the pin; it can still be edited
            }
            if (lat != null) MutedText("GPS saved: %.4f, %.4f (used for 'nearest shop')".format(lat, lng))
            BigButton(if (editingId != null) "Update address" else "Save address", busy = actions.busy, enabled = line.isNotBlank()) {
                val id = editingId
                actions.run(if (id != null) "Address updated" else "Address saved") {
                    val req = AddressRequest(label, line.trim(), lat, lng)
                    if (id != null) ApiClient.api.updateAddress(id, req) else ApiClient.api.addAddress(req)
                    clearForm()
                    addresses.reload()
                }
            }
            if (editingId != null) {
                TextButton(onClick = { clearForm() }) { Text("Cancel editing") }
            }
        }

        ChangePasswordPanel(actions)

        OutlinedButton(onClick = { Session.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
    }
}
