/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.drc.model

import net.atos.client.zgw.drc.model.generated.BestandsDeel
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectData
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockData
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
    bestandsdelen: List<BestandsDeel> = emptyList()
) = EnkelvoudigInformatieObject(
    url,
    versie,
    beginRegistratie,
    inhoud,
    locked,
    bestandsdelen
)

fun createEnkelvoudigInformatieObjectData(
    url: URI = URI("http://example.com/${UUID.randomUUID()}"),
    versie: Int = 1234,
    beginRegistratie: OffsetDateTime = OffsetDateTime.now(),
    locked: Boolean = false,
    bestandsdelen: List<BestandsDeel> = emptyList()
) = EnkelvoudigInformatieObjectData(
    url,
    versie,
    beginRegistratie,
    locked,
    bestandsdelen
)

fun createEnkelvoudigInformatieObjectWithLockData(
    url: URI = URI("http://example.com/${UUID.randomUUID()}"),
    versie: Int = 1234,
    beginRegistratie: OffsetDateTime = OffsetDateTime.now(),
    locked: Boolean = false,
    bestandsdelen: List<BestandsDeel> = emptyList()
) = EnkelvoudigInformatieObjectWithLockData(
    url,
    versie,
    beginRegistratie,
    locked,
    bestandsdelen
)
