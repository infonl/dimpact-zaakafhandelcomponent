/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

/**
 * Differs from [urlEncode] in that spaces in URL path segments should be encoded as '%20', not '+'.
 */
fun String.encodeUrlPathSegment(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)
    .replace("+", "%20")
