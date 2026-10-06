/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Rows that block a constraint added below are moved here, never deleted without a copy.
-- ZAC never reads this table. It is checked and dropped by hand after the release, so no later migration may
-- depend on its existence.
CREATE TABLE IF NOT EXISTS ${schema}.zaaktype_configuration_migration_quarantine (
    id             BIGSERIAL PRIMARY KEY,
    migration      VARCHAR NOT NULL,
    source_table   VARCHAR NOT NULL,
    reason         VARCHAR NOT NULL,
    row_data       JSONB NOT NULL,
    quarantined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE FUNCTION pg_temp.quarantine(source_table TEXT, reason TEXT, row_condition TEXT) RETURNS VOID AS $$
DECLARE
    moved_row_count BIGINT;
BEGIN
    EXECUTE format(
        'WITH moved AS (DELETE FROM ${schema}.%I t WHERE %s RETURNING t.*) '
        'INSERT INTO ${schema}.zaaktype_configuration_migration_quarantine (migration, source_table, reason, row_data) '
        'SELECT %L, %L, %L, to_jsonb(moved) FROM moved',
        source_table, row_condition, 'V100', source_table, reason
    );
    GET DIAGNOSTICS moved_row_count = ROW_COUNT;
    IF moved_row_count > 0 THEN
        RAISE WARNING 'V100: moved % row(s) of % to zaaktype_configuration_migration_quarantine: %',
            moved_row_count, source_table, reason;
    END IF;
END $$ LANGUAGE plpgsql;

-- A missing discriminator is derived from the one subclass table that holds the configuration.
UPDATE ${schema}.zaaktype_configuration base
    SET configuration_type = 'CMMN'
    WHERE base.configuration_type IS NULL
    AND EXISTS (SELECT 1 FROM ${schema}.zaaktype_cmmn_configuration cmmn WHERE cmmn.id = base.id)
    AND NOT EXISTS (SELECT 1 FROM ${schema}.zaaktype_bpmn_configuration bpmn WHERE bpmn.id = base.id);

UPDATE ${schema}.zaaktype_configuration base
    SET configuration_type = 'BPMN'
    WHERE base.configuration_type IS NULL
    AND EXISTS (SELECT 1 FROM ${schema}.zaaktype_bpmn_configuration bpmn WHERE bpmn.id = base.id)
    AND NOT EXISTS (SELECT 1 FROM ${schema}.zaaktype_cmmn_configuration cmmn WHERE cmmn.id = base.id);

-- A base row whose type still cannot be derived is moved together with its child rows. The child rows go first,
-- because their foreign keys would otherwise cascade the delete without a copy.
DO $$
DECLARE
    untyped_configuration TEXT := 'SELECT id FROM ${schema}.zaaktype_configuration WHERE configuration_type IS NULL';
BEGIN
    -- A template can belong to a template group of another configuration, and the group's foreign key cascades.
    PERFORM pg_temp.quarantine(
        'zaaktype_smartdocuments_document_template_parameters',
        'child of a zaaktype configuration without configuration type',
        format(
            't.zaaktype_configuration_id IN (%1$s) OR t.sjabloon_groep_id IN ('
            'SELECT id FROM ${schema}.zaaktype_smartdocuments_document_template_group_parameters '
            'WHERE zaaktype_configuration_id IN (%1$s))',
            untyped_configuration
        )
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_smartdocuments_document_template_group_parameters',
        'child of a zaaktype configuration without configuration type',
        format('t.zaaktype_configuration_id IN (%s)', untyped_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_completion_parameters',
        'child of a zaaktype configuration without configuration type',
        format('t.zaaktype_configuration_id IN (%s)', untyped_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_betrokkene_parameters',
        'child of a zaaktype configuration without configuration type',
        format('t.zaaktype_configuration_id IN (%s)', untyped_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_brp_parameters',
        'child of a zaaktype configuration without configuration type',
        format('t.zaaktype_configuration_id IN (%s)', untyped_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_configuration',
        'configuration type is missing and the row is in neither or both subclass tables',
        't.configuration_type IS NULL'
    );
END $$;

-- Subclass rows without a base row block the new foreign keys. The CMMN child rows go first, for the same reason
-- as above and because the zaakafzender foreign key restricts the delete.
DO $$
DECLARE
    orphaned_cmmn_configuration TEXT := 'SELECT cmmn.id FROM ${schema}.zaaktype_cmmn_configuration cmmn '
        'WHERE NOT EXISTS (SELECT 1 FROM ${schema}.zaaktype_configuration base WHERE base.id = cmmn.id)';
BEGIN
    PERFORM pg_temp.quarantine(
        'humantask_referentie_tabel',
        'child of a CMMN configuration without zaaktype configuration',
        format(
            't.id_humantask_parameters IN (SELECT id FROM ${schema}.zaaktype_cmmn_humantask_parameters '
            'WHERE zaaktype_configuration_id IN (%s))',
            orphaned_cmmn_configuration
        )
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_humantask_parameters',
        'child of a CMMN configuration without zaaktype configuration',
        format('t.zaaktype_configuration_id IN (%s)', orphaned_cmmn_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_usereventlistener_parameters',
        'child of a CMMN configuration without zaaktype configuration',
        format('t.zaaktype_configuration_id IN (%s)', orphaned_cmmn_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_mailtemplate_parameters',
        'child of a CMMN configuration without zaaktype configuration',
        format('t.zaaktype_configuration_id IN (%s)', orphaned_cmmn_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_zaakafzender_parameters',
        'child of a CMMN configuration without zaaktype configuration',
        format('t.zaaktype_configuration_id IN (%s)', orphaned_cmmn_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_email_parameters',
        'child of a CMMN configuration without zaaktype configuration',
        format('t.zaaktype_configuration_id IN (%s)', orphaned_cmmn_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_configuration',
        'CMMN configuration without zaaktype configuration',
        format('t.id IN (%s)', orphaned_cmmn_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_bpmn_configuration',
        'BPMN configuration without zaaktype configuration',
        'NOT EXISTS (SELECT 1 FROM ${schema}.zaaktype_configuration base WHERE base.id = t.id)'
    );
END $$;

-- Duplicate ids block the new BPMN primary key, and duplicate one-to-one children block the new unique
-- constraints. The physically last BPMN row and the child row with the highest id stay. The id and
-- zaaktype_configuration_id columns of the one-to-one child tables are NOT NULL, so every group is one configuration.
DO $$
BEGIN
    PERFORM pg_temp.quarantine(
        'zaaktype_bpmn_configuration',
        'duplicate BPMN configuration id',
        't.ctid NOT IN (SELECT MAX(ctid) FROM ${schema}.zaaktype_bpmn_configuration GROUP BY id)'
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_betrokkene_parameters',
        'duplicate betrokkene parameters for one zaaktype configuration',
        't.id NOT IN (SELECT MAX(id) FROM ${schema}.zaaktype_betrokkene_parameters GROUP BY zaaktype_configuration_id)'
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_brp_parameters',
        'duplicate BRP parameters for one zaaktype configuration',
        't.id NOT IN (SELECT MAX(id) FROM ${schema}.zaaktype_brp_parameters GROUP BY zaaktype_configuration_id)'
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_email_parameters',
        'duplicate email parameters for one CMMN configuration',
        't.id NOT IN (SELECT MAX(id) FROM ${schema}.zaaktype_cmmn_email_parameters GROUP BY zaaktype_configuration_id)'
    );
END $$;

DROP FUNCTION pg_temp.quarantine(TEXT, TEXT, TEXT);

ALTER TABLE ${schema}.zaaktype_configuration
    ALTER COLUMN configuration_type SET NOT NULL;

ALTER TABLE ${schema}.zaaktype_bpmn_configuration
    ADD CONSTRAINT pk_zaaktype_bpmn_configuration PRIMARY KEY (id);

ALTER TABLE ${schema}.zaaktype_cmmn_configuration
    ADD CONSTRAINT fk_zaaktype_cmmn_configuration_zaaktype_configuration FOREIGN KEY (id)
        REFERENCES ${schema}.zaaktype_configuration (id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ${schema}.zaaktype_bpmn_configuration
    ADD CONSTRAINT fk_zaaktype_bpmn_configuration_zaaktype_configuration FOREIGN KEY (id)
        REFERENCES ${schema}.zaaktype_configuration (id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ${schema}.zaaktype_betrokkene_parameters
    ADD CONSTRAINT un_zaaktype_betrokkene_parameters_zaaktype_configuration UNIQUE (zaaktype_configuration_id);

ALTER TABLE ${schema}.zaaktype_brp_parameters
    ADD CONSTRAINT un_zaaktype_brp_parameters_zaaktype_configuration UNIQUE (zaaktype_configuration_id);

ALTER TABLE ${schema}.zaaktype_cmmn_email_parameters
    ADD CONSTRAINT un_zaaktype_cmmn_email_parameters_zaaktype_configuration UNIQUE (zaaktype_configuration_id);
