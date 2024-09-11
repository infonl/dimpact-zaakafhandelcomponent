/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.smartdocuments

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import net.atos.client.smartdocuments.exception.SmartDocumentsBadRequestResponseExceptionMapper
import net.atos.client.smartdocuments.exception.SmartDocumentsResponseExceptionMapper
import net.atos.client.smartdocuments.model.document.AttendedResponse
import net.atos.client.smartdocuments.model.document.Deposit
import net.atos.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import net.atos.client.smartdocuments.rest.DownloadedFile
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "SD-Client")
@RegisterProvider(SmartDocumentsBadRequestResponseExceptionMapper::class)
@RegisterProvider(SmartDocumentsResponseExceptionMapper::class)
@Produces(MediaType.APPLICATION_JSON)
interface SmartDocumentsClient {
    @POST
    @Path("wsxmldeposit/deposit/wizard")
    fun attendedDeposit(
        @HeaderParam("Authorization") authenticationToken: String,
        @HeaderParam("Username") userName: String,
        deposit: Deposit
    ): AttendedResponse

    @GET
    @Path("sdapi/structure")
    fun listTemplates(
        @HeaderParam("Authorization") authenticationToken: String,
        @HeaderParam("Username") userName: String
    ): SmartDocumentsTemplatesResponse

    @GET
    @Path("smartdocuments/result/show")
    @Consumes(MediaType.TEXT_HTML)
    fun downloadFile(
        @QueryParam("id") smartDocumentsId: String,
        @QueryParam("format") documentFormat: String,
    ): DownloadedFile
}
