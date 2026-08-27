/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.FieldType.BOOLEAN
import nl.info.zac.solr.SolrSchemaUpdate
import nl.info.zac.solr.addCopyField
import nl.info.zac.solr.addField
import org.apache.solr.client.solrj.request.schema.SchemaRequest

/**
 * Solr schema version 8 implementation.
 *
 * Indexes whether a zaak is zaakspecifiek geautoriseerd, for zaken, taken, and documenten, so that
 * werklijsten and zoekresultaten can filter on it.
 */
class SolrSchemaV8 : SolrSchemaUpdate {
    override val versie = 8

    // Reindexing zaken, taken and documenten at startup is not done automatically; run it manually instead.
    override val teHerindexerenZoekObjectTypes = emptySet<ZoekObjectType>()

    override val schemaUpdates: List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        addField("zaakspecifiekGeautoriseerd", BOOLEAN, true),
        addField("zaak_zaakspecifiekGeautoriseerd", BOOLEAN, true),
        addCopyField("zaak_zaakspecifiekGeautoriseerd", "zaakspecifiekGeautoriseerd"),
        addField("taak_zaakspecifiekGeautoriseerd", BOOLEAN, true),
        addCopyField("taak_zaakspecifiekGeautoriseerd", "zaakspecifiekGeautoriseerd"),
        addField("informatieobject_zaakspecifiekGeautoriseerd", BOOLEAN, true),
        addCopyField("informatieobject_zaakspecifiekGeautoriseerd", "zaakspecifiekGeautoriseerd")
    )
}
