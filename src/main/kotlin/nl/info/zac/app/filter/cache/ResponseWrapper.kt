/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.filter.cache

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import java.io.PrintWriter

/**
 * Blocks Undertow from setting cache-related headers; injects the correct value on [getOutputStream] and [getWriter].
 */
class ResponseWrapper(
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
