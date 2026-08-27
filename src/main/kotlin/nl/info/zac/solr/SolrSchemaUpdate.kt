/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import org.apache.solr.client.solrj.request.schema.SchemaRequest

interface SolrSchemaUpdate {
    val versie: Int

    val teHerindexerenZoekObjectTypes: Set<ZoekObjectType>
        get() = emptySet()

    val schemaUpdates: List<SchemaRequest.Update>
}
