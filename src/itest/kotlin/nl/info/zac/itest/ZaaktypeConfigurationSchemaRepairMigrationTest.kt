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

private const val UNTYPED_CONFIGURATION_CHILD = "child of a zaaktype configuration without configuration type"
private const val UNTYPED_CONFIGURATION = "configuration type is missing and the row is in neither or both subclass tables"
private const val ORPHANED_CMMN_CONFIGURATION_CHILD = "child of a CMMN configuration without zaaktype configuration"
private const val ORPHANED_CMMN_CONFIGURATION = "CMMN configuration without zaaktype configuration"
private const val ORPHANED_BPMN_CONFIGURATION = "BPMN configuration without zaaktype configuration"
private const val DUPLICATE_BPMN_CONFIGURATION = "duplicate BPMN configuration id"
private const val DUPLICATE_BETROKKENE_PARAMETERS = "duplicate betrokkene parameters for one zaaktype configuration"
private const val DUPLICATE_BRP_PARAMETERS = "duplicate BRP parameters for one zaaktype configuration"
private const val DUPLICATE_EMAIL_PARAMETERS = "duplicate email parameters for one CMMN configuration"

class ZaaktypeConfigurationSchemaRepairMigrationTest : BehaviorSpec({
    val migrationTestDatabase = MigrationTestDatabase()

    afterSpec { migrationTestDatabase.close() }

    fun rowAsJson(table: String, condition: String) =
        migrationTestDatabase.query("SELECT to_jsonb(t)::text FROM $table t WHERE $condition").single().single()

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
                (1, 'a0000000-0000-0000-0000-000000000001', 'fakeGroup', 'fakeCmmn', NOW(), 'CMMN'),
                (2, 'a0000000-0000-0000-0000-000000000002', 'fakeGroup', 'fakeBpmn', NOW(), 'BPMN'),
                (3, 'a0000000-0000-0000-0000-000000000003', 'fakeGroup', 'fakeUntypedWithoutSubclass', NOW(), NULL),
                (4, 'a0000000-0000-0000-0000-000000000004', 'fakeGroup', 'fakeUntypedCmmn', NOW(), NULL),
                (5, 'a0000000-0000-0000-0000-000000000005', 'fakeGroup', 'fakeUntypedInBothSubclasses', NOW(), NULL),
                (6, 'a0000000-0000-0000-0000-000000000006', 'fakeGroup', 'fakeUntypedBpmn', NOW(), NULL);
            INSERT INTO zaaktype_cmmn_configuration (id, id_case_definition, intake_mail, afronden_mail)
            VALUES
                (1, 'fakeCaseDefinition', 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT'),
                (4, 'fakeCaseDefinition4', 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT'),
                (5, 'fakeCaseDefinition5', 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT'),
                (50, 'fakeOrphanedCaseDefinition', 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT');
            INSERT INTO zaaktype_bpmn_configuration
            VALUES
                (2, 'fakeProcessDefinitionKey'),
                (2, 'fakeDuplicateProcessDefinitionKey'),
                (5, 'fakeAmbiguousProcessDefinitionKey'),
                (6, 'fakeProcessDefinitionKey6'),
                (60, 'fakeOrphanedProcessDefinitionKey');
            INSERT INTO zaaktype_betrokkene_parameters (id, zaaktype_configuration_id, brpkoppelen)
            VALUES (1, 1, FALSE), (2, 1, TRUE), (3, 3, TRUE);
            INSERT INTO zaaktype_brp_parameters (id, zaaktype_configuration_id, zoekwaarde)
            VALUES (1, 1, 'fakeOldZoekwaarde'), (2, 1, 'fakeNewZoekwaarde'), (3, 3, 'fakeUntypedZoekwaarde');
            INSERT INTO zaaktype_completion_parameters
            VALUES
                (1, 1, 900, 'b0000000-0000-0000-0000-000000000001'),
                (2, 3, 900, 'b0000000-0000-0000-0000-000000000002');
            INSERT INTO zaaktype_cmmn_email_parameters (id, zaaktype_configuration_id, enabled, template_name)
            VALUES (1, 1, FALSE, 'fakeOldTemplate'), (2, 1, TRUE, 'fakeNewTemplate'), (3, 5, TRUE, 'fakeAmbiguousTemplate');
            INSERT INTO zaaktype_cmmn_humantask_parameters (id, zaaktype_configuration_id, id_planitem_definition)
            VALUES (1, 1, 'fakePlanItem'), (2, 50, 'fakeOrphanedPlanItem'), (3, 5, 'fakeAmbiguousPlanItem');
            INSERT INTO humantask_referentie_tabel
            VALUES (1, (SELECT MIN(id_referentie_tabel) FROM referentie_tabel), 2, 'fakeVeld');
            INSERT INTO zaaktype_cmmn_usereventlistener_parameters (id, zaaktype_configuration_id, id_planitem_definition)
            VALUES (1, 1, 'fakePlanItem'), (2, 50, 'fakeOrphanedPlanItem');
            INSERT INTO zaaktype_cmmn_mailtemplate_parameters (id, zaaktype_configuration_id, id_mail_template)
            VALUES
                (1, 1, (SELECT MIN(id_mail_template) FROM mail_template)),
                (2, 50, (SELECT MIN(id_mail_template) FROM mail_template));
            INSERT INTO zaaktype_cmmn_zaakafzender_parameters (id, zaaktype_configuration_id, mail)
            VALUES (1, 50, 'orphaned@example.com');
            INSERT INTO zaaktype_smartdocuments_document_template_group_parameters
                (id, smartdocuments_id, aanmaakdatum, zaaktype_configuration_id)
            VALUES (1, 'fakeUntypedGroup', NOW(), 3), (2, 'fakeValidGroup', NOW(), 1);
            INSERT INTO zaaktype_smartdocuments_document_template_parameters
                (id, smartdocuments_id, aanmaakdatum, sjabloon_groep_id, zaaktype_configuration_id,
                informatie_object_type_uuid)
            VALUES
                (1, 'fakeTemplateInUntypedGroup', NOW(), 1, 1, 'c0000000-0000-0000-0000-000000000001'),
                (2, 'fakeValidTemplate', NOW(), 2, 1, 'c0000000-0000-0000-0000-000000000002');
            """.trimIndent()
        )
        val expectedQuarantine = listOf(
            listOf(
                "zaaktype_smartdocuments_document_template_parameters",
                UNTYPED_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_smartdocuments_document_template_parameters", "id = 1")
            ),
            listOf(
                "zaaktype_smartdocuments_document_template_group_parameters",
                UNTYPED_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_smartdocuments_document_template_group_parameters", "id = 1")
            ),
            listOf(
                "zaaktype_completion_parameters",
                UNTYPED_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_completion_parameters", "id = 2")
            ),
            listOf(
                "zaaktype_betrokkene_parameters",
                UNTYPED_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_betrokkene_parameters", "id = 3")
            ),
            listOf("zaaktype_brp_parameters", UNTYPED_CONFIGURATION_CHILD, rowAsJson("zaaktype_brp_parameters", "id = 3")),
            listOf("zaaktype_configuration", UNTYPED_CONFIGURATION, rowAsJson("zaaktype_configuration", "id = 3")),
            listOf("zaaktype_configuration", UNTYPED_CONFIGURATION, rowAsJson("zaaktype_configuration", "id = 5")),
            listOf(
                "humantask_referentie_tabel",
                ORPHANED_CMMN_CONFIGURATION_CHILD,
                rowAsJson("humantask_referentie_tabel", "id_humantask_referentie_tabel = 1")
            ),
            listOf(
                "zaaktype_cmmn_humantask_parameters",
                ORPHANED_CMMN_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_cmmn_humantask_parameters", "id = 2")
            ),
            listOf(
                "zaaktype_cmmn_humantask_parameters",
                ORPHANED_CMMN_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_cmmn_humantask_parameters", "id = 3")
            ),
            listOf(
                "zaaktype_cmmn_usereventlistener_parameters",
                ORPHANED_CMMN_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_cmmn_usereventlistener_parameters", "id = 2")
            ),
            listOf(
                "zaaktype_cmmn_mailtemplate_parameters",
                ORPHANED_CMMN_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_cmmn_mailtemplate_parameters", "id = 2")
            ),
            listOf(
                "zaaktype_cmmn_zaakafzender_parameters",
                ORPHANED_CMMN_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_cmmn_zaakafzender_parameters", "id = 1")
            ),
            listOf(
                "zaaktype_cmmn_email_parameters",
                ORPHANED_CMMN_CONFIGURATION_CHILD,
                rowAsJson("zaaktype_cmmn_email_parameters", "id = 3")
            ),
            listOf(
                "zaaktype_cmmn_configuration",
                ORPHANED_CMMN_CONFIGURATION,
                rowAsJson("zaaktype_cmmn_configuration", "id = 5")
            ),
            listOf(
                "zaaktype_cmmn_configuration",
                ORPHANED_CMMN_CONFIGURATION,
                rowAsJson("zaaktype_cmmn_configuration", "id = 50")
            ),
            listOf(
                "zaaktype_bpmn_configuration",
                ORPHANED_BPMN_CONFIGURATION,
                rowAsJson("zaaktype_bpmn_configuration", "id = 5")
            ),
            listOf(
                "zaaktype_bpmn_configuration",
                ORPHANED_BPMN_CONFIGURATION,
                rowAsJson("zaaktype_bpmn_configuration", "id = 60")
            ),
            listOf(
                "zaaktype_bpmn_configuration",
                DUPLICATE_BPMN_CONFIGURATION,
                rowAsJson("zaaktype_bpmn_configuration", "bpmn_process_definition_key = 'fakeProcessDefinitionKey'")
            ),
            listOf(
                "zaaktype_betrokkene_parameters",
                DUPLICATE_BETROKKENE_PARAMETERS,
                rowAsJson("zaaktype_betrokkene_parameters", "id = 1")
            ),
            listOf("zaaktype_brp_parameters", DUPLICATE_BRP_PARAMETERS, rowAsJson("zaaktype_brp_parameters", "id = 1")),
            listOf(
                "zaaktype_cmmn_email_parameters",
                DUPLICATE_EMAIL_PARAMETERS,
                rowAsJson("zaaktype_cmmn_email_parameters", "id = 1")
            )
        )

        `when`("the database is migrated to version 100") {
            val flywayOutput = migrationTestDatabase.migrateTo("100")

            then("the valid configurations and their child rows are unchanged") {
                migrationTestDatabase.query(
                    "SELECT id, configuration_type FROM zaaktype_configuration ORDER BY id"
                ) shouldBe listOf(listOf("1", "CMMN"), listOf("2", "BPMN"), listOf("4", "CMMN"), listOf("6", "BPMN"))
                migrationTestDatabase.query(
                    "SELECT id, id_case_definition FROM zaaktype_cmmn_configuration ORDER BY id"
                ) shouldBe listOf(listOf("1", "fakeCaseDefinition"), listOf("4", "fakeCaseDefinition4"))
                migrationTestDatabase.query(
                    "SELECT id, bpmn_process_definition_key FROM zaaktype_bpmn_configuration WHERE id = 6"
                ) shouldBe listOf(listOf("6", "fakeProcessDefinitionKey6"))
                migrationTestDatabase.query(
                    "SELECT id, zaaktype_configuration_id FROM zaaktype_completion_parameters"
                ) shouldBe listOf(listOf("1", "1"))
                migrationTestDatabase.query(
                    "SELECT id, id_planitem_definition FROM zaaktype_cmmn_humantask_parameters"
                ) shouldBe listOf(listOf("1", "fakePlanItem"))
                migrationTestDatabase.query(
                    "SELECT id, id_planitem_definition FROM zaaktype_cmmn_usereventlistener_parameters"
                ) shouldBe listOf(listOf("1", "fakePlanItem"))
                migrationTestDatabase.query(
                    "SELECT id, zaaktype_configuration_id FROM zaaktype_cmmn_mailtemplate_parameters"
                ) shouldBe listOf(listOf("1", "1"))
                migrationTestDatabase.query(
                    "SELECT id, smartdocuments_id FROM zaaktype_smartdocuments_document_template_parameters"
                ) shouldBe listOf(listOf("2", "fakeValidTemplate"))
            }

            then("of each duplicate the newest row stays") {
                migrationTestDatabase.query(
                    "SELECT id, brpkoppelen FROM zaaktype_betrokkene_parameters"
                ) shouldBe listOf(listOf("2", "t"))
                migrationTestDatabase.query(
                    "SELECT id, zoekwaarde FROM zaaktype_brp_parameters"
                ) shouldBe listOf(listOf("2", "fakeNewZoekwaarde"))
                migrationTestDatabase.query(
                    "SELECT id, template_name FROM zaaktype_cmmn_email_parameters"
                ) shouldBe listOf(listOf("2", "fakeNewTemplate"))
                migrationTestDatabase.query(
                    "SELECT id, bpmn_process_definition_key FROM zaaktype_bpmn_configuration WHERE id = 2"
                ) shouldBe listOf(listOf("2", "fakeDuplicateProcessDefinitionKey"))
            }

            then("every row that blocked a constraint is in the quarantine table with exactly its original data") {
                migrationTestDatabase.query(
                    """
                    SELECT source_table, reason, row_data::text
                    FROM zaaktype_configuration_migration_quarantine
                    WHERE migration = 'V100'
                    """.trimIndent()
                ) shouldContainExactlyInAnyOrder expectedQuarantine
            }

            then("a quarantined row can be restored from its row data") {
                migrationTestDatabase.query(
                    """
                    SELECT to_jsonb(restored)::text
                    FROM zaaktype_configuration_migration_quarantine quarantine,
                        jsonb_populate_record(NULL::zaaktype_brp_parameters, quarantine.row_data) restored
                    WHERE quarantine.reason = '$DUPLICATE_BRP_PARAMETERS'
                    """.trimIndent()
                ) shouldBe listOf(
                    expectedQuarantine.single { it[1] == DUPLICATE_BRP_PARAMETERS }.drop(2)
                )
            }

            then("the migration logs a warning for every reason with the number of quarantined rows") {
                flywayOutput shouldContain
                    "V100: moved 2 row(s) of zaaktype_cmmn_humantask_parameters to " +
                    "zaaktype_configuration_migration_quarantine: $ORPHANED_CMMN_CONFIGURATION_CHILD"
                flywayOutput shouldContain
                    "V100: moved 2 row(s) of zaaktype_configuration to " +
                    "zaaktype_configuration_migration_quarantine: $UNTYPED_CONFIGURATION"
            }
        }
    }
})
