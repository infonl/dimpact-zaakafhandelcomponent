/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.log

import nl.info.zac.log.LogEvent
import nl.info.zac.search.model.zoekobject.ZoekObjectType

sealed class ReindexEvent(description: String, val objectType: ZoekObjectType) : LogEvent(description) {
    class ExistingDocumentsDeletionStarted(objectType: ZoekObjectType) :
        ReindexEvent("Deletion of the existing documents before reindexing started", objectType)

    class IndexingFailed(objectType: ZoekObjectType) : ReindexEvent("Error during indexing", objectType)

    class ReindexCountNotFound(objectType: ZoekObjectType) :
        ReindexEvent("Cannot find the count; aborting reindexing", objectType)

    class ReindexingNotStarted(objectType: ZoekObjectType) :
        ReindexEvent("Reindexing not started, still in progress", objectType)

    class SolrDocumentCountFailed(objectType: ZoekObjectType) : ReindexEvent("Error counting Solr documents", objectType)
}
