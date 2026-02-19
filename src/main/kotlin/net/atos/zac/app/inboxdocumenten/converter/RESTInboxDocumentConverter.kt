/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten.converter

import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocument
import net.atos.zac.documenten.model.InboxDocument
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
object RESTInboxDocumentConverter {
    fun convert(document: InboxDocument, informatieobjectTypeUUID: UUID?): RESTInboxDocument {
        val restDocument = RESTInboxDocument()
        restDocument.id = document.getId()
        restDocument.enkelvoudiginformatieobjectUUID = document.enkelvoudiginformatieobjectUUID
        restDocument.enkelvoudiginformatieobjectID = document.enkelvoudiginformatieobjectID
        restDocument.informatieobjectTypeUUID = informatieobjectTypeUUID
        restDocument.titel = document.titel
        restDocument.creatiedatum = document.creatiedatum
        restDocument.bestandsnaam = document.bestandsnaam
        return restDocument
    }

    fun convert(
        documenten: List<InboxDocument?>,
        informatieobjectTypeUUIDs: List<UUID?>
    ): MutableList<RESTInboxDocument?> {
        val list: MutableList<RESTInboxDocument?> = ArrayList<RESTInboxDocument?>()
        for (index in documenten.indices) {
            // Skip documents for which we don't have an informatieobjectTypeUUID
            if (informatieobjectTypeUUIDs.get(index) == null) continue
            list.add(
                convert(
                    documenten[index]!!,
                    informatieobjectTypeUUIDs.get(index)
                )
            )
        }
        return list
    }
}
