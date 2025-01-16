/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.policy.model.RestTaakRechten
import net.atos.zac.app.task.model.TaakStatus
import net.atos.zac.policy.output.TaakRechten
import net.atos.zac.util.time.DateTimeConverterUtil.convertToLocalDate
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
data class RestTaakZoekObject(
    override var id: String? = null,
    override var type: ZoekObjectType? = null,
    override var identificatie: String? = null,
    var naam: String? = null,
    var toelichting: String? = null,
    var status: TaakStatus? = null,
    var zaakUuid: String? = null,
    var zaakIdentificatie: String? = null,
    var zaakOmschrijving: String? = null,
    var zaakToelichting: String? = null,
    var zaaktypeUuid: String? = null,
    var zaaktypeIdentificatie: String? = null,
    var zaaktypeOmschrijving: String? = null,
    var creatiedatum: LocalDate? = null,
    var toekenningsdatum: LocalDate? = null,
    var fataledatum: LocalDate? = null,
    var groepID: String? = null,
    var groepNaam: String? = null,
    var behandelaarNaam: String? = null,
    var behandelaarGebruikersnaam: String? = null,
    var taakData: List<String>? = null,
    var taakInformatie: List<String>? = null,
    var rechten: RestTaakRechten? = null
) : AbstractRestZoekObject(id, type, identificatie)

fun TaakZoekObject.toRestTaakZoekObject(taakRechten: TaakRechten) = RestTaakZoekObject(
    id = this@toRestTaakZoekObject.getObjectId(),
    type = this@toRestTaakZoekObject.getType(),
    naam = this@toRestTaakZoekObject.naam,
    status = this@toRestTaakZoekObject.getStatus(),
    toelichting = this@toRestTaakZoekObject.toelichting,
    creatiedatum = convertToLocalDate(this@toRestTaakZoekObject.creatiedatum),
    toekenningsdatum = convertToLocalDate(this@toRestTaakZoekObject.toekenningsdatum),
    fataledatum = convertToLocalDate(this@toRestTaakZoekObject.fataledatum),
    groepNaam = this@toRestTaakZoekObject.groepNaam,
    behandelaarNaam = this@toRestTaakZoekObject.behandelaarNaam,
    behandelaarGebruikersnaam = this@toRestTaakZoekObject.behandelaarGebruikersnaam,
    zaaktypeOmschrijving = this@toRestTaakZoekObject.zaaktypeOmschrijving,
    zaakIdentificatie = this@toRestTaakZoekObject.zaakIdentificatie,
    zaakUuid = this@toRestTaakZoekObject.zaakUUID,
    zaakToelichting = this@toRestTaakZoekObject.zaakToelichting,
    zaakOmschrijving = this@toRestTaakZoekObject.zaakOmschrijving,
    rechten = RestRechtenConverter.convert(taakRechten)
)
