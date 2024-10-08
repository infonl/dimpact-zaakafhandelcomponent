/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.smartdocuments.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class SmartDocumentsBadRequestResponseExceptionMapper : ResponseExceptionMapper<SmartDocumentsBadRequestException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean =
        status == Response.Status.BAD_REQUEST.statusCode

    override fun toThrowable(response: Response): SmartDocumentsBadRequestException = SmartDocumentsBadRequestException(
        "SmartDocuments responded with a 400 Bad Request. " +
            "Please check if the current user is allowed to interact with SmartDocuments!"
    )
}
