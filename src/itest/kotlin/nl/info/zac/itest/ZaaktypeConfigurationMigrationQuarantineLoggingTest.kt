/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import nl.info.zac.itest.util.MigrationTestDatabase

class ZaaktypeConfigurationMigrationQuarantineLoggingTest : BehaviorSpec({
    val migrationTestDatabases = mutableListOf<MigrationTestDatabase>()

    afterSpec { migrationTestDatabases.forEach(MigrationTestDatabase::close) }

    given("a database at version 104 with one zaakbeeindig parameter in the quarantine table") {
        val migrationTestDatabase = MigrationTestDatabase().also { migrationTestDatabases.add(it) }
        migrationTestDatabase.migrateTo("103")
        migrationTestDatabase.execute(
            """
            INSERT INTO zaaktype_configuration (id, zaaktype_uuid, groep_id, zaaktype_omschrijving, creatiedatum)
            VALUES (1, 'a0000000-0000-0000-0000-000000000001', 'fakeGroup', 'fakeOmschrijving', NOW());
            INSERT INTO zaaktype_completion_parameters
                (id, zaaktype_configuration_id, id_zaakbeeindigreden, resultaattype_uuid, resultaattype_omschrijving)
            VALUES
                (1, 1, (SELECT MIN(id_zaakbeeindigreden) FROM zaakbeeindigreden),
                'c0000000-0000-0000-0000-000000000001', NULL);
            """.trimIndent()
        )
        migrationTestDatabase.migrateTo("104")

        `when`("the database is migrated to version 105") {
            val migrationOutput = migrationTestDatabase.migrateTo("105")

            then("the migration logs the quarantined row with its full row data as a warning") {
                migrationOutput shouldContain
                    "V105: zaaktype_configuration_migration_quarantine row 1: migration V104, " +
                    "source table zaaktype_completion_parameters, reason resultaattype without omschrijving"
                migrationOutput shouldContain "\"resultaattype_uuid\": \"c0000000-0000-0000-0000-000000000001\""
            }
        }
    }

    given("a database at version 104 whose quarantine table has been dropped by hand") {
        val migrationTestDatabase = MigrationTestDatabase().also { migrationTestDatabases.add(it) }
        migrationTestDatabase.migrateTo("104")
        migrationTestDatabase.execute("DROP TABLE zaaktype_configuration_migration_quarantine")

        `when`("the database is migrated to version 105") {
            val migrationOutput = migrationTestDatabase.migrateTo("105")

            then("the migration succeeds without logging a quarantined row") {
                migrationOutput shouldNotContain "V105:"
            }
        }
    }
})
