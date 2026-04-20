/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.util

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * Overrides Undertow's default `no-cache, no-store` headers on static assets.
 * Headers are set after [FilterChain.doFilter] so they overwrite what Undertow sets.
 *
 * - Hashed JS/CSS bundles: `immutable` — content hash guarantees freshness
 * - Versioned `/assets/` files with a valid `?v=` MD5 param: `immutable`
 * - `index.html`: `no-cache` — must revalidate so new chunk references are picked up after deploy
 */
@WebFilter(filterName = "staticResourceCacheFilter")
class StaticResourceCacheFilter : Filter {

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        chain.doFilter(request, response)

        if (request is HttpServletRequest && response is HttpServletResponse) {
            when {
                HASHED_RESOURCE_REGEX.containsMatchIn(request.servletPath) ->
                    response.setHeader("Cache-Control", "public, max-age=31536000, immutable")
                request.servletPath.startsWith("/assets/") &&
                    MD5_VERSION_REGEX.matches(request.getParameter("v") ?: "") ->
                    response.setHeader("Cache-Control", "public, max-age=31536000, immutable")
                request.servletPath == "/" || request.servletPath == "/index.html" ->
                    response.setHeader("Cache-Control", "no-cache")
            }
        }
    }

    companion object {
        // Angular build output: 8-character uppercase content hash in filename, e.g. main-A1B2C3D4.js
        private val HASHED_RESOURCE_REGEX = Regex("""-[A-Z0-9]{8}\.(js|css)(\.map)?$""")
        // cache-busting.js generates an 8-character lowercase hex MD5 substring, e.g. 395afa0f
        private val MD5_VERSION_REGEX = Regex("""^[0-9a-f]{8}$""")
    }
}
