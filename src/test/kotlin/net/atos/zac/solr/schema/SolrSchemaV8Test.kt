/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.solr.schema

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.solr.schema.SolrSchemaV8

class SolrSchemaV8Test : BehaviorSpec({
    given("Solr schema version 8") {
        val solrSchemaV8 = SolrSchemaV8()

        `when`("its version and reindex targets are read") {
            then("the version is 8 and no zoekobject types are marked for reindexing at startup") {
                solrSchemaV8.versie shouldBe 8
                solrSchemaV8.teHerindexerenZoekObjectTypes shouldBe emptySet()
            }
        }

        `when`("its schema updates are read") {
            then("a shared field and a per-type field plus copy field are defined for each zoekobject type") {
                // shared field + (per-type field, copy field) for ZAAK, TAAK and DOCUMENT
                solrSchemaV8.schemaUpdates.size shouldBe 1 + 3 * 2
            }
        }
    }
})
