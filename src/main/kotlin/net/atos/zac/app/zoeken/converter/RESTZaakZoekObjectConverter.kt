/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.converter

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.zoeken.model.RESTZaakZoekObject
import net.atos.zac.policy.output.ZaakRechten
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.zoeken.model.ZaakIndicatie
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject

fun convertZaakZoekObject(zoekItem: ZaakZoekObject, zaakRechten: ZaakRechten): RESTZaakZoekObject {
    val restZoekItem = RESTZaakZoekObject()
    restZoekItem.id = zoekItem.getObjectId()
    restZoekItem.type = zoekItem.getType()
    restZoekItem.identificatie = zoekItem.identificatie
    restZoekItem.omschrijving = zoekItem.omschrijving
    restZoekItem.toelichting = zoekItem.toelichting
    restZoekItem.archiefNominatie = zoekItem.archiefNominatie
    restZoekItem.archiefActiedatum = DateTimeConverterUtil.convertToLocalDate(zoekItem.archiefActiedatum)
    restZoekItem.registratiedatum = DateTimeConverterUtil.convertToLocalDate(zoekItem.registratiedatum)
    restZoekItem.startdatum = DateTimeConverterUtil.convertToLocalDate(zoekItem.startdatum)
    restZoekItem.einddatum = DateTimeConverterUtil.convertToLocalDate(zoekItem.einddatum)
    restZoekItem.einddatumGepland = DateTimeConverterUtil.convertToLocalDate(zoekItem.einddatumGepland)
    restZoekItem.uiterlijkeEinddatumAfdoening =
        DateTimeConverterUtil.convertToLocalDate(zoekItem.uiterlijkeEinddatumAfdoening)
    restZoekItem.publicatiedatum = DateTimeConverterUtil.convertToLocalDate(zoekItem.publicatiedatum)
    restZoekItem.communicatiekanaal = zoekItem.communicatiekanaal
    restZoekItem.vertrouwelijkheidaanduiding = zoekItem.vertrouwelijkheidaanduiding
    restZoekItem.afgehandeld = zoekItem.isAfgehandeld
    restZoekItem.groepId = zoekItem.groepID
    restZoekItem.groepNaam = zoekItem.groepNaam
    restZoekItem.behandelaarNaam = zoekItem.behandelaarNaam
    restZoekItem.behandelaarGebruikersnaam = zoekItem.behandelaarGebruikersnaam
    restZoekItem.initiatorIdentificatie = zoekItem.initiatorIdentificatie
    restZoekItem.zaaktypeOmschrijving = zoekItem.zaaktypeOmschrijving
    restZoekItem.statustypeOmschrijving = zoekItem.statustypeOmschrijving
    restZoekItem.resultaattypeOmschrijving = zoekItem.resultaattypeOmschrijving
    restZoekItem.aantalOpenstaandeTaken = zoekItem.aantalOpenstaandeTaken
    restZoekItem.indicatieVerlenging = zoekItem.isIndicatie(ZaakIndicatie.VERLENGD)
    restZoekItem.redenVerlenging = zoekItem.redenVerlenging
    restZoekItem.indicatieOpschorting = zoekItem.isIndicatie(ZaakIndicatie.OPSCHORTING)
    restZoekItem.redenOpschorting = zoekItem.redenOpschorting
    restZoekItem.indicatieDeelzaak = zoekItem.isIndicatie(ZaakIndicatie.DEELZAAK)
    restZoekItem.indicatieHoofdzaak = zoekItem.isIndicatie(ZaakIndicatie.HOOFDZAAK)
    restZoekItem.indicatieHeropend = zoekItem.isIndicatie(ZaakIndicatie.HEROPEND)
    restZoekItem.statusToelichting = zoekItem.statusToelichting
    restZoekItem.indicaties = zoekItem.getZaakIndicaties()
    restZoekItem.rechten = RestRechtenConverter.convert(zaakRechten)
    restZoekItem.betrokkenen = HashMap<String?, MutableList<String?>?>()
    zoekItem.betrokkenen?.forEach { (betrokkenheid: String, ids: MutableList<String>) ->
        restZoekItem.betrokkenen.put(
            betrokkenheid.replace(ZaakZoekObject.ZAAK_BETROKKENE_PREFIX, ""),
            ids
        )
    }
    return restZoekItem
}
