package com.laundryhub.mobile.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.laundryhub.mobile.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Result of loading something from the API. Call reload() to fetch again. */
class Loadable<T>(val data: T?, val error: String?, val loading: Boolean, val reload: () -> Unit)

/**
 * Loads data when the screen opens (and again when `key` changes).
 * With refreshMs > 0 it keeps refreshing, e.g. live order tracking every 5 seconds.
 */
@Composable
fun <T> rememberApi(key: Any?, refreshMs: Long = 0L, loader: suspend () -> T): Loadable<T> {
    val currentLoader by rememberUpdatedState(loader)
    var tick by remember { mutableIntStateOf(0) }
    var data by remember(key) { mutableStateOf<T?>(null) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    var loading by remember(key) { mutableStateOf(true) }

    LaunchedEffect(key, tick) {
        while (true) {
            try {
                data = currentLoader()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.userMessage()
            }
            loading = false
            if (refreshMs <= 0L) break
            delay(refreshMs)
        }
    }
    return Loadable(data, error, loading) { tick++ }
}

/** Runs a button action (accept order, save, ...) and shows errors in a snackbar. */
class Actions(private val scope: CoroutineScope, private val snackbar: SnackbarHostState) {
    var busy by mutableStateOf(false)
        private set

    fun run(successMessage: String? = null, onError: (Throwable) -> Boolean = { false }, block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            try {
                block()
                busy = false
                if (successMessage != null) snackbar.showSnackbar(successMessage)
            } catch (e: CancellationException) {
                busy = false
                throw e
            } catch (e: Exception) {
                busy = false
                if (!onError(e)) snackbar.showSnackbar(e.userMessage())
            }
        }
    }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }
}

@Composable
fun rememberActions(snackbar: SnackbarHostState): Actions {
    val scope = rememberCoroutineScope()
    return remember(snackbar) { Actions(scope, snackbar) }
}
