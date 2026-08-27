/*
 * SPDX-FileCopyrightText: 2025 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.SolrSchemaUpdate
import org.apache.solr.client.solrj.request.schema.SchemaRequest

/**
 * Solr schema version 7 implementation.
 *
 * This update was introduced to support indexing a newly added [nl.info.zac.search.model.ZaakIndicatie].
 * Although no structural changes are required in the Solr schema itself, the version bump ensures that
 * the corresponding zaken are reindexed to reflect the updated indicatie.
 */
class SolrSchemaV7 : SolrSchemaUpdate {
    override val versie = 7

    override val teHerindexerenZoekObjectTypes = setOf(ZoekObjectType.ZAAK)

    override val schemaUpdates: List<SchemaRequest.Update> = emptyList()
}
