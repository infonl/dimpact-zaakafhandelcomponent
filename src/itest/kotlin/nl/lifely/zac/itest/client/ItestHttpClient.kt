/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_READ_TIMEOUT_SECONDS
import nl.lifely.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_PORT
import okhttp3.Headers
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.util.concurrent.TimeUnit

private const val APPLICATION_JSON = "application/json"
private val MEDIA_TYPE_APPLICATION_JSON = APPLICATION_JSON.toMediaType()
private const val HEADER_AUTHORIZATION = "Authorization"

@Suppress("TooManyFunctions")
class ItestHttpClient {
    private var okHttpClient: OkHttpClient
    private val logger = KotlinLogging.logger {}

    init {
        // use a non-persistent cookie manager to we can reuse HTTP sessions across requests
        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        okHttpClient = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .readTimeout(HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
    }

    fun performDeleteRequest(
        url: String,
        headers: Headers = buildHeaders(),
        addAuthorizationHeader: Boolean = true
    ): Response {
        logger.info { "Performing DELETE request on: '$url'" }
        val request = Request.Builder()
            .headers(
                if (addAuthorizationHeader) {
                    cloneHeadersWithAuthorization(headers, url)
                } else {
                    headers
                }
            )
            .url(url)
            .delete()
            .build()
        return okHttpClient.newCall(request).execute()
    }

    fun performGetRequest(
        url: String,
        headers: Headers = buildHeaders(acceptType = null),
        addAuthorizationHeader: Boolean = true
    ): Response {
        logger.info { "Performing GET request on: '$url'" }
        val request = Request.Builder()
            .headers(
                if (addAuthorizationHeader) {
                    cloneHeadersWithAuthorization(headers, url)
                } else {
                    headers
                }
            )
            .url(url)
            .get()
            .build()
        return okHttpClient.newCall(request).execute()
    }

    fun performPostRequest(
        url: String,
        headers: Headers = buildHeaders(),
        requestBody: RequestBody,
        addAuthorizationHeader: Boolean = true
    ): Response {
        logger.info { "Performing POST request on: '$url'" }
        val request = Request.Builder()
            .headers(
                if (addAuthorizationHeader) {
                    cloneHeadersWithAuthorization(headers, url)
                } else {
                    headers
                }
            )
            .url(url)
            .post(requestBody)
            .build()
        return okHttpClient.newCall(request).execute()
    }

    fun performJSONPostRequest(
        url: String,
        headers: Headers = buildHeaders(),
        requestBodyAsString: String,
        addAuthorizationHeader: Boolean = true
    ) = performPostRequest(
        url = url,
        headers = headers,
        requestBody = requestBodyAsString.toRequestBody(MEDIA_TYPE_APPLICATION_JSON),
        addAuthorizationHeader = addAuthorizationHeader
    )

    fun performPatchRequest(
        url: String,
        headers: Headers = buildHeaders(),
        requestBodyAsString: String,
        addAuthorizationHeader: Boolean = true
    ): Response {
        logger.info { "Performing PATCH request on: '$url'" }
        val request = Request.Builder()
            .headers(
                if (addAuthorizationHeader) {
                    cloneHeadersWithAuthorization(headers, url)
                } else {
                    headers
                }
            )
            .url(url)
            .patch(requestBodyAsString.toRequestBody(MEDIA_TYPE_APPLICATION_JSON))
            .build()
        return okHttpClient.newCall(request).execute()
    }

    fun performPutRequest(
        url: String,
        headers: Headers = buildHeaders(),
        requestBodyAsString: String,
        addAuthorizationHeader: Boolean = true
    ): Response {
        logger.info { "Performing PUT request on: '$url'" }
        val request = Request.Builder()
            .headers(
                if (addAuthorizationHeader) {
                    cloneHeadersWithAuthorization(headers, url)
                } else {
                    headers
                }
            ).url(url)
            .put(requestBodyAsString.toRequestBody(MEDIA_TYPE_APPLICATION_JSON))
            .build()
        return okHttpClient.newCall(request).execute()
    }

    fun connectNewWebSocket(
        url: String,
        webSocketListener: WebSocketListener,
        headers: Headers = buildHeaders(acceptType = null),
        addAuthorizationHeader: Boolean = true
    ): WebSocket {
        logger.info { "Connecting new websocket on: '$url'" }
        val request = Request.Builder()
            .headers(
                if (addAuthorizationHeader) {
                    cloneHeadersWithAuthorization(headers, url)
                } else {
                    headers
                }
            )
            .url(url)
            .build()
        return okHttpClient.newWebSocket(
            request,
            webSocketListener
        )
    }

    fun shutdownClient() = okHttpClient.dispatcher.executorService.shutdown()

    private fun cloneHeadersWithAuthorization(headers: Headers, url: String): Headers {
        val token = if (URI(url).port == OPEN_ZAAK_EXTERNAL_PORT) {
            generateToken()
        } else {
            KeycloakClient.requestAccessToken()
        }
        return headers.newBuilder().add(HEADER_AUTHORIZATION, "Bearer $token").build()
    }
}

private const val HEADER_CONTENT_TYPE = "Content-Type"
private const val HEADER_ACCEPT = "Accept"

fun buildHeaders(
    contentType: String? = APPLICATION_JSON,
    acceptType: String? = APPLICATION_JSON,
    authorization: String? = null
): Headers = Headers.Builder().also {
    if (contentType != null) it.add(HEADER_CONTENT_TYPE, contentType)
    if (acceptType != null) it.add(HEADER_ACCEPT, acceptType)
    if (authorization != null) it.add(HEADER_AUTHORIZATION, authorization)
}.build()
