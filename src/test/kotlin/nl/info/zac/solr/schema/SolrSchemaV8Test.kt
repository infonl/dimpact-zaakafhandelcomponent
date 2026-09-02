/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import java.io.ByteArrayOutputStream

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
            val schemaUpdatesJson = solrSchemaV8.schemaUpdates.map { update ->
                ByteArrayOutputStream().also { outputStream ->
                    update.getContentWriter("application/json").write(outputStream)
                }.toString(Charsets.UTF_8)
            }

            then("a shared field and a per-type field plus copy field are defined for each zoekobject type") {
                // shared field + (per-type field, copy field) for ZAAK, TAAK and DOCUMENT
                solrSchemaV8.schemaUpdates.size shouldBe 1 + 3 * 2
            }

            then("the shared field and each zoekobject's own Solr field are actually defined") {
                schemaUpdatesJson.joinToString("\n").let { allUpdates ->
                    // quoted so a rename of a prefixed field (e.g. "zaak_zaakspecifiekGeautoriseerd")
                    // can never satisfy this on a substring match alone
                    allUpdates shouldContain "\"${ZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD}\""
                    allUpdates shouldContain ZaakZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD
                    allUpdates shouldContain TaakZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD
                    allUpdates shouldContain DocumentZoekObject.ZAAKSPECIFIEK_GEAUTORISEERD_FIELD
                }
            }
        }
    }
})
