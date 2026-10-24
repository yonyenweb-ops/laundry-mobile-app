# LaundryHub Android app (Kotlin + Jetpack Compose)

One app for three roles. After login the backend returns the role and the app shows:

- **Customer** (`ui/customer`): shops (FR-02), place order & pay (FR-03/04), live tracking with map + delivery code (FR-08), rating (FR-09), refund request (FR-10), saved addresses (FR-01)
- **Laundry shop** (`ui/shop`): new orders with 10-minute countdown, accept/reject (FR-05), mark ready (FR-07), services & prices, open/closed
- **Driver** (`ui/driver`): go online, live GPS (FR-08), pickup & delivery jobs (FR-06), confirm delivery with the customer's code

Open this folder in Android Studio and press ▶. Server address: login screen → ⚙ Server settings
(emulator `http://10.0.2.2:8080/`, real phone `http://YOUR-PC-IP:8080/`).

`data/LaundryApi.kt` lists every API call. `data/Session.kt` keeps the login token.
