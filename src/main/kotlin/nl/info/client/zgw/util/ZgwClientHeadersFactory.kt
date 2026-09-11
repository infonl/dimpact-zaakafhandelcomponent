/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedMap
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

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

        // held per thread: the explanation belongs to one request, not to the user making it
        private val auditExplanation: ThreadLocal<String?> = ThreadLocal.withInitial { null }
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        outgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        try {
            addAuthorizationHeader(outgoingHeaders, loggedInUserInstance.get())
            addXAuditToelichtingHeader(outgoingHeaders)
            return outgoingHeaders
        } finally {
            auditExplanation.remove()
        }
    }

    fun setAuditExplanation(explanation: String) = auditExplanation.set(explanation)

    private fun addAuthorizationHeader(
        outgoingHeaders: MultivaluedMap<String, String>,
        loggedInUser: LoggedInUser
    ) = outgoingHeaders.add(HttpHeaders.AUTHORIZATION, generateZgwJwtToken(clientId, secret, loggedInUser))

    private fun addXAuditToelichtingHeader(
        outgoingHeaders: MultivaluedMap<String, String>
    ) = auditExplanation.get()?.let {
        outgoingHeaders.add(X_AUDIT_TOELICHTING_HEADER, it)
    }
}
