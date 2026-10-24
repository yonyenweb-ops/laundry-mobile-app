package com.laundryhub.mobile.data

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/** Turns any error into a sentence we can show to the user. */
fun Throwable.userMessage(): String = when (this) {
    is HttpException -> {
        val body = try {
            response()?.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        val parsed = try {
            Gson().fromJson(body, ErrorBody::class.java)
        } catch (e: Exception) {
            null
        }
        if (code() == 401) {
            Session.logout()      // token expired or database was reset -> back to login
            "Please log in again"
        } else {
            parsed?.message ?: "Server error ${code()}"
        }
    }
    is IOException -> "Cannot reach the server at ${Session.serverUrl}\nIs the backend running?"
    else -> message ?: "Something went wrong"
}

/** HTTP status code of an API error, or null. Used for e.g. 402 Payment declined. */
fun Throwable.httpCode(): Int? = (this as? HttpException)?.code()
