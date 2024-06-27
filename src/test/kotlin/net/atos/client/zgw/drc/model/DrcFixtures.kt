/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.drc.model

import net.atos.client.zgw.drc.model.generated.BestandsDeel
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createEnkelvoudigInformatieObject(
    url: URI = URI("http://example.com/${UUID.randomUUID()}"),
    versie: Int = 1234,
    beginRegistratie: OffsetDateTime = OffsetDateTime.now(),
    inhoud: URI = URI("http://example.com/${UUID.randomUUID()}"),
    locked: Boolean = false,
    bestandsdelen: List<BestandsDeel> = emptyList(),
    indicatieGebruiksrecht: Boolean? = null
) = EnkelvoudigInformatieObject(
    url,
    versie,
    beginRegistratie,
    locked,
    bestandsdelen
).apply {
    this.indicatieGebruiksrecht = indicatieGebruiksrecht
    this.inhoud(inhoud)
}

fun createEnkelvoudigInformatieObjectCreateLockRequest(
    url: URI = URI("http://example.com/${UUID.randomUUID()}"),
    versie: Int = 1234,
    beginRegistratie: OffsetDateTime = OffsetDateTime.now(),
    locked: Boolean = false,
    bestandsdelen: List<BestandsDeel> = emptyList()
) = EnkelvoudigInformatieObjectCreateLockRequest().apply {
    this.link = url
    // TODO
}

fun createEnkelvoudigInformatieObjectWithLockRequest() = EnkelvoudigInformatieObjectWithLockRequest().apply {
    // TODO
}
