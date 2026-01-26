/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.officeconverter

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput
import java.io.ByteArrayInputStream

@ApplicationScoped
class OfficeConverterClientService @Inject constructor(
    @RestClient private val officeConverterClient: OfficeConverterClient
){
    fun convertToPDF(document: ByteArrayInputStream, filename: String): ByteArrayInputStream {
        val multipartFormDataOutput = MultipartFormDataOutput().apply {
            addFormData("file", document, APPLICATION_OCTET_STREAM_TYPE, filename)
        }
        val response = officeConverterClient.convert(multipartFormDataOutput)
        if (!response.bufferEntity()) {
            throw RuntimeException("Content of PDF converter could not be buffered.")
        }
        return response.entity as ByteArrayInputStream
    }
}