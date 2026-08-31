/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.FieldType.STRING
import nl.info.zac.solr.SolrSchemaUpdate
import nl.info.zac.solr.addCopyField
import nl.info.zac.solr.addDynamicField
import nl.info.zac.solr.addFieldMultiValued
import org.apache.solr.client.solrj.request.schema.SchemaRequest

class SolrSchemaV3 : SolrSchemaUpdate {
    override val versie = 3

    override val teHerindexerenZoekObjectTypes = setOf(ZoekObjectType.ZAAK)

    override val schemaUpdates: List<SchemaRequest.Update> = updateZaakSchema()

    private fun updateZaakSchema(): List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        addDynamicField("zaak_betrokkene_*", STRING, indexed = true, stored = true, multiValued = true),
        addFieldMultiValued("zaak_betrokkenen", STRING, indexed = true, stored = true),
        addCopyField("zaak_betrokkene_*", "zaak_betrokkenen"),
        addCopyField("zaak_initiatorIdentificatie", "zaak_betrokkenen")
    )
}
