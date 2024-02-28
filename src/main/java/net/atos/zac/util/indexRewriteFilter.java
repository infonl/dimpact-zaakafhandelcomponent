/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util;


import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * If the requested resource doesn't exist, use index.html
 * more information at https://angular.io/guide/deployment#server-configuration
 */
@WebFilter(filterName = "indexRewriteFilter")
public class indexRewriteFilter implements Filter {

    private final List<String> resourcePaths = List.of("/assets", "/rest", "/websocket", "/webdav");

    private static final Pattern REGEX_RESOURCES = Pattern.compile(
            "\\.(js(on|\\.map)?|css|txt|jpe?g|png|gif|svg|ico|webmanifest|eot|ttf|woff2?)$");

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException,
                                                                                                                ServletException {
        if (request instanceof final HttpServletRequest httpRequest) {
            final String path = httpRequest.getServletPath();
            if (isResourcePath(path) || isResource(path)) {
                chain.doFilter(request, response);
            } else if (path.equals("/logout")) {
                logout(httpRequest, (HttpServletResponse) response);
            } else if (path.startsWith("/startformulieren")) {
                httpRequest.getRequestDispatcher("/startformulieren/melding-klein-evenement.jsp").forward(request, response);
            } else {
                httpRequest.getRequestDispatcher("/index.html").forward(request, response);
            }
        }
    }

    private void logout(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        request.logout();
        response.sendRedirect("/");
    }

    private boolean isResourcePath(final String path) {
        return resourcePaths.stream().anyMatch(path::startsWith);
    }

    private boolean isResource(final String path) {
        return REGEX_RESOURCES.matcher(path).find();
    }
}
