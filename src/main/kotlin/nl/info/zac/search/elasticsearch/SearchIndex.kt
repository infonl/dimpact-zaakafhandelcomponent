/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.elasticsearch

import nl.info.zac.search.model.zoekobject.ZoekObjectType

/**
 * Central definition of the Elasticsearch indices used for search.
 *
 * Each [ZoekObjectType] is stored in its own index so that the three document types can evolve and be
 * reindexed independently. Full-text search queries all three indices at once (multi-index search) and
 * derives each hit's [ZoekObjectType] from the index it originates from.
 */
enum class SearchIndex(
    val indexName: String,
    val objectType: ZoekObjectType,
    /** Classpath location of the index settings + mapping definition. */
    val mappingResource: String
) {
    ZAAK("zac-zaak", ZoekObjectType.ZAAK, "/elasticsearch/index-zaak.json"),
    TAAK("zac-taak", ZoekObjectType.TAAK, "/elasticsearch/index-taak.json"),
    DOCUMENT("zac-document", ZoekObjectType.DOCUMENT, "/elasticsearch/index-document.json");

    companion object {
        /** All index names, used as the target for multi-index search queries. */
        val ALL_INDEX_NAMES: List<String> = entries.map { it.indexName }

        fun forType(objectType: ZoekObjectType): SearchIndex =
            entries.firstOrNull { it.objectType == objectType }
                ?: error("No search index defined for object type '$objectType'")

        fun indexNameForType(objectType: ZoekObjectType): String = forType(objectType).indexName

        fun objectTypeForIndexName(indexName: String): ZoekObjectType =
            entries.firstOrNull { it.indexName == indexName }?.objectType
                ?: error("No object type defined for search index '$indexName'")
    }
}
