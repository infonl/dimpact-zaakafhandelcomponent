/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.zac.util.uuidFromURI
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.net.URI
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestBesluittype(
    var id: UUID,

    var naam: String,

    var toelichting: String,

    var informatieobjecttypen: List<URI>
)

fun BesluitType.toRestBesluitType() = RestBesluittype(
    id = uuidFromURI(this.url),
    naam = this.omschrijving,
    toelichting = this.toelichting,
    informatieobjecttypen = this.informatieobjecttypen.toList()
)

fun List<BesluitType>.toRestBesluittypes() = this
    .map { it.toRestBesluitType() }
