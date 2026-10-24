package com.laundryhub.mobile.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Every backend endpoint the app uses. Same paths as the Spring Boot controllers. */
interface LaundryApi {

    // ---------- AuthController (FR-01) ----------
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @GET("api/auth/me")
    suspend fun me(): AuthResponse

    /** "My profile" for customers, drivers and shops. */
    @GET("api/auth/profile")
    suspend fun profile(): Profile

    /** Returns a fresh login, so the app shows the new name. */
    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): AuthResponse

    @PUT("api/auth/password")
    suspend fun changePassword(@Body body: PasswordChangeRequest)

    // ---------- CatalogController (FR-02) ----------
    @GET("api/shops")
    suspend fun shops(
        @Query("service") service: String?,
        @Query("sort") sort: String?,
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
    ): List<Shop>

    @GET("api/shops/{shopId}")
    suspend fun shop(@Path("shopId") shopId: Long, @Query("lat") lat: Double?, @Query("lng") lng: Double?): Shop

    // ---------- CustomerController ----------
    @GET("api/customer/addresses")
    suspend fun addresses(): List<Address>

    @POST("api/customer/addresses")
    suspend fun addAddress(@Body body: AddressRequest): Address

    @PUT("api/customer/addresses/{addressId}")
    suspend fun updateAddress(@Path("addressId") addressId: Long, @Body body: AddressRequest): Address

    @DELETE("api/customer/addresses/{addressId}")
    suspend fun deleteAddress(@Path("addressId") addressId: Long)

    @POST("api/customer/orders")
    suspend fun placeOrder(@Body body: PlaceOrderRequest): Order

    @GET("api/customer/orders")
    suspend fun myOrders(): List<Order>

    @GET("api/customer/orders/{orderId}")
    suspend fun myOrder(@Path("orderId") orderId: String): Order

    @POST("api/customer/orders/{orderId}/cancel")
    suspend fun cancelOrder(@Path("orderId") orderId: String): Order

    @POST("api/customer/orders/{orderId}/rating")
    suspend fun rateOrder(@Path("orderId") orderId: String, @Body body: RatingRequest): Order

    @POST("api/customer/orders/{orderId}/disputes")
    suspend fun openDispute(@Path("orderId") orderId: String, @Body body: DisputeRequest): Dispute

    @GET("api/customer/disputes")
    suspend fun myDisputes(): List<Dispute>

    // ---------- ShopOwnerController ----------
    @GET("api/shop/me")
    suspend fun myShop(): Shop

    @PUT("api/shop/me")
    suspend fun updateMyShop(@Body body: ShopUpdateRequest): Shop

    @POST("api/shop/services")
    suspend fun addService(@Body body: ServiceRequest): Shop

    @PUT("api/shop/services/{serviceId}")
    suspend fun updateService(@Path("serviceId") serviceId: Long, @Body body: ServiceRequest): Shop

    @DELETE("api/shop/services/{serviceId}")
    suspend fun removeService(@Path("serviceId") serviceId: Long): Shop

    @GET("api/shop/orders")
    suspend fun shopOrders(): List<Order>

    @POST("api/shop/orders/{orderId}/accept")
    suspend fun acceptOrder(@Path("orderId") orderId: String): Order

    @POST("api/shop/orders/{orderId}/reject")
    suspend fun rejectOrder(@Path("orderId") orderId: String, @Body body: RejectRequest): Order

    @POST("api/shop/orders/{orderId}/ready")
    suspend fun markReady(@Path("orderId") orderId: String): Order

    @GET("api/shop/earnings")
    suspend fun shopEarnings(): ShopEarnings

    @GET("api/shop/payout-details")
    suspend fun shopPayoutDetails(): PayoutDetails

    @PUT("api/shop/payout-details")
    suspend fun updateShopPayoutDetails(@Body body: PayoutDetails): PayoutDetails

    // ---------- DriverController ----------
    @GET("api/driver/me")
    suspend fun driverMe(): DriverProfile

    @PUT("api/driver/availability")
    suspend fun setAvailability(@Body body: AvailabilityRequest): DriverProfile

    @POST("api/driver/location")
    suspend fun sendLocation(@Body body: LocationRequest): DriverProfile

    @GET("api/driver/jobs")
    suspend fun driverJobs(): List<Order>

    @POST("api/driver/jobs/{orderId}/picked-up")
    suspend fun pickedUp(@Path("orderId") orderId: String): Order

    /** Delivery trip: driver has the clean laundry from the shop (locks the job to this driver). */
    @POST("api/driver/jobs/{orderId}/collected")
    suspend fun collectedFromShop(@Path("orderId") orderId: String): Order

    /** "Can't do this job": the job goes to the next nearest driver. */
    @POST("api/driver/jobs/{orderId}/decline")
    suspend fun declineJob(@Path("orderId") orderId: String, @Body body: RejectRequest)

    @POST("api/driver/jobs/{orderId}/delivered")
    suspend fun delivered(@Path("orderId") orderId: String, @Body body: DropOffRequest): Order

    @GET("api/driver/history")
    suspend fun driverHistory(): DriverHistory

    /** LaundryHub's own ABA account and QR code: where the driver sends the customers' cash. */
    @GET("api/driver/laundryhub-aba")
    suspend fun laundryHubAba(): PayoutDetails

    /** Cash on delivery: the driver sent the customers' cash to LaundryHub; the admin confirms it. */
    @POST("api/driver/cash/sent")
    suspend fun cashSent(@Body body: CashSentRequest)

    @GET("api/driver/payout-details")
    suspend fun driverPayoutDetails(): PayoutDetails

    @PUT("api/driver/payout-details")
    suspend fun updateDriverPayoutDetails(@Body body: PayoutDetails): PayoutDetails
}
