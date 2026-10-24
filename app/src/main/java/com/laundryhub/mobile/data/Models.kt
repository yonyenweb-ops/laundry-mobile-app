package com.laundryhub.mobile.data

// These classes mirror the JSON of the Spring Boot backend (see the backend "dto" package).
// Every field has a default value so Gson can create them safely.

// ---------------- Auth (FR-01) ----------------
data class LoginRequest(val email: String = "", val password: String = "")

data class RegisterRequest(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val role: String = "CUSTOMER",          // CUSTOMER, SHOP or DRIVER
    val shopName: String? = null,
    val shopAddress: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val vehiclePlate: String? = null,
)

/** "My profile". vehiclePlate is only for drivers. */
data class Profile(
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val vehiclePlate: String? = null,
    val role: String = "",
)

data class ProfileUpdateRequest(val name: String, val phone: String, val vehiclePlate: String? = null)

data class PasswordChangeRequest(val currentPassword: String, val newPassword: String)

data class AuthResponse(
    val token: String = "",
    val role: String = "",
    val userId: Long = 0,
    val profileId: Long? = null,
    val displayName: String = "",
    val email: String = "",
    val verified: Boolean = false,
)

// ---------------- Shops (FR-02) ----------------
data class ServiceItem(
    val serviceId: Long = 0,
    val name: String = "",
    val pricePerKg: Double = 0.0,
    val active: Boolean = true,
)

data class Shop(
    val shopId: Long = 0,
    val name: String = "",
    val address: String = "",
    val phone: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val open: Boolean = true,
    val verified: Boolean = false,
    val minPricePerKg: Double? = null,
    val distanceKm: Double? = null,
    val services: List<ServiceItem> = emptyList(),
)

// ---------------- Customer ----------------
data class Address(
    val addressId: Long = 0,
    val label: String = "",
    val addressLine: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
)

data class AddressRequest(val label: String, val addressLine: String, val lat: Double?, val lng: Double?)

data class ItemRequest(val serviceId: Long, val weightKg: Double)

data class PlaceOrderRequest(
    val shopId: Long,
    val pickupSlot: String,            // "2026-10-20T09:00:00"
    val pickupAddress: String,
    val pickupLat: Double?,
    val pickupLng: Double?,
    val items: List<ItemRequest>,
    val paymentMethod: String = "KHQR", // "KHQR" or "CASH" (cash on delivery)
    val paymentToken: String? = null,  // KHQR only; "DECLINE" simulates a failed payment
)

data class RatingRequest(val shopStars: Int, val driverStars: Int?, val comment: String?)

data class DisputeRequest(val reason: String)

data class Dispute(
    val disputeId: Long = 0,
    val orderId: String = "",
    val status: String = "",
    val reason: String = "",
    val adminNote: String? = null,
    val createdAt: String? = null,
)

// ---------------- Orders (FR-03 .. FR-09) ----------------
data class OrderItem(
    val service: String = "",
    val weightKg: Double = 0.0,
    val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0,
)

data class DriverInfo(
    val driverId: Long = 0,
    val name: String = "",
    val phone: String? = null,
    val vehiclePlate: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val locationUpdatedAt: String? = null,
)

data class RatingInfo(val shopStars: Int = 0, val driverStars: Int? = null, val comment: String? = null)

data class Order(
    val orderId: String = "",
    val status: String = "",
    val customerName: String = "",
    val customerPhone: String? = null,
    val shopId: Long = 0,
    val shopName: String = "",
    val shopPhone: String? = null,
    val shopAddress: String? = null,
    val shopLat: Double? = null,
    val shopLng: Double? = null,
    val placedAt: String? = null,
    val acceptBy: String? = null,
    val pickupSlot: String? = null,
    val pickupAddress: String = "",
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val items: List<OrderItem> = emptyList(),
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0,
    val paymentStatus: String? = null,
    val paymentTxId: String? = null,
    val paymentMethod: String? = null,   // "KHQR" or "CASH"
    val deliveryOtp: String? = null,     // only sent to the customer
    val cancelReason: String? = null,
    val pickupDriver: DriverInfo? = null,
    val returnDriver: DriverInfo? = null,
    val activeDriver: DriverInfo? = null,
    val rating: RatingInfo? = null,
    val collectedFromShop: Boolean = false,   // delivery trip: the driver has the clean laundry
)

// ---------------- Laundry shop ----------------
data class ShopUpdateRequest(
    val name: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val open: Boolean? = null,
)

data class ServiceRequest(val name: String, val pricePerKg: Double)

data class RejectRequest(val reason: String?)

/** One delivered order: laundry amount minus the 10% LaundryHub commission. */
data class ShopEarningOrder(
    val orderId: String = "",
    val deliveredAt: String? = null,
    val customerName: String = "",
    val laundryAmount: Double = 0.0,
    val commission: Double = 0.0,
    val netPayout: Double = 0.0,
    val paid: Boolean = false,         // the admin has sent this money
)

/** Totals of the shop's net payout. waiting = not paid out by the admin yet. */
data class ShopEarnings(
    val totals: EarningTotals = EarningTotals(),
    val waiting: Double = 0.0,
    val orders: List<ShopEarningOrder> = emptyList(),
)

// ---------------- Driver ----------------
data class DriverProfile(
    val driverId: Long = 0,
    val name: String = "",
    val phone: String? = null,
    val vehiclePlate: String? = null,
    val available: Boolean = false,
    val busy: Boolean = false,
    val verified: Boolean = false,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val lat: Double? = null,
    val lng: Double? = null,
    val locationUpdatedAt: String? = null,
)

data class AvailabilityRequest(val available: Boolean)

/** manual = picked on the map by hand (the backend then gives this driver the job even on a tie). */
data class LocationRequest(val lat: Double, val lng: Double, val manual: Boolean = false)

data class DropOffRequest(val otp: String)

/** One completed trip: the pickup or the return of an order. */
data class DriverLeg(
    val orderId: String = "",
    val type: String = "",             // "PICKUP" or "RETURN"
    val completedAt: String? = null,
    val customerName: String = "",
    val shopName: String = "",
    val fee: Double? = null,           // null for trips completed before fees were recorded
    val paid: Boolean = false,         // the admin has sent this money
)

/** A cash-on-delivery order whose cash the driver still has to hand over to LaundryHub. */
data class CashOrder(
    val orderId: String = "",
    val customerName: String = "",
    val amount: Double = 0.0,
    val deliveredAt: String? = null,
)

/**
 * waiting = earned but not paid out by the admin yet.
 * cashToHandOver = customers' cash this driver collected and must give to LaundryHub (cashOrders).
 */
data class DriverHistory(
    val totals: EarningTotals = EarningTotals(),
    val waiting: Double = 0.0,
    val legs: List<DriverLeg> = emptyList(),
    val cashToHandOver: Double = 0.0,
    val cashOrders: List<CashOrder> = emptyList(),
    val cashSentWaiting: Double = 0.0,   // the driver said they sent it; LaundryHub has not confirmed yet
)

/** Driver: "I sent the customers' cash to LaundryHub" (ABA transaction or receipt number). */
data class CashSentRequest(val reference: String)

// ---------------- Earnings (driver and shop) ----------------
/** "This week" starts on Monday. */
data class EarningTotals(val today: Double = 0.0, val thisWeek: Double = 0.0, val allTime: Double = 0.0)

/** Where the admin sends a shop's or driver's earnings. qrImage = "data:image/jpeg;base64,..." */
data class PayoutDetails(
    val abaAccountName: String? = null,
    val abaAccountNumber: String? = null,
    val qrImage: String? = null,
)

data class ErrorBody(val status: Int = 0, val error: String? = null, val message: String? = null)
