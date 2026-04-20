/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.productaanvraag.converter

import nl.info.zac.app.productaanvraag.model.RestInboxProductaanvraag
import nl.info.zac.productaanvraag.model.InboxProductaanvraag

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
