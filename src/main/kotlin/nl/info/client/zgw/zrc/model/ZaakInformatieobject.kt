/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbDateFormat
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.json.bind.annotation.JsonbTransient
import nl.info.client.zgw.util.DATE_TIME_FORMAT_WITH_MILLISECONDS
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.AardRelatieWeergaveEnum
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Constructor for PATCH request (no-arg) / POST and PUT requests ([informatieobject], [zaak]) /
 * GET response ([url], [uuid], [aardRelatieWeergave], [registratiedatum]).
 */
data class ZaakInformatieobject(
    /**
     * URL-referentie naar dit object.
     * Dit is de unieke identificatie en locatie van dit object.
     */
    val url: URI? = null,
    /**
     * Unieke resource identifier (UUID4)
     */
    val uuid: UUID? = null,
    /**
     * URL-referentie naar het INFORMATIEOBJECT (in de Documenten API), waar ook de relatieinformatie opgevraagd kan worden.
     */
    var informatieobject: URI? = null,
    /**
     * URL-referentie naar de ZAAK.
     */
    var zaak: URI? = null,
    /**
     * Aard relatie weergave
     */
    val aardRelatieWeergave: AardRelatieWeergaveEnum? = null,
    /**
     * De naam waaronder het INFORMATIEOBJECT binnen het OBJECT bekend is.
     * maxLength: [TITEL_MAX_LENGTH]
     */
    var titel: String? = null,
    /**
     * Een op het object gerichte beschrijving van de inhoud vanhet INFORMATIEOBJECT.
     */
    var beschrijving: String? = null,
    /**
     * De datum waarop de behandelende organisatie het INFORMATIEOBJECT heeft geregistreerd bij het OBJECT.
     * Geldige waardes zijn datumtijden gelegen op of voor de huidige datum en tijd.
     */
    @JsonbDateFormat(DATE_TIME_FORMAT_WITH_MILLISECONDS)
    val registratiedatum: ZonedDateTime? = null
) {
    /**
     * Constructor with required attributes for POST and PUT requests
     */
    constructor(informatieobjectUri: URI, zaakUri: URI) : this(informatieobject = informatieobjectUri, zaak = zaakUri)

    /**
     * Constructor with readOnly attributes for GET response
     */
    @JsonbCreator
    constructor(
        @JsonbProperty("url") urlValue: URI?,
        @JsonbProperty("uuid") uuidValue: UUID?,
        @JsonbProperty("aardRelatieWeergave") aardRelatieWeergaveValue: AardRelatieWeergaveEnum?,
        @JsonbProperty("registratiedatum") registratiedatumValue: ZonedDateTime?
    ) : this(url = urlValue, uuid = uuidValue, aardRelatieWeergave = aardRelatieWeergaveValue, registratiedatum = registratiedatumValue)

    @get:JsonbTransient
    val zaakUUID: UUID?
        get() = zaak?.extractUuid()

    companion object {
        const val TITEL_MAX_LENGTH = 200
    }
}
