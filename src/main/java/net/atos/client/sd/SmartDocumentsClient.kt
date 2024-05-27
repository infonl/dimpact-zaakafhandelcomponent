/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd

import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.sd.exception.BadRequestExceptionMapper
import net.atos.client.sd.exception.RuntimeExceptionMapper
import net.atos.client.sd.model.Deposit
import net.atos.client.sd.model.UnattendedResponse
import net.atos.client.sd.model.WizardResponse
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "SD-Client")
@RegisterProvider(BadRequestExceptionMapper::class)
@RegisterProvider(RuntimeExceptionMapper::class)
@Path("wsxmldeposit")
@Produces(MediaType.APPLICATION_JSON)
interface SmartDocumentsClient {
    @POST
    @Path("deposit/unattended")
    fun unattendedDeposit(
        @HeaderParam("Authorization") authenticationToken: String?,
        @HeaderParam("Username") username: String?,
        deposit: Deposit?
    ): UnattendedResponse?

    @POST
    @Path("deposit/wizard")
    fun wizardDeposit(
        @HeaderParam("Authorization") authenticationToken: String?,
        @HeaderParam("Username") username: String?,
        deposit: Deposit?
    ): WizardResponse?
}
