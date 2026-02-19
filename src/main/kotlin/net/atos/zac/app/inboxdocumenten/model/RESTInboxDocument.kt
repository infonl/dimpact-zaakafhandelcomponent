/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten.model

import net.atos.zac.documenten.model.InboxDocument
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RESTInboxDocument(
    var id: Long = 0,
    var enkelvoudiginformatieobjectUUID: UUID? = null,
    var enkelvoudiginformatieobjectID: String? = null,
    var informatieobjectTypeUUID: UUID? = null,
    var creatiedatum: LocalDate? = null,
    var titel: String? = null,
    var bestandsnaam: String? = null
)

fun InboxDocument.convertToRESTInboxDocument(informatieobjectTypeUUID: UUID?) =
    RESTInboxDocument(
        id = this.id,
        enkelvoudiginformatieobjectUUID = this.enkelvoudiginformatieobjectUUID,
        enkelvoudiginformatieobjectID = this.enkelvoudiginformatieobjectID,
        informatieobjectTypeUUID = informatieobjectTypeUUID,
        titel = this.titel,
        creatiedatum = this.creatiedatum,
        bestandsnaam = this.bestandsnaam
    )

fun List<InboxDocument>.convertToRESTInboxDocuments(
    informatieobjectTypeUUIDs: List<UUID?>
): MutableList<RESTInboxDocument> {
    val list: MutableList<RESTInboxDocument> = ArrayList()
    for (index in this.indices) {
        // Skip documents for which we don't have an informatieobjectTypeUUID
        if (informatieobjectTypeUUIDs[index] == null) continue
        list.add(this[index].convertToRESTInboxDocument(informatieobjectTypeUUIDs[index]))
    }
    return list
}
