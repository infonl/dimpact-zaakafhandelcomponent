/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.inboxdocument.model

import net.atos.zac.document.inboxdocument.model.InboxDocument
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestInboxDocument(
    var id: Long = 0,
    var enkelvoudiginformatieobjectUUID: UUID? = null,
    var enkelvoudiginformatieobjectID: String? = null,
    var informatieobjectTypeUUID: UUID,
    var creatiedatum: LocalDate? = null,
    var titel: String? = null,
    var bestandsnaam: String? = null
)

fun InboxDocument.toRestInboxDocument(informatieobjectTypeUUID: UUID) =
    RestInboxDocument(
        id = this.id,
        enkelvoudiginformatieobjectUUID = this.enkelvoudiginformatieobjectUUID,
        enkelvoudiginformatieobjectID = this.enkelvoudiginformatieobjectID,
        informatieobjectTypeUUID = informatieobjectTypeUUID,
        titel = this.titel,
        creatiedatum = this.creatiedatum,
        bestandsnaam = this.bestandsnaam
    )

fun List<InboxDocument>.toRestInboxDocuments(
    informatieobjectTypeUUIDs: List<UUID?>
): List<RestInboxDocument> {
    val list: MutableList<RestInboxDocument> = ArrayList()
    for (index in this.indices) {
        // skip documents for which no informatieobjectTypeUUID was provided
        informatieobjectTypeUUIDs[index]?.let {
            list.add(this[index].toRestInboxDocument(it))
        }
    }
    return list
}
