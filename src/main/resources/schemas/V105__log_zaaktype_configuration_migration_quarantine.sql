/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Flyway logs each warning in the ZAC log, so the full quarantine is also there on environments that ran V100 and
-- V104 before this migration existed. The table may already have been dropped by hand.
DO $$
DECLARE
    quarantined_row RECORD;
BEGIN
    IF to_regclass('${schema}.zaaktype_configuration_migration_quarantine') IS NULL THEN
        RETURN;
    END IF;
    FOR quarantined_row IN
        SELECT * FROM ${schema}.zaaktype_configuration_migration_quarantine ORDER BY id
    LOOP
        RAISE WARNING 'V105: zaaktype_configuration_migration_quarantine row %: migration %, source table %, reason %, quarantined at %, row data %',
            quarantined_row.id, quarantined_row.migration, quarantined_row.source_table, quarantined_row.reason,
            quarantined_row.quarantined_at, quarantined_row.row_data;
    END LOOP;
END $$;
