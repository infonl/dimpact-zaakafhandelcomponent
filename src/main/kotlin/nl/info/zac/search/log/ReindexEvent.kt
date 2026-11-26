/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.log

import nl.info.zac.log.LogEvent
import nl.info.zac.search.model.zoekobject.ZoekObjectType

class ReindexEvent(
    val type: Type,
    val objectType: ZoekObjectType
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        EXISTING_DOCUMENTS_DELETION_STARTED("Deletion of the existing documents before reindexing started"),
        INDEXING_FAILED("Error during indexing"),
        REINDEX_COUNT_NOT_FOUND("Cannot find the count; aborting reindexing"),
        REINDEXING_NOT_STARTED("Reindexing not started, still in progress"),
        SOLR_DOCUMENT_COUNT_FAILED("Error counting Solr documents")
    }
}
