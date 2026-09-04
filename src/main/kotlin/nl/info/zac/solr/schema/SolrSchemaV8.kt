/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
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

    // No zaken are zaakspecifiek geautoriseerd in production yet. Automatic reindexing of existing
    // zaken, taken and documenten is deferred to a later phase of this epic, to avoid a possibly very long
    // reindex on environments with a lot of data; run it manually instead until then.
    override val teHerindexerenZoekObjectTypes = emptySet<ZoekObjectType>()

    override val schemaUpdates: List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        addField(ZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD, BOOLEAN, docValues = true),
        addField(ZaakZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD, BOOLEAN, docValues = true),
        addCopyField(ZaakZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD, ZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD),
        addField(TaakZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD, BOOLEAN, docValues = true),
        addCopyField(TaakZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD, ZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD),
        addField(DocumentZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD, BOOLEAN, docValues = true),
        addCopyField(
            DocumentZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD,
            ZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD
        )
    )
}
