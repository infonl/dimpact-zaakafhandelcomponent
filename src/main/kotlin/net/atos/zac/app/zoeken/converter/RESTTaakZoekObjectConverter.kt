/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.converter

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.zoeken.model.RESTTaakZoekObject
import net.atos.zac.policy.output.TaakRechten
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject

fun convertTaakZoekObject(taakZoekObject: TaakZoekObject, taakRechten: TaakRechten): RESTTaakZoekObject {
    val restTaakZoekObject = RESTTaakZoekObject()
    restTaakZoekObject.id = taakZoekObject.getObjectId()
    restTaakZoekObject.type = taakZoekObject.getType()
    restTaakZoekObject.naam = taakZoekObject.naam
    restTaakZoekObject.status = taakZoekObject.getStatus()
    restTaakZoekObject.toelichting = taakZoekObject.toelichting
    restTaakZoekObject.creatiedatum = DateTimeConverterUtil.convertToLocalDate(taakZoekObject.creatiedatum)
    restTaakZoekObject.toekenningsdatum = DateTimeConverterUtil.convertToLocalDate(taakZoekObject.toekenningsdatum)
    restTaakZoekObject.fataledatum = DateTimeConverterUtil.convertToLocalDate(taakZoekObject.fataledatum)
    restTaakZoekObject.groepNaam = taakZoekObject.groepNaam
    restTaakZoekObject.behandelaarNaam = taakZoekObject.behandelaarNaam
    restTaakZoekObject.behandelaarGebruikersnaam = taakZoekObject.behandelaarGebruikersnaam
    restTaakZoekObject.zaaktypeOmschrijving = taakZoekObject.zaaktypeOmschrijving
    restTaakZoekObject.zaakIdentificatie = taakZoekObject.zaakIdentificatie
    restTaakZoekObject.zaakUuid = taakZoekObject.zaakUUID
    restTaakZoekObject.zaakToelichting = taakZoekObject.zaakToelichting
    restTaakZoekObject.zaakOmschrijving = taakZoekObject.zaakOmschrijving
    restTaakZoekObject.rechten = RestRechtenConverter.convert(taakRechten)
    return restTaakZoekObject
}
