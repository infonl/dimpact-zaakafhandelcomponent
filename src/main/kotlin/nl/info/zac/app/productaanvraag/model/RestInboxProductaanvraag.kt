/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.productaanvraag.model

import nl.info.zac.productaanvraag.model.InboxProductaanvraag
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
    var ontvangstdatum: LocalDate? = null,
    var initiatorID: String? = null
)

fun InboxProductaanvraag.toRestInboxProductaanvraag() = RestInboxProductaanvraag(
    id = this.id ?: 0,
    productaanvraagObjectUUID = this.productaanvraagObjectUUID,
    aanvraagdocumentUUID = this.aanvraagdocumentUUID,
    aantalBijlagen = this.aantalBijlagen,
    type = this.type,
    ontvangstdatum = this.ontvangstdatum,
    initiatorID = this.initiatorID
)

fun List<InboxProductaanvraag>.toRestInboxProductaanvragen() = map { it.toRestInboxProductaanvraag() }
