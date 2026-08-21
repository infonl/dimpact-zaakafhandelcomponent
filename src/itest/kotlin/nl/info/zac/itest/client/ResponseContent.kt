/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import okhttp3.Headers

/**
 * The response body is kept as bytes because it can only be read from the connection once,
 * so that both textual and binary responses can be asserted on.
 */
class ResponseContent(
    val bodyAsBytes: ByteArray,
    val headers: Headers,
    val code: Int
) {
    val bodyAsString: String by lazy { bodyAsBytes.decodeToString() }
}
