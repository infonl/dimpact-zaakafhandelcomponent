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

fun TaakZoekObject.toTaakZoekObject(taakRechten: TaakRechten) = RestTaakZoekObject().apply {
    id = this@toTaakZoekObject.getObjectId()
    type = this@toTaakZoekObject.getType()
    naam = this@toTaakZoekObject.naam
    status = this@toTaakZoekObject.getStatus()
    toelichting = this@toTaakZoekObject.toelichting
    creatiedatum = convertToLocalDate(this@toTaakZoekObject.creatiedatum)
    toekenningsdatum = convertToLocalDate(this@toTaakZoekObject.toekenningsdatum)
    fataledatum = convertToLocalDate(this@toTaakZoekObject.fataledatum)
    groepNaam = this@toTaakZoekObject.groepNaam
    behandelaarNaam = this@toTaakZoekObject.behandelaarNaam
    behandelaarGebruikersnaam = this@toTaakZoekObject.behandelaarGebruikersnaam
    zaaktypeOmschrijving = this.zaaktypeOmschrijving
    zaakIdentificatie = this@toTaakZoekObject.zaakIdentificatie
    zaakUuid = this@toTaakZoekObject.zaakUUID
    zaakToelichting = this@toTaakZoekObject.zaakToelichting
    zaakOmschrijving = this@toTaakZoekObject.zaakOmschrijving
    rechten = RestRechtenConverter.convert(taakRechten)
}
