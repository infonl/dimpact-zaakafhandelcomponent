/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest.client

import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import java.util.UUID

enum class MediaType(val value: String) {
    APPLICATION_JSON("application/json"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    fun toMediaType() = value.toMediaType()
}

enum class Header(val value: String) {
    CONTENT_TYPE("Content-Type"),
    ACCEPT("Accept"),
    AUTHORIZATION("Authorization"),
    CORRELATION_ID("X-Correlation-ID"),
}

fun buildHeaders(
    contentType: MediaType? = MediaType.APPLICATION_JSON,
    acceptType: MediaType? = MediaType.APPLICATION_JSON,
    authorization: String? = null,
    correlationId: UUID = UUID.randomUUID()
) = mutableMapOf<String, String>().apply {
    contentType?.let { put(Header.CONTENT_TYPE.value, it.value) }
    acceptType?.let { put(Header.ACCEPT.value, it.value) }
    authorization?.let { put(Header.AUTHORIZATION.value, it) }
    correlationId.let { put(Header.CORRELATION_ID.value, it.toString()) }
}.toHeaders()
