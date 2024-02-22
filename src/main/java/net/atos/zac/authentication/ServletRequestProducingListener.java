/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.authentication;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@WebListener
public class ServletRequestProducingListener implements ServletRequestListener {

    private static final ThreadLocal<ServletRequest> SERVLET_REQUESTS = new ThreadLocal<>();

    @Override
    public void requestInitialized(final ServletRequestEvent servletRequestEvent) {
        SERVLET_REQUESTS.set(servletRequestEvent.getServletRequest());
    }

    @Override
    public void requestDestroyed(final ServletRequestEvent servletRequestEvent) {
        SERVLET_REQUESTS.remove();
    }

    @Named("activeSession")
    @Produces
    @ActiveSession
    public HttpSession getActiveSession() {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) SERVLET_REQUESTS.get();
        return httpServletRequest != null ? httpServletRequest.getSession(true) : null;
    }
}
