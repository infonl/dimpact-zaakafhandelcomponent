/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.util.MigrationTestDatabase

private const val CONFIGURATION_WITHOUT_TYPE_REASON = "child of a zaaktype configuration without configuration type"
private const val ORPHANED_CMMN_CHILD_REASON = "child of a CMMN configuration without zaaktype configuration"

class ZaaktypeConfigurationSchemaRepairMigrationTest : BehaviorSpec({
    val migrationTestDatabase = MigrationTestDatabase()

    afterSpec { migrationTestDatabase.close() }

    given(
        """a database at version 99 with a valid CMMN and a valid BPMN configuration, and with rows that block
            the constraints of version 100"""
    ) {
        migrationTestDatabase.migrateTo("99")
        migrationTestDatabase.execute(
            """
            INSERT INTO zaakbeeindigreden VALUES (900, 'fakeZaakbeeindigReden');
            INSERT INTO zaaktype_configuration
                (id, zaaktype_uuid, groep_id, zaaktype_omschrijving, creatiedatum, configuration_type)
            VALUES
                (1, 'a0000000-0000-0000-0000-000000000001', 'fakeGroup', 'cmmn', NOW(), 'CMMN'),
                (2, 'a0000000-0000-0000-0000-000000000002', 'fakeGroup', 'bpmn', NOW(), 'BPMN'),
                (3, 'a0000000-0000-0000-0000-000000000003', 'fakeGroup', 'untypedWithoutSubclass', NOW(), NULL),
                (4, 'a0000000-0000-0000-0000-000000000004', 'fakeGroup', 'untypedCmmn', NOW(), NULL);
            INSERT INTO zaaktype_cmmn_configuration (id, id_case_definition, intake_mail, afronden_mail)
            VALUES
                (1, 'fakeCaseDefinition', 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT'),
                (4, 'fakeCaseDefinition4', 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT'),
                (50, 'fakeOrphanedCaseDefinition', 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT');
            INSERT INTO zaaktype_bpmn_configuration
            VALUES (2, 'fakeProcessDefinitionKey'), (2, 'fakeDuplicateProcessDefinitionKey'), (60, 'fakeOrphanedKey');
            INSERT INTO zaaktype_betrokkene_parameters (id, zaaktype_configuration_id, brpkoppelen) VALUES (1, 1, TRUE);
            INSERT INTO zaaktype_brp_parameters (id, zaaktype_configuration_id, zoekwaarde)
            VALUES (1, 1, 'fakeOldZoekwaarde'), (2, 1, 'fakeNewZoekwaarde'), (3, 3, 'fakeUntypedZoekwaarde');
            INSERT INTO zaaktype_completion_parameters
            VALUES
                (1, 1, 900, 'b0000000-0000-0000-0000-000000000001'),
                (2, 3, 900, 'b0000000-0000-0000-0000-000000000002');
            INSERT INTO zaaktype_cmmn_email_parameters (id, zaaktype_configuration_id, enabled) VALUES (1, 1, TRUE);
            INSERT INTO zaaktype_cmmn_humantask_parameters (id, zaaktype_configuration_id, id_planitem_definition)
            VALUES (1, 1, 'fakePlanItem'), (2, 50, 'fakeOrphanedPlanItem');
            INSERT INTO humantask_referentie_tabel
            VALUES (1, (SELECT MIN(id_referentie_tabel) FROM referentie_tabel), 2, 'fakeVeld');
            INSERT INTO zaaktype_cmmn_zaakafzender_parameters (id, zaaktype_configuration_id, mail)
            VALUES (1, 50, 'orphaned@example.com');
            """.trimIndent()
        )

        `when`("the database is migrated to version 100") {
            migrationTestDatabase.migrateTo("100")

            then("the valid configurations and their child rows are unchanged") {
                migrationTestDatabase.query(
                    "SELECT id, configuration_type FROM zaaktype_configuration ORDER BY id"
                ) shouldBe listOf(listOf("1", "CMMN"), listOf("2", "BPMN"), listOf("4", "CMMN"))
                migrationTestDatabase.query(
                    "SELECT id, id_case_definition FROM zaaktype_cmmn_configuration ORDER BY id"
                ) shouldBe listOf(listOf("1", "fakeCaseDefinition"), listOf("4", "fakeCaseDefinition4"))
                migrationTestDatabase.query(
                    "SELECT zaaktype_configuration_id, brpkoppelen FROM zaaktype_betrokkene_parameters"
                ) shouldBe listOf(listOf("1", "t"))
                migrationTestDatabase.query(
                    "SELECT id, zaaktype_configuration_id FROM zaaktype_completion_parameters"
                ) shouldBe listOf(listOf("1", "1"))
                migrationTestDatabase.query(
                    "SELECT zaaktype_configuration_id, enabled FROM zaaktype_cmmn_email_parameters"
                ) shouldBe listOf(listOf("1", "t"))
                migrationTestDatabase.query(
                    "SELECT id, id_planitem_definition FROM zaaktype_cmmn_humantask_parameters"
                ) shouldBe listOf(listOf("1", "fakePlanItem"))
            }

            then("of each duplicate the newest row stays") {
                migrationTestDatabase.query(
                    "SELECT id, zoekwaarde FROM zaaktype_brp_parameters"
                ) shouldBe listOf(listOf("2", "fakeNewZoekwaarde"))
                migrationTestDatabase.query(
                    "SELECT id, bpmn_process_definition_key FROM zaaktype_bpmn_configuration"
                ) shouldBe listOf(listOf("2", "fakeDuplicateProcessDefinitionKey"))
            }

            then("every row that blocked a constraint is in the quarantine table with its complete data") {
                migrationTestDatabase.query(
                    """
                    SELECT migration, source_table, reason, row_data->>'id'
                    FROM zaaktype_configuration_migration_quarantine
                    """.trimIndent()
                ) shouldContainExactlyInAnyOrder listOf(
                    listOf("V100", "zaaktype_completion_parameters", CONFIGURATION_WITHOUT_TYPE_REASON, "2"),
                    listOf("V100", "zaaktype_brp_parameters", CONFIGURATION_WITHOUT_TYPE_REASON, "3"),
                    listOf(
                        "V100",
                        "zaaktype_configuration",
                        "configuration type is missing and the row is in neither or both subclass tables",
                        "3"
                    ),
                    listOf("V100", "humantask_referentie_tabel", ORPHANED_CMMN_CHILD_REASON, null),
                    listOf("V100", "zaaktype_cmmn_humantask_parameters", ORPHANED_CMMN_CHILD_REASON, "2"),
                    listOf("V100", "zaaktype_cmmn_zaakafzender_parameters", ORPHANED_CMMN_CHILD_REASON, "1"),
                    listOf(
                        "V100",
                        "zaaktype_cmmn_configuration",
                        "CMMN configuration without zaaktype configuration",
                        "50"
                    ),
                    listOf(
                        "V100",
                        "zaaktype_bpmn_configuration",
                        "BPMN configuration without zaaktype configuration",
                        "60"
                    ),
                    listOf("V100", "zaaktype_bpmn_configuration", "duplicate BPMN configuration id", "2"),
                    listOf(
                        "V100",
                        "zaaktype_brp_parameters",
                        "duplicate BRP parameters for one zaaktype configuration",
                        "1"
                    )
                )
            }

            then("a quarantined row can be restored from its row data") {
                migrationTestDatabase.query(
                    """
                    SELECT restored.id, restored.zaaktype_configuration_id, restored.zoekwaarde
                    FROM zaaktype_configuration_migration_quarantine quarantine,
                        jsonb_populate_record(NULL::zaaktype_brp_parameters, quarantine.row_data) restored
                    WHERE quarantine.reason = 'duplicate BRP parameters for one zaaktype configuration'
                    """.trimIndent()
                ) shouldBe listOf(listOf("1", "1", "fakeOldZoekwaarde"))
            }
        }
    }
})
