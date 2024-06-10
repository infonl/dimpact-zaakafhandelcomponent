package net.atos.zac.smartdocuments.rest

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import net.atos.zac.smartdocuments.SmartDocumentsException

@Provider
class SmartDocumentsExceptionMapper : ExceptionMapper<SmartDocumentsException> {
    override fun toResponse(e: SmartDocumentsException): Response {
        return Response.status(Response.Status.BAD_REQUEST).entity(e.message).build()
    }
}
