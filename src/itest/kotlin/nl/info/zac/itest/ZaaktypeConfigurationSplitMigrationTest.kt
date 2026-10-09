/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.util.MigrationTestDatabase

class ZaaktypeConfigurationSplitMigrationTest : BehaviorSpec({
    val consistentDatabase = MigrationTestDatabase()
    val databaseWithRowOfOtherEngine = MigrationTestDatabase()

    afterSpec {
        consistentDatabase.close()
        databaseWithRowOfOtherEngine.close()
    }

    given(
        """a database at version 101 with a CMMN configuration with plan item settings, a BPMN configuration and a
            CMMN configuration without a case definition"""
    ) {
        consistentDatabase.migrateTo("101")
        consistentDatabase.execute(
            """
            INSERT INTO zaaktype_configuration
                (id, zaaktype_uuid, groep_id, zaaktype_omschrijving, creatiedatum, configuration_type)
            VALUES
                (1, 'a0000000-0000-0000-0000-000000000001', 'fakeGroup', 'fakeCmmn', NOW(), 'CMMN'),
                (2, 'a0000000-0000-0000-0000-000000000002', 'fakeGroup', 'fakeBpmn', NOW(), 'BPMN'),
                (3, 'a0000000-0000-0000-0000-000000000003', 'fakeGroup', 'fakeCmmnWithoutCase', NOW(), 'CMMN');
            INSERT INTO zaaktype_cmmn_configuration (id, id_case_definition, intake_mail, afronden_mail)
            VALUES
                (1, 'fakeCaseDefinition', 'BESCHIKBAAR_AAN', 'BESCHIKBAAR_UIT'),
                (3, NULL, 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT');
            INSERT INTO zaaktype_bpmn_configuration VALUES (2, 'fakeProcessDefinitionKey');
            INSERT INTO zaaktype_cmmn_humantask_parameters (id, zaaktype_configuration_id, id_planitem_definition)
            VALUES (1, 1, 'fakePlanItem');
            INSERT INTO humantask_referentie_tabel
            VALUES (1, (SELECT MIN(id_referentie_tabel) FROM referentie_tabel), 1, 'fakeVeld');
            INSERT INTO zaaktype_cmmn_usereventlistener_parameters (id, id_planitem_definition, zaaktype_configuration_id)
            VALUES (1, 'fakeUserEventListener', 1);
            """.trimIndent()
        )

        `when`("the database is migrated to version 102") {
            consistentDatabase.migrateTo("102")

            then("every configuration with a definition is bound to the engine of its configuration type") {
                consistentDatabase.query(
                    """
                    SELECT zaaktype_configuration_id, process_engine, definition_key
                    FROM zaaktype_process_binding ORDER BY zaaktype_configuration_id
                    """.trimIndent()
                ) shouldBe listOf(
                    listOf("1", "CMMN", "fakeCaseDefinition"),
                    listOf("2", "BPMN", "fakeProcessDefinitionKey")
                )
            }

            then("every CMMN configuration has a CMMN extension with its mail options, and keeps its plan item settings") {
                consistentDatabase.query(
                    """
                    SELECT id, zaaktype_configuration_id, intake_mail, afronden_mail
                    FROM zaaktype_cmmn_extension ORDER BY id
                    """.trimIndent()
                ) shouldBe listOf(
                    listOf("1", "1", "BESCHIKBAAR_AAN", "BESCHIKBAAR_UIT"),
                    listOf("3", "3", "BESCHIKBAAR_UIT", "BESCHIKBAAR_UIT")
                )
                consistentDatabase.query(
                    "SELECT id, zaaktype_cmmn_extension_id, id_planitem_definition FROM zaaktype_cmmn_humantask_parameters"
                ) shouldBe listOf(listOf("1", "1", "fakePlanItem"))
                consistentDatabase.query(
                    "SELECT id_humantask_referentie_tabel, id_humantask_parameters FROM humantask_referentie_tabel"
                ) shouldBe listOf(listOf("1", "1"))
                consistentDatabase.query(
                    "SELECT id, zaaktype_cmmn_extension_id FROM zaaktype_cmmn_usereventlistener_parameters"
                ) shouldBe listOf(listOf("1", "1"))
            }

            then("the configuration type and the subclass tables are gone, and every configuration is kept") {
                consistentDatabase.query(
                    """
                    SELECT table_name FROM information_schema.tables
                    WHERE table_schema = 'zaakafhandelcomponent'
                    AND table_name IN ('zaaktype_cmmn_configuration', 'zaaktype_bpmn_configuration')
                    """.trimIndent()
                ) shouldBe emptyList()
                consistentDatabase.query(
                    """
                    SELECT column_name FROM information_schema.columns
                    WHERE table_schema = 'zaakafhandelcomponent' AND table_name = 'zaaktype_configuration'
                    AND column_name = 'configuration_type'
                    """.trimIndent()
                ) shouldBe emptyList()
                consistentDatabase.query(
                    "SELECT id FROM zaaktype_configuration ORDER BY id"
                ) shouldBe listOf(listOf("1"), listOf("2"), listOf("3"))
            }

            then("a new CMMN extension gets an id after the migrated ones") {
                consistentDatabase.query("SELECT nextval('sq_zaaktype_cmmn_extension')") shouldBe listOf(listOf("4"))
            }
        }
    }

    given("a database at version 101 with a BPMN configuration that also has a row in the CMMN subclass table") {
        databaseWithRowOfOtherEngine.migrateTo("101")
        databaseWithRowOfOtherEngine.execute(
            """
            INSERT INTO zaaktype_configuration
                (id, zaaktype_uuid, groep_id, zaaktype_omschrijving, creatiedatum, configuration_type)
            VALUES (1, 'a0000000-0000-0000-0000-000000000001', 'fakeGroup', 'fakeBpmn', NOW(), 'BPMN');
            INSERT INTO zaaktype_bpmn_configuration VALUES (1, 'fakeProcessDefinitionKey');
            INSERT INTO zaaktype_cmmn_configuration (id, id_case_definition, intake_mail, afronden_mail)
            VALUES (1, 'fakeCaseDefinition', 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT');
            """.trimIndent()
        )

        `when`("the database is migrated to version 102") {
            val illegalStateException = shouldThrow<IllegalStateException> {
                databaseWithRowOfOtherEngine.migrateTo("102")
            }

            then("the migration fails and names the configuration") {
                illegalStateException.message shouldContain
                    "V102: zaaktype configurations 1 have a row in the subclass table of the other engine"
            }
        }
    }
})
