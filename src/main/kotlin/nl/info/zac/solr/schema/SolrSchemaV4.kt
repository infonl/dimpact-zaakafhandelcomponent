/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.FieldType.PDATE
import nl.info.zac.solr.FieldType.STRING
import nl.info.zac.solr.SolrSchemaUpdate
import nl.info.zac.solr.addField
import org.apache.solr.client.solrj.request.schema.SchemaRequest

class SolrSchemaV4 : SolrSchemaUpdate {
    override val versie = 4

    override val teHerindexerenZoekObjectTypes = setOf(ZoekObjectType.ZAAK)

    override val schemaUpdates: List<SchemaRequest.Update> = updateZaakSchema()

    private fun updateZaakSchema(): List<SchemaRequest.Update> = listOf(
        addField("zaak_archiefNominatie", STRING, docValues = true),
        addField("zaak_archiefActiedatum", PDATE)
    )
}
