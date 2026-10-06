/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.ItestConfiguration.ZAC_DATABASE_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.dockerComposeContainer

class ZaaktypeConfigurationSchemaTest : BehaviorSpec({
    fun queryZacDatabase(sql: String): List<String> {
        val execResult = dockerComposeContainer
            .getContainerByServiceName(ZAC_DATABASE_CONTAINER_SERVICE_NAME)
            .get()
            .execInContainer("psql", "-U", "zac", "-d", "zac", "-At", "-c", sql)
        execResult.exitCode shouldBe 0
        return execResult.stdout.lines().filter { it.isNotBlank() }
    }

    given("ZAC has migrated its database") {
        `when`("the constraints of the zaaktype configuration tables are read") {
            val constraintNames = queryZacDatabase(
                """
                SELECT conname FROM pg_constraint
                WHERE connamespace = 'zaakafhandelcomponent'::regnamespace
                """.trimIndent()
            )
            val isConfigurationTypeNullable = queryZacDatabase(
                """
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = 'zaakafhandelcomponent'
                AND table_name = 'zaaktype_configuration'
                AND column_name = 'configuration_type'
                """.trimIndent()
            )

            then(
                """the BPMN configuration has a primary key, both subclass tables reference the base table,
                    the one-to-one child tables allow one row per configuration, and the configuration type is mandatory"""
            ) {
                constraintNames shouldContainAll listOf(
                    "pk_zaaktype_bpmn_configuration",
                    "fk_zaaktype_cmmn_configuration_zaaktype_configuration",
                    "fk_zaaktype_bpmn_configuration_zaaktype_configuration",
                    "un_zaaktype_betrokkene_parameters_zaaktype_configuration",
                    "un_zaaktype_brp_parameters_zaaktype_configuration",
                    "un_zaaktype_cmmn_email_parameters_zaaktype_configuration"
                )
                isConfigurationTypeNullable shouldBe listOf("NO")
            }
        }

        `when`("the quarantine table of the zaaktype configuration migrations is read") {
            val quarantinedRowCount = queryZacDatabase(
                "SELECT COUNT(*) FROM zaakafhandelcomponent.zaaktype_configuration_migration_quarantine"
            )

            then("it is empty, because the seed data holds no rows that block a constraint") {
                quarantinedRowCount shouldBe listOf("0")
            }
        }
    }
})
