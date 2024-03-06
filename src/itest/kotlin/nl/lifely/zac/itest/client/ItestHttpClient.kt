package nl.lifely.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Headers
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.CookieManager
import java.net.CookiePolicy

class ItestHttpClient {
    private var okHttpClient: OkHttpClient
    private val logger = KotlinLogging.logger {}

    init {
        // use a non-persistent cookie manager to we can reuse HTTP sessions across requests
        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        okHttpClient = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .build()
    }

    fun performGetRequest(
        url: String,
        headers: Headers = getDefaultJSONGETHeaders()
    ): Response {
        logger.info { "Performing GET request on: '$url'" }
        val request = Request.Builder()
            .headers(headers)
            .url(url)
            .get()
            .build()
        return okHttpClient.newCall(request).execute()
    }

    fun performPostRequest(
        url: String,
        headers: Headers = getDefaultJSONHeaders(),
        requestBody: RequestBody
    ): Response {
        logger.info { "Performing POST request on: '$url'" }
        val request = Request.Builder()
            .headers(headers)
            .url(url)
            .post(requestBody)
            .build()
        return okHttpClient.newCall(request).execute()
    }

    fun performJSONPostRequest(
        url: String,
        headers: Headers = getDefaultJSONHeaders(),
        requestBodyAsString: String
    ) = performPostRequest(
        url = url,
        headers = headers,
        requestBody = requestBodyAsString.toRequestBody("application/json".toMediaType())
    )

    fun performPatchRequest(
        url: String,
        headers: Headers = getDefaultJSONHeaders(),
        requestBodyAsString: String
    ): Response {
        logger.info { "Performing PATCH request on: '$url'" }
        val request = Request.Builder()
            .headers(headers)
            .url(url)
            .patch(requestBodyAsString.toRequestBody("application/json".toMediaType()))
            .build()
        return okHttpClient.newCall(request).execute()
    }

    fun performPutRequest(
        url: String,
        headers: Headers = getDefaultJSONHeaders(),
        requestBodyAsString: String
    ): Response {
        logger.info { "Performing PUT request on: '$url'" }
        val request = Request.Builder()
            .headers(headers)
            .url(url)
            .put(requestBodyAsString.toRequestBody("application/json".toMediaType()))
            .build()
        return okHttpClient.newCall(request).execute()
    }

    private fun getDefaultJSONGETHeaders() = Headers.headersOf(
        "Authorization",
        // perform a request to Keycloak to get an access token
        // this can only be done after a successfull authentication
        "Bearer ${KeycloakClient.requestAccessToken()}",
        "Accept",
        "application/json"
    )

    private fun getDefaultJSONHeaders() =
        getDefaultJSONGETHeaders()
            .newBuilder()
            .add("Content-Type", "application/json")
            .build()
}
