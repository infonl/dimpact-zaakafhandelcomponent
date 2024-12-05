/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.zac.util.time.PeriodUtil
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.net.URI
import java.time.Period
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestBesluittype(
    var id: UUID,

    var naam: String,

    var toelichting: String,

    var informatieobjecttypen: List<URI>,

    var publicatieIndicatie: Boolean,

    var publicatietermijn: String?,

    var reactietermijn: String?
)

fun BesluitType.toRestBesluitType() = RestBesluittype(
    id = this.url.extractUuid(),
    naam = this.omschrijving,
    toelichting = this.toelichting,
    informatieobjecttypen = this.informatieobjecttypen.toList(),
    publicatieIndicatie = this.publicatieIndicatie,
    publicatietermijn = this.publicatietermijn?.let { PeriodUtil.format(Period.parse(it)) },
    reactietermijn = this.reactietermijn?.let { PeriodUtil.format(Period.parse(it)) }
)

fun List<BesluitType>.toRestBesluittypes() = this
    .map { it.toRestBesluitType() }
