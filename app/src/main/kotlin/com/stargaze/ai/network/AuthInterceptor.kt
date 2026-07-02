package com.stargaze.ai.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Attaches the client session token (if present) as a Bearer header, and hard-fails any attempt to
 * send a request over a non-HTTPS scheme. This is defence-in-depth on top of the network security
 * config: even a misconfigured base URL cannot leak a token over cleartext.
 */
class AuthInterceptor(private val securePrefs: SecurePrefs) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!request.url.isHttps) {
            throw IOException("Refusing to send request over insecure transport: ${request.url}")
        }

        val token = securePrefs.getSessionToken()
        val authed = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("X-Client", "stargaze-android")
                .build()
        } else {
            request.newBuilder().header("X-Client", "stargaze-android").build()
        }
        return chain.proceed(authed)
    }
}
