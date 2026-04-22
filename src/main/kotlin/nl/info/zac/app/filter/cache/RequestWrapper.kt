/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.filter.cache

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.util.Collections

/**
 * Strips conditional request headers (If-None-Match, If-Modified-Since) so Undertow always
 * responds with 200 instead of 304 for immutable resources. Without this, Undertow sets
 * `Cache-Control: no-cache, no-store` at exchange level on 304 responses — bypassing our
 * [ResponseWrapper] — causing the browser to discard its cached entry and repeat the cycle.
 *
 * After one 200 with `Cache-Control: immutable`, the browser serves from memory and never
 * sends conditional headers again, so the bandwidth cost of stripping is a one-time hit.
 */
class RequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    private fun isConditionalHeader(name: String) =
        name.equals("If-None-Match", ignoreCase = true) ||
            name.equals("If-Modified-Since", ignoreCase = true)

    override fun getHeader(name: String): String? =
        if (isConditionalHeader(name)) null else super.getHeader(name)

    override fun getHeaders(name: String): java.util.Enumeration<String> =
        if (isConditionalHeader(name)) Collections.emptyEnumeration() else super.getHeaders(name)

    override fun getDateHeader(name: String): Long =
        if (isConditionalHeader(name)) -1L else super.getDateHeader(name)
}
