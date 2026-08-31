/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.SolrSchemaUpdate
import org.apache.solr.client.solrj.request.schema.SchemaRequest

class SolrSchemaV6 : SolrSchemaUpdate {
    override val versie = 6

    override val teHerindexerenZoekObjectTypes = setOf(ZoekObjectType.ZAAK)

    override val schemaUpdates: List<SchemaRequest.Update> = emptyList()
}
