package com.laundryhub.mobile.ui.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.ui.common.EmptyBox
import com.laundryhub.mobile.ui.common.ErrorBox
import com.laundryhub.mobile.ui.common.LoadingBox
import com.laundryhub.mobile.ui.common.MutedText
import com.laundryhub.mobile.ui.common.Panel
import com.laundryhub.mobile.ui.common.ScreenPadding
import com.laundryhub.mobile.ui.common.StatusChip
import com.laundryhub.mobile.ui.common.Title
import com.laundryhub.mobile.ui.common.money
import com.laundryhub.mobile.ui.common.rememberApi
import com.laundryhub.mobile.ui.common.showDate

@Composable
fun OrdersTab(onOpenOrder: (String) -> Unit) {
    val orders = rememberApi("my-orders", refreshMs = 10_000) { ApiClient.api.myOrders() }
    val list = orders.data
    when {
        list == null && orders.error != null -> ErrorBox(orders.error) { orders.reload() }
        list == null -> LoadingBox()
        list.isEmpty() -> EmptyBox("No orders yet.\nPick a shop in the Shops tab to place your first order.")
        else -> LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.orderId }) { o ->
                Panel(modifier = Modifier.clickable { onOpenOrder(o.orderId) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Title(o.shopName, modifier = Modifier.weight(1f))
                        StatusChip(o.status)
                    }
                    MutedText(o.items.joinToString(" · ") { "${it.service} ${it.weightKg} kg" })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MutedText("Placed ${showDate(o.placedAt)}")
                        Text(money(o.total), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

