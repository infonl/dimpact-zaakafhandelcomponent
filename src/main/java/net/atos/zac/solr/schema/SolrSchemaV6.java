/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.solr.schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.request.schema.SchemaRequest;

import net.atos.zac.solr.SolrSchemaUpdate;
import nl.info.zac.search.model.zoekobject.ZoekObjectType;


class SolrSchemaV6 implements SolrSchemaUpdate {

    @Override
    public int getVersie() {
        return 6;
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
