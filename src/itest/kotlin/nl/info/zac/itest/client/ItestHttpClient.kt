/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.info.zac.itest.config.ItestConfiguration.HTTP_READ_TIMEOUT_SECONDS
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_PORT
import nl.info.zac.itest.config.TestUser
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URI
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
class ItestHttpClient {
    private val logger = KotlinLogging.logger {}
    private var okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    fun connectNewWebSocket(
        url: String,
        webSocketListener: WebSocketListener,
        headers: Headers = buildHeaders(acceptType = null),
        testUser: TestUser
    ): WebSocket {
        val tokens = authenticate(testUser)
        logger.info { "Connecting new websocket on: '$url'" }
        val request = Request.Builder()
            .headers(cloneHeadersWithAuthorization(headers, url, tokens.first))
            .url(url)
            .build()
        val webSocket = okHttpClient.newWebSocket(request, webSocketListener)
        logout(testUser, tokens.second)
        return webSocket
    }

    fun performDeleteRequest(
        url: String,
        headers: Headers = buildHeaders(),
        testUser: TestUser? = null
    ): ResponseContent {
        val tokens = testUser?.let(::authenticate)
        logger.info { "Performing DELETE request on: '$url'" }
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
            .delete()
            .build()
        val responseContent = okHttpClient.newCall(request).execute().use {
            logger.info { "Received response with status code: '${it.code}'" }
            ResponseContent(it.body.string(), it.headers, it.code)
        }
        tokens?.let { logout(testUser, it.second) }
        return responseContent
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
        tokens?.let { logout(testUser, it.second) }
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
