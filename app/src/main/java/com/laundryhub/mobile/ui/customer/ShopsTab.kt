package com.laundryhub.mobile.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundryhub.mobile.data.Address
import com.laundryhub.mobile.data.ApiClient
import com.laundryhub.mobile.data.Shop
import com.laundryhub.mobile.ui.common.Chip
import com.laundryhub.mobile.ui.common.EmptyBox
import com.laundryhub.mobile.ui.common.ErrorBox
import com.laundryhub.mobile.ui.common.LoadingBox
import com.laundryhub.mobile.ui.common.MutedText
import com.laundryhub.mobile.ui.common.Panel
import com.laundryhub.mobile.ui.common.ScreenPadding
import com.laundryhub.mobile.ui.common.Title
import com.laundryhub.mobile.ui.common.Tone
import com.laundryhub.mobile.ui.common.money
import com.laundryhub.mobile.ui.common.rememberApi
import com.laundryhub.mobile.ui.theme.Amber
import com.laundryhub.mobile.ui.theme.AmberSoft
import com.laundryhub.mobile.ui.theme.Blue
import com.laundryhub.mobile.ui.theme.BlueSoft
import com.laundryhub.mobile.ui.theme.Green
import com.laundryhub.mobile.ui.theme.GreenSoft
import com.laundryhub.mobile.ui.theme.Muted
import com.laundryhub.mobile.ui.theme.Red
import com.laundryhub.mobile.ui.theme.RedSoft
import kotlinx.coroutines.delay

/** FR-02: browse nearby laundry shops by service, price and rating. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopsTab(
    addresses: List<Address>,
    selected: Address?,
    onSelectAddress: (Address) -> Unit,
    onOpenShop: (Long) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("distance") }

    // wait until the user stops typing for 0.4 s before searching
    LaunchedEffect(search) {
        delay(400)
        query = search.trim()
    }

    val shops = rememberApi("$query|$sort|${selected?.addressId}") {
        ApiClient.api.shops(query.ifBlank { null }, sort, selected?.lat, selected?.lng)
    }

    LazyColumn(
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (addresses.isNotEmpty()) {
            item {
                Column {
                MutedText("Near")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(addresses, key = { it.addressId }) { a ->
                        FilterChip(
                            selected = a.addressId == selected?.addressId,
                            onClick = { onSelectAddress(a) },
                            label = { Text("📍 ${a.label}") },
                        )
                    }
                }
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search shop name, area or service") },
                leadingIcon = { Text("🔍") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("distance" to "Nearest", "price" to "Cheapest", "rating" to "Top rated").forEach { (value, label) ->
                    FilterChip(selected = sort == value, onClick = { sort = value }, label = { Text(label) })
                }
            }
        }

        val list = shops.data
        when {
            list == null && shops.error != null -> item { ErrorBox(shops.error) { shops.reload() } }
            list == null -> item { LoadingBox(Modifier.fillMaxWidth()) }
            list.isEmpty() -> item { EmptyBox("No shops found. Try another search.") }
            else -> items(list, key = { it.shopId }) { shop -> ShopCard(shop) { onOpenShop(shop.shopId) } }
        }
    }
}

/** Soft background + strong text color for the shop's letter badge, picked by shopId. */
private val BadgeColors = listOf(BlueSoft to Blue, GreenSoft to Green, AmberSoft to Amber, RedSoft to Red)
private val StarGold = Color(0xFFF59E0B)

/** "Clean & Fresh Laundry" -> "CF", "Bopha Shop 9" -> "BS" */
private fun initials(name: String): String =
    name.split(" ").filter { it.firstOrNull()?.isLetter() == true }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "🧺" }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopCard(shop: Shop, onClick: () -> Unit) {
    val (badgeBg, badgeFg) = BadgeColors[(shop.shopId % BadgeColors.size).toInt()]
    Panel(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(52.dp).background(badgeBg, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials(shop.name), color = badgeFg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Title(shop.name, modifier = Modifier.weight(1f))
                    if (shop.open) Chip("Open", Tone.Green) else Chip("Closed", Tone.Gray)
                }
                MutedText(shop.address)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★ ", color = StarGold, fontWeight = FontWeight.Bold)
                    Text(if (shop.ratingCount > 0) "${shop.rating} (${shop.ratingCount})" else "New", fontWeight = FontWeight.SemiBold)
                    shop.distanceKm?.let { MutedText("  ·  📍 $it km") }
                }
            }
        }
        shop.minPricePerKg?.let {
            Text("from ${money(it)}/kg", color = Blue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        if (shop.services.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                shop.services.forEach { svc ->
                    Text(
                        svc.name,
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(Color(0xFFF2F4F7), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
