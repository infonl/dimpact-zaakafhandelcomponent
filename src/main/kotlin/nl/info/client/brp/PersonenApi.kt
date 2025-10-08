/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.brp.exception.BrpResponseExceptionMapper
import nl.info.client.brp.model.generated.PersonenQuery
import nl.info.client.brp.model.generated.PersonenQueryResponse
import nl.info.client.brp.util.BRPClientHeadersFactory
import nl.info.client.brp.util.JsonbConfiguration
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.time.temporal.ChronoUnit

/**
 * BRP Personen Bevragen
 *
 * API voor het bevragen van personen uit de basisregistratie personen (BRP), inclusief de registratie niet-ingezeten (RNI).
 * Met deze API kun je personen zoeken en actuele gegevens over personen, kinderen, partners en ouders raadplegen.
 * Gegevens die er niet zijn of niet actueel zijn krijg je niet terug.
 * Had een persoon bijvoorbeeld een verblijfstitel die nu niet meer geldig is, dan wordt die verblijfstitel niet opgenomen.
 * In partners wordt alleen de actuele of de laatst ontbonden partner geleverd.
 * Zie de [Functionele documentatie](https://brp-api.github.io/Haal-Centraal-BRP-bevragen) voor nadere toelichting.
 */
@RegisterRestClient(configKey = "BRP-API-Client")
@RegisterClientHeaders(BRPClientHeadersFactory::class)
@RegisterProvider(BrpResponseExceptionMapper::class)
@RegisterProvider(JsonbConfiguration::class)
@Path("/personen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Timeout(unit = ChronoUnit.SECONDS, value = 10)
interface PersonenApi {

    /**
     * Search for persons
     *
     * Search for persons using one of the mandatory parameter combinations below and supplement them with optional parameters if necessary.
     * 1. Search using citizen service number
     * 2. Search using surname and date of birth
     * 3. Search using surname, first names, and municipality of registration
     * 4. Search using postal code and house number
     * 5. Search using street, house number, and municipality of registration
     * 6. Search using number identification
     *
     * @param personenQuery the search criteria for persons
     * @param purpose the request purpose (iConnect's X-DOELBINDING header), mandatory for logging and authorization
     * @param auditEvent what the response data will be used for (iConnect's X-VERWERKING header)
     * @param recipient customer requesting the data (2Secure's X-REQUEST-AFNEMERSCODE header)
     *
     * By default, you will receive persons who are still alive, unless you specify "includedeceasedpersons=true."
     * Use the fields parameter to request only the data you need and are authorized to access.
     */
    @POST
    fun personen(
        personenQuery: PersonenQuery,
        @HeaderParam(BRPClientHeadersFactory.ICONNECT_X_DOELBINDING) purpose: String? = null,
        @HeaderParam(BRPClientHeadersFactory.ICONNECT_X_VERWERKING) auditEvent: String? = null,
        @HeaderParam(BRPClientHeadersFactory.TWOSECURE_X_REQUEST_AFNEMERSCODE) recipient: String? = null
    ): PersonenQueryResponse
}
