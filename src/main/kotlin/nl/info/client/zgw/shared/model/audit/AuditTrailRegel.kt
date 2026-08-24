/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model.audit

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.zgw.shared.model.Bron
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Audit trail data related to a change on an object.
 */
@Suppress("LongParameterList")
data class AuditTrailRegel @JsonbCreator constructor(
    /**
     * Unieke resource identifier (UUID4)
     */
    @param:JsonbProperty("uuid") val uuid: UUID? = null,

    /**
     * De naam van het component waar de wijziging in is gedaan.
     */
    @param:JsonbProperty("bron") val bron: Bron,

    /**
     * Unieke identificatie van de applicatie, binnen de organisatie.
     * maxLength: 100
     */
    @param:JsonbProperty("applicatieId") val applicatieId: String? = null,

    /**
     * Vriendelijke naam van de applicatie.
     * maxLength: 200
     */
    @param:JsonbProperty("applicatieWeergave") val applicatieWeergave: String? = null,

    /**
     * Unieke identificatie van de gebruiker die binnen de organisatie herleid kan worden naar een persoon.
     * maxLenght: 255
     */
    @param:JsonbProperty("gebruikersId") val gebruikersId: String? = null,

    /**
     * Vriendelijke naam van de gebruiker.
     * maxLenght: 255
     */
    @param:JsonbProperty("gebruikersWeergave") val gebruikersWeergave: String? = null,

    /**
     * De uitgevoerde handeling.
     * maxLength: 50
     *
     * De bekende waardes voor dit veld zijn hieronder aangegeven, maar andere waardes zijn ook toegestaan
     *
     * Uitleg bij mogelijke waarden:
     * create - Object aangemaakt
     * list - Lijst van objecten opgehaald
     * retrieve - Object opgehaald
     * destroy - Object verwijderd
     * update - Object bijgewerkt
     * partial_update - Object deels bijgewerkt
     */
    @param:JsonbProperty("actie") val actie: String,

    /**
     * Vriendelijke naam van de actie.
     * maxLength: 200
     */
    @param:JsonbProperty("actieWeergave") val actieWeergave: String? = null,

    /**
     * HTTP status code van de API response van de uitgevoerde handeling.
     * min: 100
     * max: 599
     */
    @param:JsonbProperty("resultaat") val resultaat: Int,

    /**
     * De URL naar het hoofdobject van een component.
     */
    @param:JsonbProperty("hoofdObject") val hoofdObject: URI,

    /**
     * Het type resource waarop de actie gebeurde.
     * maxLength: 50
     */
    @param:JsonbProperty("resource") val resource: String,

    /**
     * De URL naar het object.
     */
    @param:JsonbProperty("resourceUrl") val resourceUrl: URI,

    /**
     * Toelichting waarom de handeling is uitgevoerd.
     */
    @param:JsonbProperty("toelichting") val toelichting: String? = null,

    /**
     * Vriendelijke identificatie van het object.
     * maxLength: 200
     */
    @param:JsonbProperty("resourceWeergave") val resourceWeergave: String,

    /**
     * De datum waarop de handeling is gedaan.
     */
    @param:JsonbProperty("aanmaakdatum") val aanmaakdatum: ZonedDateTime,

    /**
     * object (Wijzigingen) oud en nieuw
     */
    @param:JsonbProperty("wijzigingen") val wijzigingen: AuditWijziging<*>? = null
)
