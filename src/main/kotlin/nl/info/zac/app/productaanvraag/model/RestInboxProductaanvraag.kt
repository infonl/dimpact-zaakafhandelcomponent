/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.productaanvraag.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestInboxProductaanvraag(
    var id: Long = 0,
    var productaanvraagObjectUUID: UUID,
    var aanvraagdocumentUUID: UUID? = null,
    var aantalBijlagen: Int = 0,
    var type: String,
    var ontvangstdatum: LocalDate,
    var initiatorID: String? = null
)
