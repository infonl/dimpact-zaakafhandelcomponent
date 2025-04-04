/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.solr.schema;

import static net.atos.zac.solr.FieldType.STRING;
import static net.atos.zac.solr.SolrSchemaUpdateHelper.addCopyField;
import static net.atos.zac.solr.SolrSchemaUpdateHelper.addDynamicField;
import static net.atos.zac.solr.SolrSchemaUpdateHelper.addFieldMultiValued;

import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.request.schema.SchemaRequest;

import net.atos.zac.solr.SolrSchemaUpdate;
import nl.info.zac.search.model.zoekobject.ZoekObjectType;

class SolrSchemaV3 implements SolrSchemaUpdate {

    @Override
    public int getVersie() {
        return 3;

    }

    @Override
    public Set<ZoekObjectType> getTeHerindexerenZoekObjectTypes() {
        return Set.of(ZoekObjectType.ZAAK);
    }

    @Override
    public List<SchemaRequest.Update> getSchemaUpdates() {
        return updateZaakSchema();
    }

    private List<SchemaRequest.Update> updateZaakSchema() {
        return List.of(
                addDynamicField("zaak_betrokkene_*", STRING, true, true, true),
                addFieldMultiValued("zaak_betrokkenen", STRING, true, true),
                addCopyField("zaak_betrokkene_*", "zaak_betrokkenen"),
                addCopyField("zaak_betrokkene_*", "zaak_betrokkenen"),
                addCopyField("zaak_initiatorIdentificatie", "zaak_betrokkenen")
        );
    }

}
