/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest.client

import okhttp3.Headers.Companion.toHeaders
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
) = mutableMapOf<String, String>().apply {
    contentType?.let { put(Header.CONTENT_TYPE.value, it.value) }
    acceptType?.let { put(Header.ACCEPT.value, it.value) }
    authorization?.let { put(Header.AUTHORIZATION.value, it) }
}.toHeaders()
