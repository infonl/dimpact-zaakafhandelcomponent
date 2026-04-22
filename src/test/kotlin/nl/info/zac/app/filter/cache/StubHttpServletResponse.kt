/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.filter.cache

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletResponse
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale

/**
 * Minimal HttpServletResponse stub that records headers and exposes an output stream,
 * allowing us to verify the headers set by [StaticCacheFilter]'s response wrapper.
 */
class StubHttpServletResponse : HttpServletResponse {
    val headers = mutableMapOf<String, String>()
    private var committed = false

    override fun setHeader(name: String, value: String) { headers[name] = value }
    override fun addHeader(name: String, value: String) { headers[name] = value }
    override fun containsHeader(name: String) = headers.containsKey(name)
    override fun getHeader(name: String) = headers[name]
    override fun getHeaders(name: String) = headers[name]?.let { listOf(it) } ?: emptyList()
    override fun getHeaderNames() = headers.keys
    override fun isCommitted() = committed
    override fun getOutputStream() = object : ServletOutputStream() {
        override fun isReady() = true
        override fun setWriteListener(listener: WriteListener?) = Unit
        override fun write(b: Int) { committed = true }
    }
    override fun getWriter() = PrintWriter(StringWriter()).also { committed = true }
    override fun setStatus(sc: Int) = Unit
    override fun setContentType(type: String?) = Unit
    override fun setContentLength(len: Int) = Unit
    override fun setContentLengthLong(len: Long) = Unit
    override fun setCharacterEncoding(charset: String?) = Unit
    override fun setBufferSize(size: Int) = Unit
    override fun setIntHeader(name: String, value: Int) { headers[name] = value.toString() }
    override fun addIntHeader(name: String, value: Int) { headers[name] = value.toString() }
    override fun setDateHeader(name: String, date: Long) { headers[name] = date.toString() }
    override fun addDateHeader(name: String, date: Long) { headers[name] = date.toString() }
    override fun getBufferSize() = 0
    override fun flushBuffer() { committed = true }
    override fun resetBuffer() = Unit
    override fun reset() = Unit
    override fun getCharacterEncoding() = "UTF-8"
    override fun getContentType() = "text/plain"
    override fun getLocale(): Locale = Locale.getDefault()
    override fun setLocale(loc: Locale?) = Unit
    override fun sendError(sc: Int) = Unit
    override fun sendError(sc: Int, msg: String?) = Unit
    override fun sendRedirect(location: String?) = Unit
    override fun encodeURL(url: String?) = url
    override fun encodeRedirectURL(url: String?) = url
    override fun getStatus() = 200
    override fun addCookie(cookie: jakarta.servlet.http.Cookie?) = Unit
}
