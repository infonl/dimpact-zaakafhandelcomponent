package net.atos.client.smartdocuments.rest

import jakarta.ws.rs.HeaderParam
import org.jboss.resteasy.annotations.Body
import org.jboss.resteasy.annotations.ResponseObject

@ResponseObject
interface DownloadedFile {
    @Body
    fun body(): ByteArray

    @HeaderParam("content-disposition")
    fun contentDisposition(): String
}
