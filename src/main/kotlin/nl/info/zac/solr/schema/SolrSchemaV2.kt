/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.FieldType.PDATE
import nl.info.zac.solr.SolrSchemaUpdate
import nl.info.zac.solr.addField
import nl.info.zac.solr.deleteCopyField
import nl.info.zac.solr.deleteField
import org.apache.solr.client.solrj.request.schema.SchemaRequest

class SolrSchemaV2 : SolrSchemaUpdate {
    override val versie = 2

    override val teHerindexerenZoekObjectTypes = setOf(ZoekObjectType.TAAK)

    override val schemaUpdates: List<SchemaRequest.Update> = updateGenericSchema() + updateTaakSchema()

    private fun updateGenericSchema(): List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        deleteCopyField("zaak_einddatumGepland", "streefdatum"),
        deleteCopyField("taak_streefdatum", "streefdatum"),
        deleteField("streefdatum")
    )

    private fun updateTaakSchema(): List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        deleteField("taak_streefdatum"),
        addField("taak_fataledatum", PDATE)
    )
}
