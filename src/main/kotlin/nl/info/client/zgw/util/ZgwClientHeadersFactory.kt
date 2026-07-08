/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import io.opentelemetry.api.baggage.Baggage
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedMap
import nl.info.zac.app.util.filter.OtelLoggingFilter.Companion.REQUEST_CORRELATION_ID
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory
import java.util.concurrent.ConcurrentHashMap

@NoArgConstructor
class ZgwClientHeadersFactory @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,

    @ConfigProperty(name = "ZGW_API_CLIENTID")
    private val clientId: String,

    @ConfigProperty(name = "ZGW_API_SECRET")
    private val secret: String
) : ClientHeadersFactory {
    companion object {
        private const val X_AUDIT_TOELICHTING_HEADER = "X-Audit-Toelichting"
        private const val X_NLX_REQUEST_ID = "X-NLX-Request-Id"
        private val auditExplanations = ConcurrentHashMap<String, String>()
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        outgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        val loggedInUser = loggedInUserInstance.get()
        try {
            addAuthorizationHeader(outgoingHeaders, loggedInUser)
            addXAuditToelichtingHeader(outgoingHeaders, loggedInUser)
            addXNlxRequestIdHeader(outgoingHeaders)
            return outgoingHeaders
        } finally {
            clearAuditExplanation(loggedInUser)
        }
    }

    fun setAuditExplanation(auditExplanation: String) =
        loggedInUserInstance.get().let {
            auditExplanations[it.id] = auditExplanation
        }

    private fun clearAuditExplanation(loggedInUser: LoggedInUser) =
        auditExplanations.remove(loggedInUser.id)

    private fun addAuthorizationHeader(
        outgoingHeaders: MultivaluedMap<String, String>,
        loggedInUser: LoggedInUser
    ) = outgoingHeaders.add(HttpHeaders.AUTHORIZATION, generateZgwJwtToken(clientId, secret, loggedInUser))

    private fun addXAuditToelichtingHeader(
        outgoingHeaders: MultivaluedMap<String, String>,
        loggedInUser: LoggedInUser
    ) = auditExplanations[loggedInUser.id]?.run {
        outgoingHeaders.add(X_AUDIT_TOELICHTING_HEADER, this)
    }

    private fun addXNlxRequestIdHeader(
        outgoingHeaders: MultivaluedMap<String, String>
    ) = Baggage.current().getEntryValue(REQUEST_CORRELATION_ID)?.run {
        outgoingHeaders.add(X_NLX_REQUEST_ID, this)
    }
}
