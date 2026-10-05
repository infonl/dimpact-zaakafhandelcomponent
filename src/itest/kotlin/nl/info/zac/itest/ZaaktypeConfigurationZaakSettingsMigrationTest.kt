/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.util.MigrationTestDatabase

class ZaaktypeConfigurationZaakSettingsMigrationTest : BehaviorSpec({
    val migrationTestDatabase = MigrationTestDatabase()

    afterSpec { migrationTestDatabase.close() }

    given(
        """a database at version 100 with a CMMN configuration that has deadline warning windows, a confirmation
            email, a zaakafzender and a mailtemplate koppeling, and a BPMN configuration"""
    ) {
        migrationTestDatabase.migrateTo("100")
        migrationTestDatabase.execute(
            """
            INSERT INTO zaaktype_configuration
                (id, zaaktype_uuid, groep_id, zaaktype_omschrijving, creatiedatum, configuration_type)
            VALUES
                (1, 'a0000000-0000-0000-0000-000000000001', 'fakeGroup', 'fakeCmmn', NOW(), 'CMMN'),
                (2, 'a0000000-0000-0000-0000-000000000002', 'fakeGroup', 'fakeBpmn', NOW(), 'BPMN');
            INSERT INTO zaaktype_cmmn_configuration
                (id, id_case_definition, eindatum_gepland_waarschuwing, uiterlijke_einddatum_afdoening_waarschuwing,
                intake_mail, afronden_mail)
            VALUES (1, 'fakeCaseDefinition', 3, 2, 'BESCHIKBAAR_UIT', 'BESCHIKBAAR_UIT');
            INSERT INTO zaaktype_bpmn_configuration VALUES (2, 'fakeProcessDefinitionKey');
            INSERT INTO zaaktype_cmmn_email_parameters (id, zaaktype_configuration_id, enabled, template_name)
            VALUES (1, 1, TRUE, 'fakeTemplate');
            INSERT INTO zaaktype_cmmn_zaakafzender_parameters (id, zaaktype_configuration_id, mail)
            VALUES (1, 1, 'afzender@example.com');
            INSERT INTO zaaktype_cmmn_mailtemplate_parameters (id, id_mail_template, zaaktype_configuration_id)
            VALUES (1, (SELECT MIN(id_mail_template) FROM mail_template), 1);
            """.trimIndent()
        )
        val emailParametersBefore = migrationTestDatabase.query(
            "SELECT to_jsonb(t)::text FROM zaaktype_cmmn_email_parameters t"
        )
        val zaakafzenderParametersBefore = migrationTestDatabase.query(
            "SELECT to_jsonb(t)::text FROM zaaktype_cmmn_zaakafzender_parameters t"
        )
        val mailtemplateParametersBefore = migrationTestDatabase.query(
            "SELECT to_jsonb(t)::text FROM zaaktype_cmmn_mailtemplate_parameters t"
        )

        `when`("the database is migrated to version 101") {
            migrationTestDatabase.migrateTo("101")

            then("the deadline warning windows are on the zaaktype configuration") {
                migrationTestDatabase.query(
                    """
                    SELECT id, eindatum_gepland_waarschuwing, uiterlijke_einddatum_afdoening_waarschuwing
                    FROM zaaktype_configuration ORDER BY id
                    """.trimIndent()
                ) shouldBe listOf(listOf("1", "3", "2"), listOf("2", null, null))
            }

            then("the email, zaakafzender and mailtemplate rows are unchanged in their renamed tables") {
                migrationTestDatabase.query(
                    "SELECT to_jsonb(t)::text FROM zaaktype_email_parameters t"
                ) shouldBe emailParametersBefore
                migrationTestDatabase.query(
                    "SELECT to_jsonb(t)::text FROM zaaktype_zaakafzender_parameters t"
                ) shouldBe zaakafzenderParametersBefore
                migrationTestDatabase.query(
                    "SELECT to_jsonb(t)::text FROM zaaktype_mailtemplate_parameters t"
                ) shouldBe mailtemplateParametersBefore
            }

            then("the renamed tables reference the zaaktype configuration, so a BPMN configuration can have them") {
                migrationTestDatabase.execute(
                    """
                    INSERT INTO zaaktype_zaakafzender_parameters (id, zaaktype_configuration_id, mail)
                    VALUES (2, 2, 'bpmn-afzender@example.com');
                    """.trimIndent()
                )
                migrationTestDatabase.query(
                    "SELECT zaaktype_configuration_id, mail FROM zaaktype_zaakafzender_parameters WHERE id = 2"
                ) shouldBe listOf(listOf("2", "bpmn-afzender@example.com"))
            }

            then("nothing is quarantined") {
                migrationTestDatabase.query(
                    "SELECT COUNT(*) FROM zaaktype_configuration_migration_quarantine"
                ) shouldBe listOf(listOf("0"))
            }
        }
    }
})
