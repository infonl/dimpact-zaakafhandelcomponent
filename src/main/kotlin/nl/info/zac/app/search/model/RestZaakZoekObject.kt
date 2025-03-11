/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.model

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.policy.model.RestZaakRechten
import net.atos.zac.policy.output.ZaakRechten
import net.atos.zac.search.model.ZaakIndicatie
import net.atos.zac.search.model.zoekobject.ZaakZoekObject
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import net.atos.zac.util.time.DateTimeConverterUtil.convertToLocalDate
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
    val afgehandeld: Boolean = false,
    val groepId: String? = null,
    val groepNaam: String? = null,
    val behandelaarNaam: String? = null,
    val behandelaarGebruikersnaam: String? = null,
    val initiatorIdentificatie: String? = null,
    val locatie: String? = null,
    val indicatieVerlenging: Boolean = false,
    val indicatieOpschorting: Boolean = false,
    val indicatieHeropend: Boolean = false,
    val indicatieDeelzaak: Boolean = false,
    val indicatieHoofdzaak: Boolean = false,
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
