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

    given(
        """a database at version 104 with a zaakbeeindig parameter and a configuration with a behandelaar in the
            quarantine table"""
    ) {
        val migrationTestDatabase = MigrationTestDatabase().also { migrationTestDatabases.add(it) }
        migrationTestDatabase.migrateTo("103")
        migrationTestDatabase.execute(
            """
            INSERT INTO zaaktype_configuration
                (id, zaaktype_uuid, groep_id, zaaktype_omschrijving, creatiedatum, gebruikersnaam_behandelaar,
                niet_ontvankelijk_resultaattype_uuid, niet_ontvankelijk_resultaattype_omschrijving)
            VALUES
                (1, 'a0000000-0000-0000-0000-000000000001', 'fakeGroup', 'fakeOmschrijving', NOW(),
                'fakeBehandelaar', 'b0000000-0000-0000-0000-000000000001', NULL);
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

            then("the migration logs every quarantined row with its row data as a warning") {
                migrationOutput shouldContain
                    "V105: zaaktype_configuration_migration_quarantine row 1: migration V104, " +
                    "source table zaaktype_completion_parameters, reason resultaattype without omschrijving"
                migrationOutput shouldContain "\"resultaattype_uuid\": \"c0000000-0000-0000-0000-000000000001\""
                migrationOutput shouldContain
                    "V105: zaaktype_configuration_migration_quarantine row 2: migration V104, " +
                    "source table zaaktype_configuration, reason niet-ontvankelijk resultaattype without omschrijving"
                migrationOutput shouldContain
                    "\"niet_ontvankelijk_resultaattype_uuid\": \"b0000000-0000-0000-0000-000000000001\""
            }

            then("the migration logs the username of the behandelaar only as redacted") {
                migrationOutput shouldContain "\"gebruikersnaam_behandelaar\": \"[REDACTED]\""
                migrationOutput shouldNotContain "fakeBehandelaar"
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
