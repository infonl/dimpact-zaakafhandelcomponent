/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.converter

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.zoeken.model.RestTaakZoekObject
import net.atos.zac.policy.output.TaakRechten
import net.atos.zac.util.time.DateTimeConverterUtil.convertToLocalDate
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject

fun TaakZoekObject.toRestTaakZoekObject(taakRechten: TaakRechten) = RestTaakZoekObject().apply {
    id = this@toRestTaakZoekObject.getObjectId()
    type = this@toRestTaakZoekObject.getType()
    naam = this@toRestTaakZoekObject.naam
    status = this@toRestTaakZoekObject.getStatus()
    toelichting = this@toRestTaakZoekObject.toelichting
    creatiedatum = convertToLocalDate(this@toRestTaakZoekObject.creatiedatum)
    toekenningsdatum = convertToLocalDate(this@toRestTaakZoekObject.toekenningsdatum)
    fataledatum = convertToLocalDate(this@toRestTaakZoekObject.fataledatum)
    groepNaam = this@toRestTaakZoekObject.groepNaam
    behandelaarNaam = this@toRestTaakZoekObject.behandelaarNaam
    behandelaarGebruikersnaam = this@toRestTaakZoekObject.behandelaarGebruikersnaam
    zaaktypeOmschrijving = this@toRestTaakZoekObject.zaaktypeOmschrijving
    zaakIdentificatie = this@toRestTaakZoekObject.zaakIdentificatie
    zaakUuid = this@toRestTaakZoekObject.zaakUUID
    zaakToelichting = this@toRestTaakZoekObject.zaakToelichting
    zaakOmschrijving = this@toRestTaakZoekObject.zaakOmschrijving
    rechten = RestRechtenConverter.convert(taakRechten)
}
