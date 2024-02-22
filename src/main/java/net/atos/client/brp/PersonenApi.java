/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.brp;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.brp.exception.RuntimeExceptionMapper;
import net.atos.client.brp.model.PersonenQuery;
import net.atos.client.brp.model.PersonenQueryResponse;
import net.atos.client.brp.util.BRPClientHeadersFactory;
import net.atos.client.brp.util.JsonbConfiguration;

/**
 * BRP Personen Bevragen
 * <p>
 * API voor het bevragen van personen uit de basisregistratie personen (BRP), inclusief de registratie niet-ingezeten (RNI).
 * Met deze API kun je personen zoeken en actuele gegevens over personen, kinderen, partners en ouders raadplegen.
 * Gegevens die er niet zijn of niet actueel zijn krijg je niet terug.
 * Had een persoon bijvoorbeeld een verblijfstitel die nu niet meer geldig is, dan wordt die verblijfstitel niet opgenomen.
 * In partners wordt alleen de actuele of de laatst ontbonden partner geleverd.
 * Zie de [Functionele documentatie](https://brp-api.github.io/Haal-Centraal-BRP-bevragen) voor nadere toelichting.
 */
@RegisterRestClient(configKey = "BRP-API-Client")
@RegisterClientHeaders(BRPClientHeadersFactory.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@RegisterProvider(JsonbConfiguration.class)
@Path("/personen")
@Consumes({"application/json"})
@Produces({"application/json"})
@Timeout(unit = ChronoUnit.SECONDS, value = 10)
public interface PersonenApi {

    /**
     * Zoek personen
     * <p>
     * Zoek personen met één van de onderstaande verplichte combinaties van parameters en vul ze evt. aan met optionele parameters.
     * 1.  Raadpleeg met burgerservicenummer
     * 2.  Zoek met geslachtsnaam en geboortedatum
     * 3.  Zoek met geslachtsnaam, voornamen en gemeente van inschrijving
     * 4.  Zoek met postcode en huisnummer
     * 5.  Zoek met straat, huisnummer en gemeente van inschrijving
     * 6.  Zoek met nummeraanduiding identificatie
     * <p>
     * Default krijg je personen terug die nog in leven zijn, tenzij je de inclusiefoverledenpersonen=true opgeeft.
     * Gebruik de fields parameter om alleen die gegevens op te vragen die je nodig hebt en waarvoor je geautoriseerd bent.
     */
    @POST
    PersonenQueryResponse personen(final PersonenQuery personenQuery);

    @POST
    CompletionStage<PersonenQueryResponse> personenAsync(final PersonenQuery personenQuery);
}
