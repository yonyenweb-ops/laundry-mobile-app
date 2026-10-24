package com.laundryhub.mobile.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.laundryhub.mobile.data.PayoutDetails
import com.laundryhub.mobile.ui.theme.Line
import java.io.ByteArrayOutputStream

/**
 * "Get paid" box for shops and drivers: their ABA account and KHQR photo.
 * The admin scans this QR in the ABA app to send them their earnings.
 */
@Composable
fun PayoutPanel(details: Loadable<PayoutDetails>, actions: Actions, save: suspend (PayoutDetails) -> PayoutDetails) {
    val context = LocalContext.current
    val d = details.data
    if (d == null) {
        if (details.error != null) ErrorBox(details.error) { details.reload() }
        return
    }
    var name by remember(d) { mutableStateOf(d.abaAccountName ?: "") }
    var number by remember(d) { mutableStateOf(d.abaAccountNumber ?: "") }
    var qr by remember(d) { mutableStateOf(d.qrImage) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val dataUrl = imageToDataUrl(context, uri)
            if (dataUrl == null) actions.message("Could not read that photo") else qr = dataUrl
        }
    }

    Panel {
        Title("💳 Get paid by ABA")
        MutedText("Add your ABA QR code. LaundryHub scans it to send you your earnings.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            QrPreview(qr)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(if (qr == null) "Upload QR photo" else "Change QR photo") }
                if (qr != null) TextButton(onClick = { qr = null }) { Text("Remove") }
            }
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ABA account name") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = number, onValueChange = { v -> number = v.filter { it.isDigit() || it == ' ' } },
            label = { Text("ABA account number") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        BigButton("Save payment details", busy = actions.busy) {
            actions.run("Payment details saved") {
                save(PayoutDetails(name.trim().ifBlank { null }, number.trim().ifBlank { null }, qr))
                details.reload()
            }
        }
    }
}

@Composable
private fun QrPreview(dataUrl: String?) {
    val bitmap = remember(dataUrl) { dataUrl?.let { decodeDataUrl(it) } }
    Box(
        modifier = Modifier
            .size(120.dp)
            .border(1.dp, Line, RoundedCornerShape(8.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = "Your ABA QR code", contentScale = ContentScale.Fit)
        } else {
            MutedText("No QR yet")
        }
    }
}

/** Shrinks the photo to at most 800 px and returns "data:image/jpeg;base64,...". */
private fun imageToDataUrl(context: Context, uri: Uri): String? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
    decoded?.let { bmp ->
        val scale = 800f / maxOf(bmp.width, bmp.height)
        val small = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        val out = ByteArrayOutputStream()
        small.compress(Bitmap.CompressFormat.JPEG, 90, out)
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
} catch (e: Exception) {
    null
}

/** "data:image/jpeg;base64,..." -> Bitmap (null if it cannot be read). */
fun decodeDataUrl(dataUrl: String): Bitmap? = try {
    val bytes = Base64.decode(dataUrl.substringAfter("base64,"), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (e: Exception) {
    null
}
