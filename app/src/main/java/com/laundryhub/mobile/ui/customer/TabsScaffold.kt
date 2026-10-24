package com.laundryhub.mobile.ui.customer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.laundryhub.mobile.ui.theme.Line

/** Top bar + bottom tabs + snackbar, shared by the customer, shop and driver homes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScaffold(
    title: String,
    bottomBar: @Composable () -> Unit = {},
    onBack: (() -> Unit)? = null,
    content: @Composable (SnackbarHostState) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onBack != null) TextButton(onClick = onBack) { Text("← Back") }
                    },
                )
                HorizontalDivider(color = Line)
            }
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            content(snackbar)
        }
    }
}
