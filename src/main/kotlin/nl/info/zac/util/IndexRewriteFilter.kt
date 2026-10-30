/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
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
 * If the requested resource doesn't exist, use index.html
 * more information at <a href="https://angular.io/guide/deployment#server-configuration">server configuration</a>
 */
@WebFilter(filterName = "indexRewriteFilter")
class IndexRewriteFilter : Filter {
    companion object {
        private val RESOURCE_PATHS = listOf("/assets", "/static", "/rest", "/websocket", "/webdav")
        private val REGEX_RESOURCES = Regex(
            "\\.(js(on|\\.map)?|css|txt|jpe?g|png|gif|svg|ico|webmanifest|eot|ttf|woff2?)$"
        )
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request is HttpServletRequest) {
            val path = request.servletPath
            when {
                isResourcePath(path) || isResource(path) -> chain.doFilter(request, response)
                path == "/sign-out" -> logout(request, response as HttpServletResponse)
                else -> request.getRequestDispatcher("/index.html").forward(request, response)
            }
        }
    }

    private fun logout(request: HttpServletRequest, response: HttpServletResponse) {
        request.logout()
        response.sendRedirect("/")
    }

    private fun isResourcePath(path: String) = RESOURCE_PATHS.any { path.startsWith(it) }

    private fun isResource(path: String) = REGEX_RESOURCES.containsMatchIn(path)
}
