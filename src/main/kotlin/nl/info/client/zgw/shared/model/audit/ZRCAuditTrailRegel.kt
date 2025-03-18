/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model.audit

import net.atos.client.zgw.shared.model.Bron
import nl.info.client.zgw.zrc.model.generated.Wijzigingen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Gegevens m.b.t. een wijziging gedaan op een object
 */
@NoArgConstructor
data class ZRCAuditTrailRegel(
    /**
     * URL-referentie naar dit object. Dit is de unieke identificatie en locatie van dit object.
     */
    var url: URI? = null,

    /**
     * Unieke resource identifier (UUID4)
     */
    var uuid: UUID? = null,

    /**
     * De naam van het component waar de wijziging in is gedaan.
     */
    var bron: Bron? = null,

    /**
     * Unieke identificatie van de applicatie, binnen de organisatie.
     * maxLength: 100
     */
    var applicatieId: String? = null,

    /**
     * Vriendelijke naam van de applicatie.
     * maxLength: 200
     */
    var applicatieWeergave: String? = null,

    /**
     * Unieke identificatie van de gebruiker die binnen de organisatie herleid kan worden naar een persoon.
     * maxLenght: 255
     */
    var gebruikersId: String? = null,

    /**
     * Vriendelijke naam van de gebruiker.
     * maxLenght: 255
     */
    var gebruikersWeergave: String? = null,

    /**
     * De uitgevoerde handeling.
     * maxLength: 50
     *
     *
     * De bekende waardes voor dit veld zijn hieronder aangegeven, maar andere waardes zijn ook toegestaan
     *
     *
     * Uitleg bij mogelijke waarden:
     * create - Object aangemaakt
     * list - Lijst van objecten opgehaald
     * retrieve - Object opgehaald
     * destroy - Object verwijderd
     * update - Object bijgewerkt
     * partial_update - Object deels bijgewerkt
     */
    var actie: String? = null,

    /**
     * Vriendelijke naam van de actie.
     * maxLength: 200
     */
    var actieWeergave: String? = null,

    /**
     * HTTP status code van de API-response van de uitgevoerde handeling.
     * min: 100
     * max: 599
     */
    var resultaat: Int = 0,

    /**
     * De URL naar het hoofdobject van een component.
     */
    var hoofdObject: URI? = null,

    /**
     * Het type resource waarop de actie gebeurde.
     * maxLength: 50
     */
    var resource: String,

    /**
     * De URL naar het object.
     */
    var resourceUrl: URI? = null,

    /**
     * Toelichting waarom de handeling is uitgevoerd.
     */
    var toelichting: String? = null,

    /**
     * Vriendelijke identificatie van het object.
     * maxLength: 200
     */
    var resourceWeergave: String? = null,

    /**
     * De datum waarop de handeling is gedaan.
     */
    var aanmaakdatum: ZonedDateTime,

    /**
     * Object (Wijzigingen) oud en nieuw
     */
    var wijzigingen: Wijzigingen,
)
