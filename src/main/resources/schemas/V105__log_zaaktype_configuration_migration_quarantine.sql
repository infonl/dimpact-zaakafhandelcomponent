/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Flyway logs each warning in the ZAC log, so the quarantine is also there on environments that ran V100 and V104
-- before this migration existed. The table may already have been dropped by hand.
-- Email addresses and usernames must not be logged (docs/development/logging.md), so their values are redacted; the
-- quarantine table keeps the full row data.
DO $$
DECLARE
    personal_data_columns TEXT[] := ARRAY['email_sender', 'email_reply', 'mail', 'replyto', 'gebruikersnaam_behandelaar'];
    quarantined_row RECORD;
    redacted_row_data JSONB;
BEGIN
    IF to_regclass('${schema}.zaaktype_configuration_migration_quarantine') IS NULL THEN
        RETURN;
    END IF;
    FOR quarantined_row IN
        SELECT * FROM ${schema}.zaaktype_configuration_migration_quarantine ORDER BY id
    LOOP
        SELECT COALESCE(jsonb_object_agg(
            column_value.key,
            CASE
                WHEN column_value.key = ANY (personal_data_columns) AND column_value.value <> 'null'::JSONB
                THEN to_jsonb('[REDACTED]'::TEXT)
                ELSE column_value.value
            END
        ), '{}'::JSONB)
        INTO redacted_row_data
        FROM jsonb_each(quarantined_row.row_data) column_value;
        RAISE WARNING 'V105: zaaktype_configuration_migration_quarantine row %: migration %, source table %, reason %, quarantined at %, row data %',
            quarantined_row.id, quarantined_row.migration, quarantined_row.source_table, quarantined_row.reason,
            quarantined_row.quarantined_at, redacted_row_data;
    END LOOP;
END $$;
