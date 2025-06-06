/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import net.atos.zac.util.time.PeriodUtil
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.generated.BesluitType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.Period
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestDecisionType(
    var id: UUID,

    var naam: String,

    var toelichting: String,

    var informatieobjecttypen: List<URI>,

    var publication: RestDecisionTypePublication,
)

fun BesluitType.toRestDecisionType() = RestDecisionType(
    id = this.url.extractUuid(),
    naam = this.omschrijving,
    toelichting = this.toelichting,
    informatieobjecttypen = this.informatieobjecttypen,
    publication = RestDecisionTypePublication(
        enabled = this.publicatieIndicatie,
        publicationTerm = this.publicatietermijn?.let { PeriodUtil.format(Period.parse(it)) },
        publicationTermDays = this.publicatietermijn?.let { PeriodUtil.numberOfDaysFromToday(Period.parse(it)) },
        responseTerm = this.reactietermijn?.let { PeriodUtil.format(Period.parse(it)) },
        responseTermDays = this.reactietermijn?.let { PeriodUtil.numberOfDaysFromToday(Period.parse(it)) }
    )
)

fun List<BesluitType>.toRestDecisionTypes() =
    this.map { it.toRestDecisionType() }
