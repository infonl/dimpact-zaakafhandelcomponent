/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import okhttp3.Headers

data class ResponseContent(
    val bodyAsString: String,
    val headers: Headers,
    val code: Int
)
