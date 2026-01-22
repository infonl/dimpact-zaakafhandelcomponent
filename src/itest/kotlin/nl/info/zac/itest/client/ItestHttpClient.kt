/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.info.zac.itest.config.ItestConfiguration.HTTP_READ_TIMEOUT_SECONDS
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_PORT
import nl.info.zac.itest.config.ItestConfiguration.ZAC_BASE_URI
import nl.info.zac.itest.config.TestUser
import okhttp3.Headers
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
class ItestHttpClient {
    private val logger = KotlinLogging.logger {}
    private var okHttpClient: OkHttpClient

    init {
        // use a non-persistent cookie manager, so that we can reuse HTTP sessions across requests
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
        okHttpClient = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .readTimeout(HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
    }

    // TODO: create generic function for all HTTP methods that does:
    // 1. log in, 2. perform request, 3. log out

    fun performDeleteRequest(
        url: String,
        headers: Headers = buildHeaders(),
        addAuthorizationHeader: Boolean = true
    ): ResponseContent {
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
        return okHttpClient.newCall(request).execute().use {
            logger.info { "Received response with status code: '${it.code}'" }
            ResponseContent(it.body.string(), it.headers, it.code)
        }
    }

    fun performGetRequest(
        url: String,
        headers: Headers = buildHeaders(acceptType = null),
        testUser: TestUser? = null
    ): ResponseContent {
        val tokens = testUser?.let(::authenticate)
        logger.info { "Performing GET request on: '$url'" }
        val request = Request.Builder()
            .headers(
                tokens?.let {
                    cloneHeadersWithAuthorization(
                        headers = headers,
                        url = url,
                        accessToken = tokens.first
                    )
                } ?: run {
                    headers
                }
            )
            .url(url)
            .get()
            .build()
        val responseContent = okHttpClient.newCall(request).execute().use {
            logger.info { "Received response with status code: '${it.code}'" }
            ResponseContent(it.body.string(), it.headers, it.code)
        }
        tokens?.let{ logout(testUser, it.second) }
        // TODO test, log out from ZAC
        // tokens?.run { performGetRequest(url = "$ZAC_BASE_URI/logout") }
        return responseContent
    }

    fun performZgwApiGetRequest(
        url: String,
        headers: Headers = buildHeaders(acceptType = null)
    ): ResponseContent {
        logger.info { "Performing GET request on: '$url'" }
        val request = Request.Builder()
            .headers(
                cloneHeadersWithAuthorization(
                    headers = headers,
                    url = url
                )
            )
            .url(url)
            .get()
            .build()
        val responseContent = okHttpClient.newCall(request).execute().use {
            logger.info { "Received response with status code: '${it.code}'" }
            ResponseContent(it.body.string(), it.headers, it.code)
        }
        return responseContent
    }

    fun performHeadRequest(
        url: String,
        headers: Headers = buildHeaders(acceptType = null)
    ): Int {
        logger.info { "Performing HEAD request on: '$url'" }
        val request = Request.Builder()
            .headers(headers)
            .url(url)
            .head()
            .build()
        return okHttpClient.newCall(request).execute().use {
            logger.info { "Received response with status code: '${it.code}'" }
            it.code
        }
    }

    fun performPostRequest(
        url: String,
        headers: Headers = buildHeaders(),
        requestBody: RequestBody,
        testUser: TestUser? = null
    ): ResponseContent {
        val tokens = testUser?.let(::authenticate)
        logger.info { "Performing POST request on: '$url'" }
        val request = Request.Builder()
            .headers(
                tokens?.let {
                    cloneHeadersWithAuthorization(headers, url, tokens.first)
                } ?: run {
                    headers
                }
            )
            .url(url)
            .post(requestBody)
            .build()
        val responseContent = okHttpClient.newCall(request).execute().use {
            logger.info { "Received response with status code: '${it.code}'" }
            ResponseContent(it.body.string(), it.headers, it.code)
        }
        tokens?.let { logout(testUser, it.second) }
        return responseContent
    }

    fun performJSONPostRequest(
        url: String,
        headers: Headers = buildHeaders(),
        requestBodyAsString: String,
        testUser: TestUser? = null
    ) = performPostRequest(
        url = url,
        headers = headers,
        requestBody = requestBodyAsString.toRequestBody(MediaType.APPLICATION_JSON.toMediaType()),
        testUser = testUser
    )

    fun performPatchRequest(
        url: String,
        headers: Headers = buildHeaders(),
        requestBodyAsString: String,
        testUser: TestUser? = null
    ): ResponseContent {
        val tokens = testUser?.let(::authenticate)
        logger.info { "Performing PATCH request on: '$url'" }
        val request = Request.Builder()
            .headers(
                tokens?.let {
                    cloneHeadersWithAuthorization(headers, url, tokens.first)
                } ?: run {
                    headers
                }
            )
            .url(url)
            .patch(requestBodyAsString.toRequestBody(MediaType.APPLICATION_JSON.toMediaType()))
            .build()
        val responseContent = okHttpClient.newCall(request).execute().use {
            logger.info { "Received response with status code: '${it.code}'" }
            ResponseContent(it.body.string(), it.headers, it.code)
        }
        tokens?.let { logout(testUser, it.second) }
        return responseContent
    }

    fun performPutRequest(
        url: String,
        headers: Headers = buildHeaders(),
        requestBodyAsString: String,
        testUser: TestUser? = null
    ): ResponseContent {
        val tokens = testUser?.let(::authenticate)
        logger.info { "Performing PUT request on: '$url'" }
        val request = Request.Builder()
            .headers(
                tokens?.let {
                    cloneHeadersWithAuthorization(headers, url, tokens.first)
                } ?: run {
                    headers
                }
            ).url(url)
            .put(requestBodyAsString.toRequestBody(MediaType.APPLICATION_JSON.toMediaType()))
            .build()
        val responseContent = okHttpClient.newCall(request).execute().use {
            logger.info { "Received response with status code: '${it.code}'" }
            ResponseContent(it.body.string(), it.headers, it.code)
        }
        tokens?.let { logout(testUser, it.second) }
        return responseContent
    }

    fun connectNewWebSocket(
        url: String,
        webSocketListener: WebSocketListener,
        headers: Headers = buildHeaders(acceptType = null),
        // TODO: for releasing zaken from the list authorisation seems to be required, but for
        // assigning zaken from the list not? see ZaakRestServiceTest
        // websockets are not secured at all I think?
        testUser: TestUser? = null
    ): WebSocket {
        val tokens = testUser?.let(::authenticate)
        logger.info { "Connecting new websocket on: '$url'" }
        val request = Request.Builder()
            .headers(
                tokens?.let {
                    cloneHeadersWithAuthorization(headers, url, tokens.first)
                } ?: run {
                    headers
                }
            )
            .url(url)
            .build()
        val webSocket = okHttpClient.newWebSocket(
            request,
            webSocketListener
        )
        tokens?.let { logout(testUser, it.second) }
        return webSocket
    }

    private fun cloneHeadersWithAuthorization(headers: Headers, url: String): Headers =
        headers.newBuilder().add(Header.AUTHORIZATION.name, generateBearerToken(url)).build()

    private fun cloneHeadersWithAuthorization(headers: Headers, url: String, accessToken: String): Headers =
        headers.newBuilder().add(Header.AUTHORIZATION.name, generateBearerToken(url, accessToken)).build()

    private fun generateBearerToken(url: String, accessToken: String? = null) = "Bearer " + if (URI(url).port == OPEN_ZAAK_EXTERNAL_PORT) {
        generateOpenZaakJwtToken()
    } else {
        accessToken
    }
}
