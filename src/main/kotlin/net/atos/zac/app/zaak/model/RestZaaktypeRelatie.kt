/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import net.atos.client.zgw.ztc.model.generated.ZaakTypenRelatie
import net.atos.zac.util.uuidFromURI
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.net.URI
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestZaaktypeRelatie(
    var zaaktypeUuid: UUID,

    var relatieType: RelatieType
)

fun RelatieType.toRestZaaktypeRelatie(zaaktypUri: URI) =
    RestZaaktypeRelatie(
        zaaktypeUuid = uuidFromURI(zaaktypUri),
        relatieType = this
    )

fun ZaakTypenRelatie.toRestZaaktypeRelatie() =
    RestZaaktypeRelatie(
        zaaktypeUuid = uuidFromURI(this.zaaktype),
        relatieType = RelatieType.valueOf(this.aardRelatie.name)
    )
