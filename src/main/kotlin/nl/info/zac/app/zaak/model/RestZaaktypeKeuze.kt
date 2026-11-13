/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

/**
 * A zaaktype that a user can choose when creating a zaak.
 */
@NoArgConstructor
@AllOpen
data class RestZaaktypeKeuze(
    var uuid: UUID,
    var omschrijving: String
)

fun ZaakType.toRestZaaktypeKeuze() = RestZaaktypeKeuze(uuid = url.extractUuid(), omschrijving = omschrijving)
