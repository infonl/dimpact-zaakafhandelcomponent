/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.util.filter

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider
import org.jboss.resteasy.plugins.providers.multipart.InputPart

/**
 * Filter to set the default charset to UTF-8 for multipart request inputs (as used in multipart/formdata).
 * This fixes the issue where JBoss/RESTEasy does not default (yet) to UTF-8.
 * See e.g.: https://github.com/quarkusio/quarkus/issues/10323
 * Once JBoss/RESTEasy has fixed this issue and defaults correctly to UTF-8, as per specification,
 * this filter can be removed.
 */
@Provider
class CharsetInterceptorFilter : ContainerRequestFilter {
    override fun filter(context: ContainerRequestContext) {
        context.setProperty(InputPart.DEFAULT_CHARSET_PROPERTY, "UTF-8")
    }
}
