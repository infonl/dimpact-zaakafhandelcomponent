/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.model

import nl.info.zac.search.model.DatumRange
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createInboxProductaanvraag(
    id: Long = 1L,
    productaanvraagObjectUUID: UUID = UUID.randomUUID(),
    aanvraagdocumentUUID: UUID? = null,
    ontvangstdatum: LocalDate = LocalDate.now(),
    type: String = "fakeType",
    initiatorID: String? = null,
    aantalBijlagen: Int = 0
) = InboxProductaanvraag().apply {
    this.id = id
    this.productaanvraagObjectUUID = productaanvraagObjectUUID
    this.aanvraagdocumentUUID = aanvraagdocumentUUID
    this.ontvangstdatum = ontvangstdatum
    this.type = type
    this.initiatorID = initiatorID
    this.aantalBijlagen = aantalBijlagen
}

fun createInboxProductaanvraagListParameters(
    initiatorID: String? = null,
    type: String? = null,
    ontvangstdatumRange: DatumRange? = null
) = InboxProductaanvraagListParameters().apply {
    this.initiatorID = initiatorID
    this.type = type
    this.ontvangstdatum = ontvangstdatumRange
}
