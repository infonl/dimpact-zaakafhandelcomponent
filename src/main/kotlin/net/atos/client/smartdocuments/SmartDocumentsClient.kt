/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.smartdocuments

import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.smartdocuments.exception.SmartDocumentsBadRequestResponseExceptionMapper
import net.atos.client.smartdocuments.exception.SmartDocumentsResponseExceptionMapper
import net.atos.client.smartdocuments.model.document.Deposit
import net.atos.client.smartdocuments.model.document.UnattendedResponse
import net.atos.client.smartdocuments.model.document.WizardRequest
import net.atos.client.smartdocuments.model.document.WizardResponse
import net.atos.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "SD-Client")
@RegisterProvider(SmartDocumentsBadRequestResponseExceptionMapper::class)
@RegisterProvider(SmartDocumentsResponseExceptionMapper::class)
@Produces(MediaType.APPLICATION_JSON)
interface SmartDocumentsClient {
    @POST
    @Path("wsxmldeposit/deposit/unattended")
    fun unattendedDeposit(
        @HeaderParam("Authorization") authenticationToken: String,
        @HeaderParam("Username") username: String,
        deposit: Deposit
    ): UnattendedResponse

    @POST
    @Path("wsxmldeposit/deposit/wizard")
    fun wizardDeposit(
        @HeaderParam("Authorization") authenticationToken: String,
        @HeaderParam("Username") userName: String,
        wizardRequest: WizardRequest
    ): WizardResponse

    @GET
    @Path("sdapi/structure")
    fun listTemplates(
        @HeaderParam("Authorization") authenticationToken: String,
        @HeaderParam("Username") username: String
    ): SmartDocumentsTemplatesResponse
}
