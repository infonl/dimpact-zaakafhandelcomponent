/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.FieldType.STRING
import nl.info.zac.solr.SolrSchemaUpdate
import nl.info.zac.solr.addFieldMultiValued
import org.apache.solr.client.solrj.request.schema.SchemaRequest

class SolrSchemaV5 : SolrSchemaUpdate {
    override val versie = 5

    override val teHerindexerenZoekObjectTypes = setOf(ZoekObjectType.ZAAK)

    override val schemaUpdates: List<SchemaRequest.Update> =
        listOf(addFieldMultiValued("zaak_bagObjecten", STRING, indexed = true, stored = true))
}
