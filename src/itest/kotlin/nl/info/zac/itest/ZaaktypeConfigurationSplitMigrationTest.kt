/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.util.MigrationTestDatabase

private const val CMMN_ROW_OF_BPMN_CONFIGURATION = "CMMN row of a BPMN zaaktype configuration"
private const val CHILD_OF_CMMN_ROW_OF_BPMN_CONFIGURATION = "child of a CMMN row of a BPMN zaaktype configuration"
private const val BPMN_ROW_OF_CMMN_CONFIGURATION = "BPMN row of a CMMN zaaktype configuration"

class ZaaktypeConfigurationSplitMigrationTest : BehaviorSpec({
    val migrationTestDatabase = MigrationTestDatabase()

    afterSpec { migrationTestDatabase.close() }

    fun rowAsJson(table: String, condition: String) =
        migrationTestDatabase.query("SELECT to_jsonb(t)::text FROM $table t WHERE $condition").single().single()

    given(
        """a database at version 101 with CMMN and BPMN configurations, a CMMN configuration without a case
            definition, and subclass rows of the other engine"""
    ) {
        migrationTestDatabase.migrateTo("101")
        migrationTestDatabase.execute(
            """
            INSERT INTO zaaktype_configuration
                (id, zaaktype_uuid, groep_id, zaaktype_omschrijving, creatiedatum, configuration_type)
            VALUES
                (1, 'a0000000-0000-0000-0000-000000000001', 'fakeGroup', 'fakeCmmn', NOW(), 'CMMN'),
                (2, 'a0000000-0000-0000-0000-000000000002', 'fakeGroup', 'fakeBpmn', NOW(), 'BPMN'),
                (3, 'a0000000-0000-0000-0000-000000000003', 'fakeGroup', 'fakeCmmnWithoutCase', NOW(), 'CMMN'),
                (4, 'a0000000-0000-0000-0000-000000000004', 'fakeGroup', 'fakeBpmnWithCmmnRow', NOW(), 'BPMN');
            INSERT INTO zaaktype_cmmn_configuration (id, id_case_definition, intake_mail, afronden_mail)
            VALUES
                (1, 'fakeCaseDefinition', 'BESCHIKBAAR_AAN', 'BESCHIKBAAR_UIT'),
                (3, NULL, 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT'),
                (4, 'fakeStrayCaseDefinition', 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT');
            INSERT INTO zaaktype_bpmn_configuration
            VALUES (2, 'fakeProcessDefinitionKey'), (4, 'fakeProcessDefinitionKey4'), (1, 'fakeStrayProcessDefinitionKey');
            INSERT INTO zaaktype_cmmn_humantask_parameters (id, zaaktype_configuration_id, id_planitem_definition)
            VALUES (1, 1, 'fakePlanItem'), (2, 4, 'fakeStrayPlanItem');
            INSERT INTO humantask_referentie_tabel
            VALUES
                (1, (SELECT MIN(id_referentie_tabel) FROM referentie_tabel), 1, 'fakeVeld'),
                (2, (SELECT MIN(id_referentie_tabel) FROM referentie_tabel), 2, 'fakeStrayVeld');
            INSERT INTO zaaktype_cmmn_usereventlistener_parameters (id, id_planitem_definition, zaaktype_configuration_id)
            VALUES (1, 'fakeUserEventListener', 1);
            """.trimIndent()
        )
        val expectedQuarantine = listOf(
            listOf(
                "humantask_referentie_tabel",
                CHILD_OF_CMMN_ROW_OF_BPMN_CONFIGURATION,
                rowAsJson("humantask_referentie_tabel", "id_humantask_referentie_tabel = 2")
            ),
            listOf(
                "zaaktype_cmmn_humantask_parameters",
                CHILD_OF_CMMN_ROW_OF_BPMN_CONFIGURATION,
                rowAsJson("zaaktype_cmmn_humantask_parameters", "id = 2")
            ),
            listOf(
                "zaaktype_cmmn_configuration",
                CMMN_ROW_OF_BPMN_CONFIGURATION,
                rowAsJson("zaaktype_cmmn_configuration", "id = 4")
            ),
            listOf(
                "zaaktype_bpmn_configuration",
                BPMN_ROW_OF_CMMN_CONFIGURATION,
                rowAsJson("zaaktype_bpmn_configuration", "id = 1")
            )
        )

        `when`("the database is migrated to version 102") {
            val flywayOutput = migrationTestDatabase.migrateTo("102")

            then("every configuration with a definition is bound to the engine of its configuration type") {
                migrationTestDatabase.query(
                    """
                    SELECT zaaktype_configuration_id, process_engine, definition_key
                    FROM zaaktype_process_binding ORDER BY zaaktype_configuration_id
                    """.trimIndent()
                ) shouldBe listOf(
                    listOf("1", "CMMN", "fakeCaseDefinition"),
                    listOf("2", "BPMN", "fakeProcessDefinitionKey"),
                    listOf("4", "BPMN", "fakeProcessDefinitionKey4")
                )
            }

            then("every CMMN configuration has a CMMN extension with its mail options, and keeps its plan item settings") {
                migrationTestDatabase.query(
                    """
                    SELECT id, zaaktype_configuration_id, intake_mail, afronden_mail
                    FROM zaaktype_cmmn_extension ORDER BY id
                    """.trimIndent()
                ) shouldBe listOf(
                    listOf("1", "1", "BESCHIKBAAR_AAN", "BESCHIKBAAR_UIT"),
                    listOf("3", "3", "BESCHIKBAAR_UIT", "BESCHIKBAAR_UIT")
                )
                migrationTestDatabase.query(
                    "SELECT id, zaaktype_cmmn_extension_id, id_planitem_definition FROM zaaktype_cmmn_humantask_parameters"
                ) shouldBe listOf(listOf("1", "1", "fakePlanItem"))
                migrationTestDatabase.query(
                    "SELECT id_humantask_referentie_tabel, id_humantask_parameters FROM humantask_referentie_tabel"
                ) shouldBe listOf(listOf("1", "1"))
                migrationTestDatabase.query(
                    "SELECT id, zaaktype_cmmn_extension_id FROM zaaktype_cmmn_usereventlistener_parameters"
                ) shouldBe listOf(listOf("1", "1"))
            }

            then("the subclass rows of the other engine are in the quarantine table with exactly their original data") {
                migrationTestDatabase.query(
                    """
                    SELECT source_table, reason, row_data::text
                    FROM zaaktype_configuration_migration_quarantine
                    WHERE migration = 'V102'
                    """.trimIndent()
                ) shouldContainExactlyInAnyOrder expectedQuarantine
                flywayOutput shouldContain
                    "V102: moved 1 row(s) of zaaktype_cmmn_configuration to " +
                    "zaaktype_configuration_migration_quarantine: $CMMN_ROW_OF_BPMN_CONFIGURATION"
            }

            then("the configuration type and the subclass tables are gone, and every configuration is kept") {
                migrationTestDatabase.query(
                    """
                    SELECT table_name FROM information_schema.tables
                    WHERE table_schema = 'zaakafhandelcomponent'
                    AND table_name IN ('zaaktype_cmmn_configuration', 'zaaktype_bpmn_configuration')
                    """.trimIndent()
                ) shouldBe emptyList()
                migrationTestDatabase.query(
                    """
                    SELECT column_name FROM information_schema.columns
                    WHERE table_schema = 'zaakafhandelcomponent' AND table_name = 'zaaktype_configuration'
                    AND column_name = 'configuration_type'
                    """.trimIndent()
                ) shouldBe emptyList()
                migrationTestDatabase.query(
                    "SELECT id FROM zaaktype_configuration ORDER BY id"
                ) shouldBe listOf(listOf("1"), listOf("2"), listOf("3"), listOf("4"))
            }

            then("a new CMMN extension gets an id after the migrated ones") {
                migrationTestDatabase.query("SELECT nextval('sq_zaaktype_cmmn_extension')") shouldBe listOf(listOf("4"))
            }
        }
    }
})
