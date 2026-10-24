package com.laundryhub.mobile.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** LaundryHub admin on Telegram: drivers message here for questions (cash, payouts, jobs). */
const val ADMIN_TELEGRAM = "YenYon03"

/** Opens a Telegram chat: the Telegram app if installed, otherwise t.me in the browser. */
fun openTelegram(context: Context, username: String = ADMIN_TELEGRAM): Boolean {
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$username"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$username"))
    return listOf(app, web).any { intent ->
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false   // no Telegram app (or no browser): try the next one
        }
    }
}

/** "💬 Contact admin on Telegram (@YenYon03)" */
@Composable
fun ContactAdminButton(onFail: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        if (!openTelegram(context)) onFail("Install Telegram, then message @$ADMIN_TELEGRAM")
    }, modifier = modifier) {
        Text("💬 Contact admin on Telegram (@$ADMIN_TELEGRAM)")
    }
}
