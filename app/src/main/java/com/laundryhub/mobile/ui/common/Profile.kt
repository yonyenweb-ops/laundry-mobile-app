package com.laundryhub.mobile.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.data.PasswordChangeRequest
import com.laundryhub.mobile.data.ProfileUpdateRequest
import com.laundryhub.mobile.data.Session

/**
 * "My profile": name and phone (drivers also the vehicle plate). Email is the login and cannot change.
 * Used by customers and drivers; shops edit their shop name and phone in the Shop profile panel.
 */
@Composable
fun ProfilePanel(actions: Actions, isDriver: Boolean = false) {
    val profile = rememberApi("my-profile") { ApiClient.api.profile() }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    // Fill the fields once the profile has loaded (and again after saving)
    LaunchedEffect(profile.data) {
        profile.data?.let { p ->
            name = p.name
            phone = p.phone ?: ""
            plate = p.vehiclePlate ?: ""
        }
    }

    Panel {
        Title("👤 My profile")
        val p = profile.data
        if (p == null) {
            if (profile.error != null) ErrorBox(profile.error) { profile.reload() } else LoadingBox(Modifier.fillMaxWidth())
            return@Panel
        }
        MutedText("Email (used to log in): ${p.email}")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        if (isDriver) {
            OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Vehicle plate (e.g. 2A-1234)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        val changed = name.trim() != p.name || phone.trim() != (p.phone ?: "") ||
            (isDriver && plate.trim() != (p.vehiclePlate ?: ""))
        BigButton("Save profile", busy = actions.busy,
            enabled = changed && name.isNotBlank() && phone.isNotBlank() && (!isDriver || plate.isNotBlank())) {
            actions.run("Profile saved") {
                val auth = ApiClient.api.updateProfile(
                    ProfileUpdateRequest(name.trim(), phone.trim(), if (isDriver) plate.trim() else null),
                )
                Session.login(auth)   // the screen titles show the new name
                profile.reload()
            }
        }
    }
}

/** Change password: the current one, then the new one twice. */
@Composable
fun ChangePasswordPanel(actions: Actions) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    Panel {
        Title("🔒 Change password")
        OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("Current password") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = new, onValueChange = { new = it }, label = { Text("New password (at least 6 characters)") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = repeat, onValueChange = { repeat = it }, label = { Text("New password again") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            isError = repeat.isNotEmpty() && repeat != new,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        if (repeat.isNotEmpty() && repeat != new) MutedText("The new passwords are not the same.")
        BigButton("Change password", busy = actions.busy,
            enabled = current.isNotEmpty() && new.length >= 6 && new == repeat) {
            actions.run("Password changed") {
                ApiClient.api.changePassword(PasswordChangeRequest(current, new))
                current = ""
                new = ""
                repeat = ""
            }
        }
    }
}
