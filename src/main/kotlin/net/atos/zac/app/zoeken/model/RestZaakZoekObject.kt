/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.policy.model.RestZaakRechten
import net.atos.zac.policy.output.ZaakRechten
import net.atos.zac.util.time.DateTimeConverterUtil.convertToLocalDate
import net.atos.zac.zoeken.model.ZaakIndicatie
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.EnumSet

@NoArgConstructor
data class RestZaakZoekObject(
    override var id: String? = null,
    override var type: ZoekObjectType? = null,
    override var identificatie: String? = null,
    var omschrijving: String? = null,
    var toelichting: String? = null,
    var registratiedatum: LocalDate? = null,
    var startdatum: LocalDate? = null,
    var einddatumGepland: LocalDate? = null,
    var einddatum: LocalDate? = null,
    var archiefActiedatum: LocalDate? = null,
    var uiterlijkeEinddatumAfdoening: LocalDate? = null,
    var publicatiedatum: LocalDate? = null,
    var communicatiekanaal: String? = null,
    var vertrouwelijkheidaanduiding: String? = null,
    var archiefNominatie: String? = null,
    var afgehandeld: Boolean = false,
    var groepId: String? = null,
    var groepNaam: String? = null,
    var behandelaarNaam: String? = null,
    var behandelaarGebruikersnaam: String? = null,
    var initiatorIdentificatie: String? = null,
    var locatie: String? = null,
    var indicatieVerlenging: Boolean = false,
    var indicatieOpschorting: Boolean = false,
    var indicatieHeropend: Boolean = false,
    var indicatieDeelzaak: Boolean = false,
    var indicatieHoofdzaak: Boolean = false,
    var duurVerlenging: String? = null,
    var redenVerlenging: String? = null,
    var redenOpschorting: String? = null,
    var zaaktypeUuid: String? = null,
    var zaaktypeOmschrijving: String? = null,
    var resultaattypeOmschrijving: String? = null,
    var resultaatToelichting: String? = null,
    var statustypeOmschrijving: String? = null,
    var statusToelichting: String? = null,
    var aantalOpenstaandeTaken: Long = 0,
    var indicaties: EnumSet<ZaakIndicatie>? = null,
    var rechten: RestZaakRechten? = null,
    var betrokkenen: Map<String, MutableList<String>>? = null
) : AbstractRestZoekObject(id, type, identificatie)

fun ZaakZoekObject.toRestZaakZoekObject(zaakRechten: ZaakRechten) = RestZaakZoekObject(
    id = this@toRestZaakZoekObject.getObjectId(),
    type = this@toRestZaakZoekObject.getType(),
    identificatie = this@toRestZaakZoekObject.identificatie,
    omschrijving = this@toRestZaakZoekObject.omschrijving,
    toelichting = this@toRestZaakZoekObject.toelichting,
    archiefNominatie = this@toRestZaakZoekObject.archiefNominatie,
    archiefActiedatum = convertToLocalDate(this@toRestZaakZoekObject.archiefActiedatum),
    registratiedatum = convertToLocalDate(this@toRestZaakZoekObject.registratiedatum),
    startdatum = convertToLocalDate(this@toRestZaakZoekObject.startdatum),
    einddatum = convertToLocalDate(this@toRestZaakZoekObject.einddatum),
    einddatumGepland = convertToLocalDate(this@toRestZaakZoekObject.einddatumGepland),
    uiterlijkeEinddatumAfdoening = convertToLocalDate(this@toRestZaakZoekObject.uiterlijkeEinddatumAfdoening),
    publicatiedatum = convertToLocalDate(this@toRestZaakZoekObject.publicatiedatum),
    communicatiekanaal = this@toRestZaakZoekObject.communicatiekanaal,
    vertrouwelijkheidaanduiding = this@toRestZaakZoekObject.vertrouwelijkheidaanduiding,
    afgehandeld = this@toRestZaakZoekObject.isAfgehandeld,
    groepId = this@toRestZaakZoekObject.groepID,
    groepNaam = this@toRestZaakZoekObject.groepNaam,
    behandelaarNaam = this@toRestZaakZoekObject.behandelaarNaam,
    behandelaarGebruikersnaam = this@toRestZaakZoekObject.behandelaarGebruikersnaam,
    initiatorIdentificatie = this@toRestZaakZoekObject.initiatorIdentificatie,
    zaaktypeOmschrijving = this@toRestZaakZoekObject.zaaktypeOmschrijving,
    statustypeOmschrijving = this@toRestZaakZoekObject.statustypeOmschrijving,
    resultaattypeOmschrijving = this@toRestZaakZoekObject.resultaattypeOmschrijving,
    aantalOpenstaandeTaken = this@toRestZaakZoekObject.aantalOpenstaandeTaken,
    indicatieVerlenging = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.VERLENGD),
    redenVerlenging = this@toRestZaakZoekObject.redenVerlenging,
    indicatieOpschorting = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.OPSCHORTING),
    redenOpschorting = this@toRestZaakZoekObject.redenOpschorting,
    indicatieDeelzaak = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.DEELZAAK),
    indicatieHoofdzaak = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.HOOFDZAAK),
    indicatieHeropend = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.HEROPEND),
    statusToelichting = this@toRestZaakZoekObject.statusToelichting,
    indicaties = this@toRestZaakZoekObject.getZaakIndicaties(),
    rechten = RestRechtenConverter.convert(zaakRechten),
    betrokkenen = this@toRestZaakZoekObject.betrokkenen?.mapKeys {
        it.key.replace(ZaakZoekObject.ZAAK_BETROKKENE_PREFIX, "")
    }?.toMutableMap() ?: mutableMapOf()
)
