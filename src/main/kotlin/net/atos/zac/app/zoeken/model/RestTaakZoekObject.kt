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
import java.time.LocalDate

data class RestTaakZoekObject(
    override val id: String? = null,
    override val type: ZoekObjectType? = null,
    override val identificatie: String? = null,
    val naam: String? = null,
    val toelichting: String? = null,
    val status: TaakStatus? = null,
    val zaakUuid: String? = null,
    val zaakIdentificatie: String? = null,
    val zaakOmschrijving: String? = null,
    val zaakToelichting: String? = null,
    val zaaktypeUuid: String? = null,
    val zaaktypeIdentificatie: String? = null,
    val zaaktypeOmschrijving: String? = null,
    val creatiedatum: LocalDate? = null,
    val toekenningsdatum: LocalDate? = null,
    val fataledatum: LocalDate? = null,
    val groepID: String? = null,
    val groepNaam: String? = null,
    val behandelaarNaam: String? = null,
    val behandelaarGebruikersnaam: String? = null,
    val taakData: List<String>? = null,
    val taakInformatie: List<String>? = null,
    val rechten: RestTaakRechten? = null
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
