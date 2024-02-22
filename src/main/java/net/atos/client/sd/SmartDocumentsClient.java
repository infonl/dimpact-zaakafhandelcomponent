/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.sd.exception.BadRequestExceptionMapper;
import net.atos.client.sd.exception.RuntimeExceptionMapper;
import net.atos.client.sd.model.Deposit;
import net.atos.client.sd.model.UnattendedResponse;
import net.atos.client.sd.model.WizardResponse;

@RegisterRestClient(configKey = "SD-Client")
@RegisterProvider(BadRequestExceptionMapper.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@Path("wsxmldeposit")
@Produces(APPLICATION_JSON)
public interface SmartDocumentsClient {

    @POST
    @Path("deposit/unattended")
    UnattendedResponse unattendedDeposit(
            @HeaderParam("Authorization") final String authenticationToken,
            @HeaderParam("Username") final String username,
            final Deposit deposit);

    @POST
    @Path("deposit/wizard")
    WizardResponse wizardDeposit(
            @HeaderParam("Authorization") final String authenticationToken,
            @HeaderParam("Username") final String username,
            final Deposit deposit);
}
