/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.search.model

import net.atos.zac.search.model.zoekobject.ZaakZoekObject
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import java.util.UUID

fun createZaakZoekObject(
    uuidAsString: String = UUID.randomUUID().toString(),
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    zaaktypeOmschrijving: String = "dummyOmschrijving"
) = ZaakZoekObject(
    id = uuidAsString,
    type = type.name
).apply {
    this.zaaktypeOmschrijving = zaaktypeOmschrijving
}
