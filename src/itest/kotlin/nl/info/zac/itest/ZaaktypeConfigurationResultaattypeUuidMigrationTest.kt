/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.util.MigrationTestDatabase

class ZaaktypeConfigurationResultaattypeUuidMigrationTest : BehaviorSpec({
    val migrationTestDatabase = MigrationTestDatabase()

    afterSpec { migrationTestDatabase.close() }

    given(
        """a database at version 103 with a configuration whose resultaattype references hold their omschrijving, and
            a configuration whose niet-ontvankelijk reference and one zaakbeeindig parameter hold only a UUID"""
    ) {
        migrationTestDatabase.migrateTo("103")
        migrationTestDatabase.execute(
            """
            INSERT INTO zaaktype_configuration
                (id, zaaktype_uuid, groep_id, zaaktype_omschrijving, creatiedatum,
                niet_ontvankelijk_resultaattype_uuid, niet_ontvankelijk_resultaattype_omschrijving)
            VALUES
                (1, 'a0000000-0000-0000-0000-000000000001', 'fakeGroup', 'fakeResolved', NOW(),
                'b0000000-0000-0000-0000-000000000001', 'fakeNietOntvankelijk'),
                (2, 'a0000000-0000-0000-0000-000000000002', 'fakeGroup', 'fakeUnresolved', NOW(),
                'b0000000-0000-0000-0000-000000000002', NULL);
            INSERT INTO zaaktype_completion_parameters
                (id, zaaktype_configuration_id, id_zaakbeeindigreden, resultaattype_uuid, resultaattype_omschrijving)
            VALUES
                (1, 1, (SELECT MIN(id_zaakbeeindigreden) FROM zaakbeeindigreden),
                'c0000000-0000-0000-0000-000000000001', 'fakeToegekend'),
                (2, 2, (SELECT MIN(id_zaakbeeindigreden) FROM zaakbeeindigreden),
                'c0000000-0000-0000-0000-000000000002', 'fakeAfgebroken'),
                (3, 2, (SELECT MAX(id_zaakbeeindigreden) FROM zaakbeeindigreden),
                'c0000000-0000-0000-0000-000000000003', NULL);
            """.trimIndent()
        )

        `when`("the database is migrated to version 104") {
            val migrationOutput = migrationTestDatabase.migrateTo("104")

            then("the resolved references keep their omschrijving") {
                migrationTestDatabase.query(
                    "SELECT id, niet_ontvankelijk_resultaattype_omschrijving FROM zaaktype_configuration ORDER BY id"
                ) shouldBe listOf(listOf("1", "fakeNietOntvankelijk"), listOf("2", null))
                migrationTestDatabase.query(
                    "SELECT id, resultaattype_omschrijving FROM zaaktype_completion_parameters ORDER BY id"
                ) shouldBe listOf(listOf("1", "fakeToegekend"), listOf("2", "fakeAfgebroken"))
            }

            then("the resultaattype UUID columns are gone") {
                migrationTestDatabase.query(
                    """
                    SELECT table_name, column_name FROM information_schema.columns
                    WHERE column_name IN ('niet_ontvankelijk_resultaattype_uuid', 'resultaattype_uuid')
                    """.trimIndent()
                ) shouldBe emptyList()
            }

            then(
                """the zaakbeeindig parameter without an omschrijving is moved to the quarantine table, and the
                    configuration without a niet-ontvankelijk omschrijving is copied there, both with their UUID"""
            ) {
                migrationTestDatabase.query(
                    """
                    SELECT migration, source_table, row_data->>'id', row_data->>'resultaattype_uuid',
                        row_data->>'niet_ontvankelijk_resultaattype_uuid'
                    FROM zaaktype_configuration_migration_quarantine ORDER BY source_table
                    """.trimIndent()
                ) shouldBe listOf(
                    listOf("V104", "zaaktype_completion_parameters", "3", "c0000000-0000-0000-0000-000000000003", null),
                    listOf("V104", "zaaktype_configuration", "2", null, "b0000000-0000-0000-0000-000000000002")
                )
            }

            then("the migration reports both counts as warnings") {
                migrationOutput.contains(
                    "V104: moved 1 row(s) of zaaktype_completion_parameters to zaaktype_configuration_migration_quarantine"
                ) shouldBe true
                migrationOutput.contains(
                    "V104: copied 1 row(s) of zaaktype_configuration to zaaktype_configuration_migration_quarantine"
                ) shouldBe true
            }

            then("a zaakbeeindig parameter without an omschrijving can no longer be stored") {
                migrationTestDatabase.query(
                    """
                    SELECT is_nullable FROM information_schema.columns
                    WHERE table_name = 'zaaktype_completion_parameters' AND column_name = 'resultaattype_omschrijving'
                    """.trimIndent()
                ) shouldBe listOf(listOf("NO"))
            }
        }
    }
})
