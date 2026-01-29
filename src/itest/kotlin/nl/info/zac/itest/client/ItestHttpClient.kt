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

    /**
     * Connects a new WebSocket to the given URL with the provided [webSocketListener] and optional headers.
     * The [testUser] will be authenticated in Keycloak before the WebSocket connection is established,
     * and will be logged out from Keycloak afterwards.
     *
     * @param url The URL to connect the WebSocket to.
     * @param webSocketListener The [WebSocketListener] to handle WebSocket events.
     * @param headers Optional headers to include in the WebSocket request. Defaults to standard headers.
     * @param testUser The [TestUser] to authenticate to Keycloak before establishing the WebSocket connection.
     * @return The established [WebSocket] connection.
     */
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

    /**
     * Performs a DELETE request on the given URL with optional headers.
     * If a [testUser] is provided, the user will be authenticated in Keycloak before the request is performed,
     * and will be logged out from Keycloak afterwards.
     *
     * @param url The URL to perform the DELETE request on.
     * @param headers Optional headers to include in the request. Defaults to standard headers.
     * @param testUser Optional [TestUser] to authenticate to Keycloak before performing the request.
     * If provided, the user will be logged out afterwards.
     * @return A [ResponseContent] containing the response body, headers, and status code
     */
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

    /**
     * Performs a GET request on the given URL with optional headers.
     * If a [testUser] is provided, the user will be authenticated in Keycloak before the request is performed,
     * and will be logged out from Keycloak afterwards.
     *
     * @param url The URL to perform the GET request on.
     * @param headers Optional headers to include in the request. Defaults to standard headers.
     * @param testUser Optional [TestUser] to authenticate to Keycloak before performing the request.
     * If provided, the user will be logged out afterwards.
     * @return A [ResponseContent] containing the response body, headers, and status code
     */
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

    /**
     * Performs a HEAD request on the given URL with optional headers.
     *
     * @param url The URL to perform the HEAD request on.
     * @param headers Optional headers to include in the request. Defaults to standard headers.
     * @return The HTTP status code of the response.
     */
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

    /**
     * Performs a POST request on the given URL with optional headers and request body.
     * If a [testUser] is provided, the user will be authenticated in Keycloak before the request is performed,
     * and will be logged out from Keycloak afterwards.
     *
     * @param url The URL to perform the POST request on.
     * @param headers Optional headers to include in the request. Defaults to standard headers.
     * @param requestBody The body of the POST request.
     * @param testUser Optional [TestUser] to authenticate to Keycloak before performing the request.
     * If provided, the user will be logged out afterwards.
     * @return A [ResponseContent] containing the response body, headers, and status code
     */
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

    /**
     * Performs a POST request with a JSON body on the given URL with optional headers.
     * If a [testUser] is provided, the user will be authenticated in Keycloak before the request is performed,
     * and will be logged out from Keycloak afterwards.
     *
     * @param url The URL to perform the POST request on.
     * @param headers Optional headers to include in the request. Defaults to standard headers.
     * @param requestBodyAsString The JSON body of the POST request as a string.
     * @param testUser Optional [TestUser] to authenticate to Keycloak before performing the request.
     * If provided, the user will be logged out afterwards.
     * @return A [ResponseContent] containing the response body, headers, and status code
     */
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

    /**
     * Performs a PATCH request with a JSON body on the given URL with optional headers.
     * If a [testUser] is provided, the user will be authenticated in Keycloak before the request is performed,
     * and will be logged out from Keycloak afterwards.
     *
     * @param url The URL to perform the PATCH request on.
     * @param headers Optional headers to include in the request. Defaults to standard headers.
     * @param requestBodyAsString The JSON body of the PATCH request as a string.
     * @param testUser Optional [TestUser] to authenticate to Keycloak before performing the request.
     * If provided, the user will be logged out afterwards.
     * @return A [ResponseContent] containing the response body, headers, and status code
     */
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

    /**
     * Performs a PUT request with a JSON body on the given URL with optional headers.
     * If a [testUser] is provided, the user will be authenticated in Keycloak before the request is performed,
     * and will be logged out from Keycloak afterwards.
     *
     * @param url The URL to perform the PUT request on.
     * @param headers Optional headers to include in the request. Defaults to standard headers.
     * @param requestBodyAsString The JSON body of the PUT request as a string.
     * @param testUser Optional [TestUser] to authenticate to Keycloak before performing the request.
     * If provided, the user will be logged out afterwards.
     * @return A [ResponseContent] containing the response body, headers, and status code
     */
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

    /**
     * Performs a ZGW API GET request on the given URL with optional headers.
     */
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
