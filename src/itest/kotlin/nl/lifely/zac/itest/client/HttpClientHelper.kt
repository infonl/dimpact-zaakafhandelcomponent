/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.client

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType

enum class MediaType(val value: String) {
    APPLICATION_JSON("application/json");

    fun toMediaType() = value.toMediaType()
}

enum class Header(val value: String) {
    CONTENT_TYPE("Content-Type"),
    ACCEPT("Accept"),
    AUTHORIZATION("Authorization")
}

fun buildHeaders(
    contentType: MediaType? = MediaType.APPLICATION_JSON,
    acceptType: MediaType? = MediaType.APPLICATION_JSON,
    authorization: String? = null
): Headers = Headers.Builder().also {
    if (contentType != null) it.add(Header.CONTENT_TYPE.value, contentType.value)
    if (acceptType != null) it.add(Header.ACCEPT.value, acceptType.value)
    if (authorization != null) it.add(Header.AUTHORIZATION.value, authorization)
}.build()
