/*
 *
 *  * SPDX-FileCopyrightText: 2025 INFO.nl
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.time.convertToLocalDate
import nl.info.zac.app.policy.model.RestZaakRechten
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.policy.output.ZaakRechten
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.time.LocalDate
import java.util.EnumSet

data class RestZaakZoekObject(
    override val id: String? = null,
    override val type: ZoekObjectType? = null,
    override val identificatie: String? = null,
    val omschrijving: String? = null,
    val toelichting: String? = null,
    val registratiedatum: LocalDate? = null,
    val startdatum: LocalDate? = null,
    val einddatumGepland: LocalDate? = null,
    val einddatum: LocalDate? = null,
    val archiefActiedatum: LocalDate? = null,
    val uiterlijkeEinddatumAfdoening: LocalDate? = null,
    val publicatiedatum: LocalDate? = null,
    val communicatiekanaal: String? = null,
    val vertrouwelijkheidaanduiding: String? = null,
    val archiefNominatie: String? = null,

    @get:JsonbProperty("isAfgehandeld")
    val isAfgehandeld: Boolean = false,

    @get:JsonbProperty("isZaakspecifiekGeautoriseerd")
    val isZaakspecifiekGeautoriseerd: Boolean = false,
    val groepId: String? = null,
    val groepNaam: String? = null,
    val behandelaarNaam: String? = null,
    val behandelaarGebruikersnaam: String? = null,
    val initiatorIdentificatie: String? = null,
    val locatie: String? = null,

    @get:JsonbProperty("isVerlengd")
    val isVerlengd: Boolean = false,

    @get:JsonbProperty("isOpgeschort")
    val isOpgeschort: Boolean = false,

    @get:JsonbProperty("isHeropend")
    val isHeropend: Boolean = false,

    @get:JsonbProperty("isDeelzaak")
    val isDeelzaak: Boolean = false,

    @get:JsonbProperty("isHoofdzaak")
    val isHoofdzaak: Boolean = false,

    val duurVerlenging: String? = null,
    val redenVerlenging: String? = null,
    val redenOpschorting: String? = null,
    val zaaktypeUuid: String? = null,
    val zaaktypeOmschrijving: String? = null,
    val resultaattypeOmschrijving: String? = null,
    val resultaatToelichting: String? = null,
    val statustypeOmschrijving: String? = null,
    val statusToelichting: String? = null,
    val aantalOpenstaandeTaken: Long = 0,
    val indicaties: EnumSet<ZaakIndicatie>? = null,
    val rechten: RestZaakRechten? = null,
    val betrokkenen: Map<String, MutableList<String>>? = null
) : AbstractRestZoekObject(id, type, identificatie)

fun ZaakZoekObject.toRestZaakZoekObject(zaakRechten: ZaakRechten) = RestZaakZoekObject(
    id = this@toRestZaakZoekObject.getObjectId(),
    type = this@toRestZaakZoekObject.getType(),
    identificatie = this@toRestZaakZoekObject.identificatie,
    omschrijving = this@toRestZaakZoekObject.omschrijving,
    toelichting = this@toRestZaakZoekObject.toelichting,
    archiefNominatie = this@toRestZaakZoekObject.archiefNominatie,
    archiefActiedatum = this@toRestZaakZoekObject.archiefActiedatum?.let(::convertToLocalDate),
    registratiedatum = this@toRestZaakZoekObject.registratiedatum?.let(::convertToLocalDate),
    startdatum = this@toRestZaakZoekObject.startdatum?.let(::convertToLocalDate),
    einddatum = this@toRestZaakZoekObject.einddatum?.let(::convertToLocalDate),
    einddatumGepland = this@toRestZaakZoekObject.einddatumGepland?.let(::convertToLocalDate),
    uiterlijkeEinddatumAfdoening = this@toRestZaakZoekObject.uiterlijkeEinddatumAfdoening?.let(::convertToLocalDate),
    publicatiedatum = this@toRestZaakZoekObject.publicatiedatum?.let(::convertToLocalDate),
    communicatiekanaal = this@toRestZaakZoekObject.communicatiekanaal,
    vertrouwelijkheidaanduiding = this@toRestZaakZoekObject.vertrouwelijkheidaanduiding,
    isAfgehandeld = this@toRestZaakZoekObject.isAfgehandeld,
    isZaakspecifiekGeautoriseerd = this@toRestZaakZoekObject.isZaakspecifiekGeautoriseerd,
    groepId = this@toRestZaakZoekObject.groepID,
    groepNaam = this@toRestZaakZoekObject.groepNaam,
    behandelaarNaam = this@toRestZaakZoekObject.behandelaarNaam,
    behandelaarGebruikersnaam = this@toRestZaakZoekObject.behandelaarGebruikersnaam,
    initiatorIdentificatie = this@toRestZaakZoekObject.initiatorIdentificatie,
    zaaktypeOmschrijving = this@toRestZaakZoekObject.zaaktypeOmschrijving,
    statustypeOmschrijving = this@toRestZaakZoekObject.statustypeOmschrijving,
    resultaattypeOmschrijving = this@toRestZaakZoekObject.resultaattypeOmschrijving,
    aantalOpenstaandeTaken = this@toRestZaakZoekObject.aantalOpenstaandeTaken,
    isVerlengd = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.VERLENGD),
    redenVerlenging = this@toRestZaakZoekObject.redenVerlenging,
    isOpgeschort = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.OPSCHORTING),
    redenOpschorting = this@toRestZaakZoekObject.redenOpschorting,
    isDeelzaak = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.DEELZAAK),
    isHoofdzaak = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.HOOFDZAAK),
    isHeropend = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.HEROPEND),
    statusToelichting = this@toRestZaakZoekObject.statusToelichting,
    indicaties = this@toRestZaakZoekObject.getZaakIndicaties(),
    rechten = zaakRechten.toRestZaakRechten(),
    betrokkenen = this@toRestZaakZoekObject.betrokkenen?.mapKeys {
        it.key.replace(ZaakZoekObject.ZAAK_BETROKKENE_PREFIX, "")
    }?.toMutableMap() ?: mutableMapOf()
)
