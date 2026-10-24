package com.laundryhub.mobile.ui.common

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun money(amount: Double?): String = if (amount == null) "—" else String.format(Locale.US, "$%.2f", amount)

private val SHOW = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.US)
private val SEND = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

fun parseDate(iso: String?): LocalDateTime? = try {
    if (iso == null) null else LocalDateTime.parse(iso.take(19))
} catch (e: Exception) {
    null
}

fun showDate(iso: String?): String = parseDate(iso)?.format(SHOW) ?: "—"

fun sendDate(time: LocalDateTime): String = time.withNano(0).format(SEND)

/** "4:59" until the time, or null if it has passed. */
fun countdown(iso: String?): String? {
    val until = parseDate(iso) ?: return null
    val seconds = Duration.between(LocalDateTime.now(), until).seconds
    if (seconds <= 0) return null
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

fun shortId(id: String): String = id.take(8).uppercase(Locale.US)

/** Deliverable 9 states in friendly words. */
fun statusLabel(status: String): String = when (status) {
    "PENDING_PAYMENT" -> "Waiting for payment"
    "AWAITING_SHOP" -> "Waiting for shop"
    "PICKUP_SCHEDULED" -> "Driver picking up"
    "WASHING" -> "Washing"
    "OUT_FOR_DELIVERY" -> "Out for delivery"
    "DELIVERED" -> "Delivered"
    "CANCELLED" -> "Cancelled"
    else -> status
}

/** 1..5 progress steps for the tracking screen. */
fun statusStep(status: String): Int = when (status) {
    "AWAITING_SHOP" -> 1
    "PICKUP_SCHEDULED" -> 2
    "WASHING" -> 3
    "OUT_FOR_DELIVERY" -> 4
    "DELIVERED" -> 5
    else -> 0
}
