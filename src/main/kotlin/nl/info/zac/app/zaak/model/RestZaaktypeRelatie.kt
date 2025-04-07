/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.generated.ZaakTypenRelatie
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
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
        zaaktypeUuid = zaaktypUri.extractUuid(),
        relatieType = this
    )

fun ZaakTypenRelatie.toRestZaaktypeRelatie() =
    RestZaaktypeRelatie(
        zaaktypeUuid = this.zaaktype.extractUuid(),
        relatieType = RelatieType.valueOf(this.aardRelatie.name)
    )
