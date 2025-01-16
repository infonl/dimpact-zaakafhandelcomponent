/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.converter

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.zoeken.model.RestZaakZoekObject
import net.atos.zac.policy.output.ZaakRechten
import net.atos.zac.util.time.DateTimeConverterUtil.convertToLocalDate
import net.atos.zac.zoeken.model.ZaakIndicatie
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject

fun ZaakZoekObject.toRestZaakZoekObject(zaakRechten: ZaakRechten) = RestZaakZoekObject().apply {
    id = this@toRestZaakZoekObject.getObjectId()
    type = this@toRestZaakZoekObject.getType()
    identificatie = this@toRestZaakZoekObject.identificatie
    omschrijving = this@toRestZaakZoekObject.omschrijving
    toelichting = this@toRestZaakZoekObject.toelichting
    archiefNominatie = this@toRestZaakZoekObject.archiefNominatie
    archiefActiedatum = convertToLocalDate(this@toRestZaakZoekObject.archiefActiedatum)
    registratiedatum = convertToLocalDate(this@toRestZaakZoekObject.registratiedatum)
    startdatum = convertToLocalDate(this@toRestZaakZoekObject.startdatum)
    einddatum = convertToLocalDate(this@toRestZaakZoekObject.einddatum)
    einddatumGepland = convertToLocalDate(this@toRestZaakZoekObject.einddatumGepland)
    uiterlijkeEinddatumAfdoening = convertToLocalDate(this@toRestZaakZoekObject.uiterlijkeEinddatumAfdoening)
    publicatiedatum = convertToLocalDate(this@toRestZaakZoekObject.publicatiedatum)
    communicatiekanaal = this@toRestZaakZoekObject.communicatiekanaal
    vertrouwelijkheidaanduiding = this@toRestZaakZoekObject.vertrouwelijkheidaanduiding
    afgehandeld = this@toRestZaakZoekObject.isAfgehandeld
    groepId = this@toRestZaakZoekObject.groepID
    groepNaam = this@toRestZaakZoekObject.groepNaam
    behandelaarNaam = this@toRestZaakZoekObject.behandelaarNaam
    behandelaarGebruikersnaam = this@toRestZaakZoekObject.behandelaarGebruikersnaam
    initiatorIdentificatie = this@toRestZaakZoekObject.initiatorIdentificatie
    zaaktypeOmschrijving = this@toRestZaakZoekObject.zaaktypeOmschrijving
    statustypeOmschrijving = this@toRestZaakZoekObject.statustypeOmschrijving
    resultaattypeOmschrijving = this@toRestZaakZoekObject.resultaattypeOmschrijving
    aantalOpenstaandeTaken = this@toRestZaakZoekObject.aantalOpenstaandeTaken
    indicatieVerlenging = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.VERLENGD)
    redenVerlenging = this@toRestZaakZoekObject.redenVerlenging
    indicatieOpschorting = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.OPSCHORTING)
    redenOpschorting = this@toRestZaakZoekObject.redenOpschorting
    indicatieDeelzaak = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.DEELZAAK)
    indicatieHoofdzaak = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.HOOFDZAAK)
    indicatieHeropend = this@toRestZaakZoekObject.isIndicatie(ZaakIndicatie.HEROPEND)
    statusToelichting = this@toRestZaakZoekObject.statusToelichting
    indicaties = this@toRestZaakZoekObject.getZaakIndicaties()
    rechten = RestRechtenConverter.convert(zaakRechten)
    betrokkenen = this@toRestZaakZoekObject.betrokkenen?.mapKeys {
        it.key.replace(ZaakZoekObject.ZAAK_BETROKKENE_PREFIX, "")
    }?.toMutableMap() ?: mutableMapOf()
}
