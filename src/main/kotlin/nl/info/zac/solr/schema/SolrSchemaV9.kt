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
import nl.info.zac.solr.FieldType.STRING
import nl.info.zac.solr.SolrSchemaUpdate
import nl.info.zac.solr.addCopyField
import nl.info.zac.solr.addFieldMultiValued
import org.apache.solr.client.solrj.request.schema.SchemaRequest

/**
 * Solr schema version 9 implementation.
 *
 * Indexes the medewerkers who are individually authorised for the zaak a row belongs to, for zaken, taken
 * and documenten, so that werklijsten and zoekresultaten can keep a zaakspecifiek geautoriseerde zaak
 * visible to them.
 *
 * The fields are multi-valued even though only the zaak's current behandelaar is recorded in them for now,
 * so that widening the set later needs neither a new Solr schema version nor another manual reindex.
 */
class SolrSchemaV9 : SolrSchemaUpdate {
    override val versie = 9

    // Reindexing existing zaken, taken and documenten is run manually per environment, to avoid a possibly
    // very long reindex at startup on environments with a lot of data, as in SolrSchemaV8.
    override val teHerindexerenZoekObjectTypes = emptySet<ZoekObjectType>()

    override val schemaUpdates: List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        addFieldMultiValued(ZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD, STRING, docValues = true),
        addFieldMultiValued(ZaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD, STRING, docValues = true),
        addCopyField(
            ZaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD,
            ZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD
        ),
        addFieldMultiValued(TaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD, STRING, docValues = true),
        addCopyField(
            TaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD,
            ZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD
        ),
        addFieldMultiValued(DocumentZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD, STRING, docValues = true),
        addCopyField(
            DocumentZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD,
            ZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD
        )
    )
}
