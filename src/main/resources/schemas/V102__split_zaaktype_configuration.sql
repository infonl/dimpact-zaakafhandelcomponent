/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Replaces the CMMN/BPMN inheritance of the zaaktype configuration by one engine-agnostic configuration, a process
-- binding that names the engine and its definition key, and a CMMN extension for the CMMN plan item settings.

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
        source_table, row_condition, 'V102', source_table, reason
    );
    GET DIAGNOSTICS moved_row_count = ROW_COUNT;
    IF moved_row_count > 0 THEN
        RAISE WARNING 'V102: moved % row(s) of % to zaaktype_configuration_migration_quarantine: %',
            moved_row_count, source_table, reason;
    END IF;
END $$ LANGUAGE plpgsql;

-- The discriminator decides the engine, so Hibernate never loads a subclass row of the other engine. The CMMN child
-- rows go first, because their foreign keys would otherwise cascade the delete without a copy.
DO $$
DECLARE
    bpmn_configuration TEXT := 'SELECT id FROM ${schema}.zaaktype_configuration WHERE configuration_type = ''BPMN''';
BEGIN
    PERFORM pg_temp.quarantine(
        'humantask_referentie_tabel',
        'child of a CMMN row of a BPMN zaaktype configuration',
        format(
            't.id_humantask_parameters IN (SELECT id FROM ${schema}.zaaktype_cmmn_humantask_parameters '
            'WHERE zaaktype_configuration_id IN (%s))',
            bpmn_configuration
        )
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_humantask_parameters',
        'child of a CMMN row of a BPMN zaaktype configuration',
        format('t.zaaktype_configuration_id IN (%s)', bpmn_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_usereventlistener_parameters',
        'child of a CMMN row of a BPMN zaaktype configuration',
        format('t.zaaktype_configuration_id IN (%s)', bpmn_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_cmmn_configuration',
        'CMMN row of a BPMN zaaktype configuration',
        format('t.id IN (%s)', bpmn_configuration)
    );
    PERFORM pg_temp.quarantine(
        'zaaktype_bpmn_configuration',
        'BPMN row of a CMMN zaaktype configuration',
        't.id IN (SELECT id FROM ${schema}.zaaktype_configuration WHERE configuration_type = ''CMMN'')'
    );
END $$;

DROP FUNCTION pg_temp.quarantine(TEXT, TEXT, TEXT);

CREATE SEQUENCE ${schema}.sq_zaaktype_process_binding START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE ${schema}.zaaktype_process_binding (
    id                        BIGINT  NOT NULL,
    zaaktype_configuration_id BIGINT  NOT NULL,
    process_engine            VARCHAR NOT NULL,
    definition_key            VARCHAR NOT NULL,
    CONSTRAINT pk_zaaktype_process_binding PRIMARY KEY (id),
    CONSTRAINT un_zaaktype_process_binding_zaaktype_configuration UNIQUE (zaaktype_configuration_id),
    CONSTRAINT fk_zaaktype_process_binding_zaaktype_configuration FOREIGN KEY (zaaktype_configuration_id)
        REFERENCES ${schema}.zaaktype_configuration (id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_zaaktype_process_binding_process_engine CHECK (process_engine IN ('CMMN', 'BPMN'))
);

-- A CMMN configuration without a case definition gets no binding: it is not valid for zaak creation, as before.
INSERT INTO ${schema}.zaaktype_process_binding (id, zaaktype_configuration_id, process_engine, definition_key)
SELECT nextval('${schema}.sq_zaaktype_process_binding'), cmmn.id, 'CMMN', cmmn.id_case_definition
FROM ${schema}.zaaktype_cmmn_configuration cmmn
JOIN ${schema}.zaaktype_configuration base ON base.id = cmmn.id AND base.configuration_type = 'CMMN'
WHERE NULLIF(TRIM(cmmn.id_case_definition), '') IS NOT NULL;

INSERT INTO ${schema}.zaaktype_process_binding (id, zaaktype_configuration_id, process_engine, definition_key)
SELECT nextval('${schema}.sq_zaaktype_process_binding'), bpmn.id, 'BPMN', bpmn.bpmn_process_definition_key
FROM ${schema}.zaaktype_bpmn_configuration bpmn
JOIN ${schema}.zaaktype_configuration base ON base.id = bpmn.id AND base.configuration_type = 'BPMN';

-- The CMMN extension keeps the id of its configuration, so the foreign keys of its child rows keep their values.
ALTER TABLE ${schema}.zaaktype_cmmn_configuration RENAME TO zaaktype_cmmn_extension;
ALTER TABLE ${schema}.zaaktype_cmmn_extension RENAME CONSTRAINT pk_zaakafhandelparameters TO pk_zaaktype_cmmn_extension;
ALTER TABLE ${schema}.zaaktype_cmmn_extension
    DROP CONSTRAINT fk_zaaktype_cmmn_configuration_zaaktype_configuration,
    DROP COLUMN id_case_definition,
    ADD COLUMN zaaktype_configuration_id BIGINT;
UPDATE ${schema}.zaaktype_cmmn_extension SET zaaktype_configuration_id = id;
ALTER TABLE ${schema}.zaaktype_cmmn_extension
    ALTER COLUMN zaaktype_configuration_id SET NOT NULL,
    ADD CONSTRAINT un_zaaktype_cmmn_extension_zaaktype_configuration UNIQUE (zaaktype_configuration_id),
    ADD CONSTRAINT fk_zaaktype_cmmn_extension_zaaktype_configuration FOREIGN KEY (zaaktype_configuration_id)
        REFERENCES ${schema}.zaaktype_configuration (id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;

DO $$
BEGIN
    CREATE SEQUENCE ${schema}.sq_zaaktype_cmmn_extension START WITH 1 INCREMENT BY 1 MINVALUE 1 NO MAXVALUE CACHE 1;
    PERFORM setval(
        '${schema}.sq_zaaktype_cmmn_extension',
        (SELECT COALESCE(MAX(id), 0) + 1 FROM ${schema}.zaaktype_cmmn_extension),
        false
    );
END $$;

ALTER TABLE ${schema}.zaaktype_cmmn_humantask_parameters
    RENAME COLUMN zaaktype_configuration_id TO zaaktype_cmmn_extension_id;
ALTER TABLE ${schema}.zaaktype_cmmn_humantask_parameters
    RENAME CONSTRAINT fk_zaaktype_cmmn_configuration TO fk_zaaktype_cmmn_extension;
ALTER TABLE ${schema}.zaaktype_cmmn_usereventlistener_parameters
    RENAME COLUMN zaaktype_configuration_id TO zaaktype_cmmn_extension_id;
ALTER TABLE ${schema}.zaaktype_cmmn_usereventlistener_parameters
    RENAME CONSTRAINT fk_zaaktype_cmmn_configuration TO fk_zaaktype_cmmn_extension;

DROP TABLE ${schema}.zaaktype_bpmn_configuration;

ALTER TABLE ${schema}.zaaktype_configuration DROP COLUMN configuration_type;
DROP TYPE IF EXISTS ${schema}.zaaktype_configuration_type;
