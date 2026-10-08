/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- A resultaattype reference without an omschrijving cannot be resolved once its UUID is gone. Its UUID is kept in
-- the quarantine table, never dropped without a copy.
CREATE TABLE IF NOT EXISTS ${schema}.zaaktype_configuration_migration_quarantine (
    id             BIGSERIAL PRIMARY KEY,
    migration      VARCHAR NOT NULL,
    source_table   VARCHAR NOT NULL,
    reason         VARCHAR NOT NULL,
    row_data       JSONB NOT NULL,
    quarantined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

DO $$
DECLARE
    moved_row_count BIGINT;
BEGIN
    WITH moved AS (
        DELETE FROM ${schema}.zaaktype_completion_parameters t
        WHERE t.resultaattype_omschrijving IS NULL
        RETURNING t.*
    )
    INSERT INTO ${schema}.zaaktype_configuration_migration_quarantine (migration, source_table, reason, row_data)
    SELECT 'V104', 'zaaktype_completion_parameters', 'resultaattype without omschrijving', to_jsonb(moved) FROM moved;
    GET DIAGNOSTICS moved_row_count = ROW_COUNT;
    IF moved_row_count > 0 THEN
        RAISE WARNING 'V104: moved % row(s) of zaaktype_completion_parameters to zaaktype_configuration_migration_quarantine: resultaattype without omschrijving',
            moved_row_count;
    END IF;

    -- The configuration itself stays; it only loses its niet-ontvankelijk resultaattype.
    INSERT INTO ${schema}.zaaktype_configuration_migration_quarantine (migration, source_table, reason, row_data)
    SELECT 'V104', 'zaaktype_configuration', 'niet-ontvankelijk resultaattype without omschrijving', to_jsonb(t)
    FROM ${schema}.zaaktype_configuration t
    WHERE t.niet_ontvankelijk_resultaattype_uuid IS NOT NULL AND t.niet_ontvankelijk_resultaattype_omschrijving IS NULL;
    GET DIAGNOSTICS moved_row_count = ROW_COUNT;
    IF moved_row_count > 0 THEN
        RAISE WARNING 'V104: copied % row(s) of zaaktype_configuration to zaaktype_configuration_migration_quarantine: niet-ontvankelijk resultaattype without omschrijving',
            moved_row_count;
    END IF;
END $$;

ALTER TABLE ${schema}.zaaktype_configuration DROP COLUMN niet_ontvankelijk_resultaattype_uuid;
ALTER TABLE ${schema}.zaaktype_completion_parameters DROP COLUMN resultaattype_uuid;
ALTER TABLE ${schema}.zaaktype_completion_parameters ALTER COLUMN resultaattype_omschrijving SET NOT NULL;
