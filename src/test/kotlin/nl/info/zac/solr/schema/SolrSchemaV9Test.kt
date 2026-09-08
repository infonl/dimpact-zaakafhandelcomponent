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
import org.apache.solr.common.util.Utils
import java.io.ByteArrayOutputStream

class SolrSchemaV9Test : BehaviorSpec({
    given("Solr schema version 9") {
        val solrSchemaV9 = SolrSchemaV9()

        `when`("its version and reindex targets are read") {
            then("the version is 9 and no zoekobject types are marked for reindexing at startup") {
                solrSchemaV9.versie shouldBe 9
                solrSchemaV9.teHerindexerenZoekObjectTypes shouldBe emptySet()
            }
        }

        `when`("its schema updates are read") {
            val schemaUpdatesJson = solrSchemaV9.schemaUpdates.map { update ->
                ByteArrayOutputStream().also { outputStream ->
                    update.getContentWriter("application/json").write(outputStream)
                }.toString(Charsets.UTF_8)
            }

            then("a shared field and a per-type field plus copy field are defined for each zoekobject type") {
                // shared field + (per-type field, copy field) for ZAAK, TAAK and DOCUMENT
                solrSchemaV9.schemaUpdates.size shouldBe 1 + 3 * 2
            }

            then("the shared field and each zoekobject's own Solr field are actually defined") {
                schemaUpdatesJson.joinToString("\n").let { allUpdates ->
                    // quoted so a rename of a prefixed field (e.g. "zaak_zaakGeautoriseerdeMedewerkers")
                    // can never satisfy this on a substring match alone
                    allUpdates shouldContain "\"${ZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD}\""
                    allUpdates shouldContain ZaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD
                    allUpdates shouldContain TaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD
                    allUpdates shouldContain DocumentZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD
                }
            }

            then("every field is multi-valued, so more than one medewerker can be recorded per row") {
                schemaUpdatesJson
                    .map { Utils.fromJSONString(it) as Map<*, *> }
                    .mapNotNull { it["add-field"] as? Map<*, *> }
                    .also { it.size shouldBe 4 }
                    .forEach { it["multiValued"] shouldBe true }
            }

            then("each per-type field is copied into the shared field, and not the other way around") {
                val copyFieldsBySource = schemaUpdatesJson
                    .map { Utils.fromJSONString(it) as Map<*, *> }
                    .mapNotNull { it["add-copy-field"] as? Map<*, *> }
                    .associate { it["source"] to it["dest"] }

                copyFieldsBySource shouldBe mapOf(
                    ZaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD to
                        listOf(ZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD),
                    TaakZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD to
                        listOf(ZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD),
                    DocumentZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD to
                        listOf(ZoekObject.ZAAK_GEAUTORISEERDE_MEDEWERKERS_FIELD)
                )
            }
        }
    }
})
