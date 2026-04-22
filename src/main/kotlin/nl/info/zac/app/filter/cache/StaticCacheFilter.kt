/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.filter.cache

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlin.time.Duration.Companion.days

/**
 * Overrides Undertow's default `no-cache, no-store` headers on static assets.
 *
 * Undertow writes response headers when [getOutputStream] or [getWriter] is first called — at that
 * point the response is not yet committed, so headers can still be set. A [ResponseWrapper]
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
@WebFilter(filterName = "StaticCacheFilter", urlPatterns = ["/*"])
class StaticCacheFilter : Filter {

    companion object {
        /** Max-age for immutable assets: 1 year in seconds, the conventional maximum for `Cache-Control: immutable`. */
        private val MAX_AGE_SECONDS = 365.days.inWholeSeconds

        /** 8 is Angular's default hash length (hardcoded in `@angular/build`); filename example: main-A1B2C3D4.js */
        private val HASHED_RESOURCE_REGEX = Regex("""-[A-Za-z0-9]{8}\.(js|css)(\.map)?$""")

        /** 8 must match `.substring(0, 8)` in `scripts/cache-busting.js`; `?v=` param example: 395afa0f */
        private val MD5_VERSION_REGEX = Regex("""^[0-9a-f]{8}$""")
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as? HttpServletRequest
        val cacheControl = httpRequest?.let { resolveCacheControl(it) }
        if (cacheControl != null && httpRequest != null && response is HttpServletResponse) {
            chain.doFilter(RequestWrapper(httpRequest), ResponseWrapper(response, cacheControl))
        } else {
            chain.doFilter(request, response)
        }
    }

    private fun resolveCacheControl(request: HttpServletRequest): String? {
        val path = request.requestURI.removePrefix(request.contextPath)
        return when {
            HASHED_RESOURCE_REGEX.containsMatchIn(path) ||
                (path.startsWith("/assets/") && MD5_VERSION_REGEX.matches(request.getParameter("v") ?: "")) ->
                "public, max-age=$MAX_AGE_SECONDS, immutable"
            path == "/" || path == "/index.html" -> "no-cache"
            else -> null
        }
    }

}
