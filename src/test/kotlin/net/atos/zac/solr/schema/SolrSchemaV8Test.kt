/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.solr.schema

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.search.model.zoekobject.ZoekObjectType

class SolrSchemaV8Test : BehaviorSpec({
    given("Solr schema version 8") {
        val solrSchemaV8 = SolrSchemaV8()

        `when`("its version and reindex targets are read") {
            then("the version is 8 and zaken, taken and documenten are marked for reindexing") {
                solrSchemaV8.getVersie() shouldBe 8
                solrSchemaV8.getTeHerindexerenZoekObjectTypes() shouldBe setOf(
                    ZoekObjectType.ZAAK,
                    ZoekObjectType.TAAK,
                    ZoekObjectType.DOCUMENT
                )
            }
        }

        `when`("its schema updates are read") {
            then("a shared field and a per-type field plus copy field are defined for each zoekobject type") {
                // shared field + (per-type field, copy field) for ZAAK, TAAK and DOCUMENT
                solrSchemaV8.getSchemaUpdates().size shouldBe 1 + 3 * 2
            }
        }
    }
})
