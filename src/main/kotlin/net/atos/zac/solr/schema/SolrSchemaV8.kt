/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.solr.schema

import net.atos.zac.solr.FieldType.BOOLEAN
import net.atos.zac.solr.SolrSchemaUpdate
import net.atos.zac.solr.SolrSchemaUpdateHelper.addCopyField
import net.atos.zac.solr.SolrSchemaUpdateHelper.addField
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import org.apache.solr.client.solrj.request.schema.SchemaRequest

/**
 * Solr schema version 8 implementation.
 *
 * Indexes whether a zaak is zaakspecifiek geautoriseerd, for zaken, taken, and documenten, so that
 * werklijsten and zoekresultaten can filter on it.
 */
class SolrSchemaV8 : SolrSchemaUpdate {
    override fun getVersie() = 8

    // Reindexing zaken, taken and documenten at startup is too heavy for this field; run it manually instead.
    override fun getTeHerindexerenZoekObjectTypes() = emptySet<ZoekObjectType>()

    override fun getSchemaUpdates(): List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        addField("zaakspecifiekGeautoriseerd", BOOLEAN, true),
        addField("zaak_zaakspecifiekGeautoriseerd", BOOLEAN, true),
        addCopyField("zaak_zaakspecifiekGeautoriseerd", "zaakspecifiekGeautoriseerd"),
        addField("taak_zaakspecifiekGeautoriseerd", BOOLEAN, true),
        addCopyField("taak_zaakspecifiekGeautoriseerd", "zaakspecifiekGeautoriseerd"),
        addField("informatieobject_zaakspecifiekGeautoriseerd", BOOLEAN, true),
        addCopyField("informatieobject_zaakspecifiekGeautoriseerd", "zaakspecifiekGeautoriseerd")
    )
}
