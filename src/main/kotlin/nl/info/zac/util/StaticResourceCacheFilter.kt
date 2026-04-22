/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.util

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import java.io.PrintWriter

/**
 * Overrides Undertow's default `no-cache, no-store` headers on static assets.
 *
 * Undertow writes response headers when [getOutputStream] or [getWriter] is first called — at that
 * point the response is not yet committed, so headers can still be set. A [CacheControlResponseWrapper]
 * intercepts both calls to inject the correct Cache-Control value and suppress the legacy
 * `Pragma` and `Expires` headers Undertow also sets.
 *
 * Path matching uses `requestURI.removePrefix(contextPath)` rather than `servletPath`, because
 * `servletPath` can be empty for the default servlet mapping (`/`).
 *
 * - Hashed JS/CSS bundles: `immutable` — content hash guarantees freshness
 * - Versioned `/assets/` files with a valid `?v=` MD5 param: `immutable`
 * - `index.html`: `no-cache` — must revalidate so new chunk references are picked up after deploy
 */
@WebFilter(filterName = "StaticResourceCacheFilter", urlPatterns = ["/*"])
class StaticResourceCacheFilter : Filter {

    companion object {
        /** 8 is Angular's default hash length (hardcoded in `@angular/build`); filename example: main-A1B2C3D4.js */
        private val HASHED_RESOURCE_REGEX = Regex("""-[A-Za-z0-9]{8}\.(js|css)(\.map)?$""")

        /** 8 must match `.substring(0, 8)` in `scripts/cache-busting.js`; `?v=` param example: 395afa0f */
        private val MD5_VERSION_REGEX = Regex("""^[0-9a-f]{8}$""")
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val wrappedResponse = (request as? HttpServletRequest)
            ?.let { httpRequest -> resolveCacheControl(httpRequest) }
            ?.let { cacheControl ->
                (response as? HttpServletResponse)?.let { CacheControlResponseWrapper(it, cacheControl) }
            }
            ?: response
        chain.doFilter(request, wrappedResponse)
    }

    private fun resolveCacheControl(request: HttpServletRequest): String? {
        val path = request.requestURI.removePrefix(request.contextPath)
        return when {
            HASHED_RESOURCE_REGEX.containsMatchIn(path) ||
                (path.startsWith("/assets/") && MD5_VERSION_REGEX.matches(request.getParameter("v") ?: "")) ->
                "public, max-age=31536000, immutable"
            path == "/" || path == "/index.html" -> "no-cache"
            else -> null
        }
    }

    /** Blocks Undertow from setting cache-related headers; injects the correct value on [getOutputStream] and [getWriter]. */
    private class CacheControlResponseWrapper(
        response: HttpServletResponse,
        private val cacheControl: String
    ) : HttpServletResponseWrapper(response) {

        private fun isCacheHeader(name: String) =
            name.equals("Cache-Control", ignoreCase = true) ||
                name.equals("Pragma", ignoreCase = true) ||
                name.equals("Expires", ignoreCase = true)

        override fun setHeader(name: String, value: String) { if (!isCacheHeader(name)) super.setHeader(name, value) }
        override fun addHeader(name: String, value: String) { if (!isCacheHeader(name)) super.addHeader(name, value) }
        override fun setIntHeader(name: String, value: Int) {
            if (!isCacheHeader(name)) super.setIntHeader(name, value)
        }

        override fun addIntHeader(name: String, value: Int) {
            if (!isCacheHeader(name)) super.addIntHeader(name, value)
        }

        override fun setDateHeader(name: String, date: Long) {
            if (!isCacheHeader(name)) super.setDateHeader(name, date)
        }

        override fun addDateHeader(name: String, date: Long) {
            if (!isCacheHeader(name)) super.addDateHeader(name, date)
        }

        private fun applyCacheHeaders() {
            super.setHeader("Cache-Control", cacheControl)
        }

        override fun getOutputStream(): ServletOutputStream {
            applyCacheHeaders()
            return super.getOutputStream()
        }

        override fun getWriter(): PrintWriter {
            applyCacheHeaders()
            return super.getWriter()
        }
    }

}
