/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.gpppublicatiebank.util

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedMap
import nl.info.zac.authentication.LoggedInUser
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class GppPublicatiebankClientHeadersFactory @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>
) : ClientHeadersFactory {
    companion object {
        const val HEADER_AUDIT_REMARKS = "Audit-Remarks"
        const val HEADER_AUDIT_USER_ID = "Audit-User-ID"
        const val HEADER_AUDIT_USER_REPRESENTATION = "Audit-User-Representation"
        // use hardcoded JWT token for now
        const val GPP_PUBLICATIEBANK_API_TOKEN = "insecure-ea1a8d297e3b2d3313b8a30b18959c3"
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        outgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        val loggedInUser = loggedInUserInstance.get()
        addAutorizationHeader(outgoingHeaders, loggedInUser)
        return outgoingHeaders
    }

    private fun addAutorizationHeader(
        outgoingHeaders: MultivaluedMap<String, String>,
        loggedInUser: LoggedInUser
    ) {
        // outgoingHeaders.add(HttpHeaders.AUTHORIZATION, JWTTokenGenerator.generate(clientId, secret, loggedInUser))
        outgoingHeaders.add(HttpHeaders.AUTHORIZATION, "Token $GPP_PUBLICATIEBANK_API_TOKEN")
        outgoingHeaders.add(HEADER_AUDIT_REMARKS, "ZAC Common Ground demo")
        outgoingHeaders.add(HEADER_AUDIT_USER_ID, loggedInUser.id)
        outgoingHeaders.add(HEADER_AUDIT_USER_REPRESENTATION, loggedInUser.displayName)
    }
}
