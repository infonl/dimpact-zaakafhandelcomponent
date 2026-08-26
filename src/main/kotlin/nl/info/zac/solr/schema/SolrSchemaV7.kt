/*
 * SPDX-FileCopyrightText: 2025 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.solr.schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.request.schema.SchemaRequest;

import net.atos.zac.solr.SolrSchemaUpdate;
import nl.info.zac.search.model.zoekobject.ZoekObjectType;

/**
 * Solr schema version 7 implementation.
 *
 * This update was introduced to support indexing a newly added {@link nl.info.zac.search.model.ZaakIndicatie}.
 * Although no structural changes are required in the Solr schema itself, the version bump ensures that
 * the corresponding zaken are reindexed to reflect the updated indicatie.
 */
class SolrSchemaV7 implements SolrSchemaUpdate {

    @Override
    public int getVersie() {
        return 7;
    }

    @Override
    public Set<ZoekObjectType> getTeHerindexerenZoekObjectTypes() {
        return Set.of(ZoekObjectType.ZAAK);
    }

    @Override
    public List<SchemaRequest.Update> getSchemaUpdates() {
        return Collections.emptyList();
    }
}
