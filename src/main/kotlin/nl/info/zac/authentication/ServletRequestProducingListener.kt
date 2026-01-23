/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletRequestEvent
import jakarta.servlet.ServletRequestListener
import jakarta.servlet.annotation.WebListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession

@WebListener
class ServletRequestProducingListener : ServletRequestListener {
    companion object {
        private val SERVLET_REQUESTS = ThreadLocal<ServletRequest>()
    }

    override fun requestInitialized(servletRequestEvent: ServletRequestEvent) =
        SERVLET_REQUESTS.set(servletRequestEvent.servletRequest)

    override fun requestDestroyed(servletRequestEvent: ServletRequestEvent) =
        SERVLET_REQUESTS.remove()

    @ActiveSession
    @Produces
    @Named("activeSession")
    fun getActiveSession(): HttpSession? =
        SERVLET_REQUESTS.get()?.let { (it as HttpServletRequest).getSession(true) }
}
