/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.brp.model.generated.PersonenQuery
import net.atos.client.brp.model.generated.PersonenQueryResponse
import nl.info.client.brp.exception.BrpResponseExceptionMapper
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
     * Zoek personen
     *
     *
     * Zoek personen met één van de onderstaande verplichte combinaties van parameters en vul ze evt. aan met optionele parameters.
     * 1. Raadpleeg met burgerservicenummer
     * 2. Zoek met geslachtsnaam en geboortedatum
     * 3. Zoek met geslachtsnaam, voornamen en gemeente van inschrijving
     * 4. Zoek met postcode en huisnummer
     * 5. Zoek met straat, huisnummer en gemeente van inschrijving
     * 6. Zoek met nummeraanduiding identificatie
     *
     *
     * Default krijg je personen terug die nog in leven zijn, tenzij je de inclusiefoverledenpersonen=true opgeeft.
     * Gebruik de fields parameter om alleen die gegevens op te vragen die je nodig hebt en waarvoor je geautoriseerd bent.
     */
    @POST
    fun personen(personenQuery: PersonenQuery): PersonenQueryResponse
}
