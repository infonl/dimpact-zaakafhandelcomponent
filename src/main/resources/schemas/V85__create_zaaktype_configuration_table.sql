/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- copy CMMN table in the base table
CREATE TABLE ${schema}.zaaktype_configuration (
    LIKE ${schema}.zaaktype_cmmn_configuration INCLUDING ALL
);

-- copy existing data from CMMN table
INSERT INTO ${schema}.zaaktype_configuration
    SELECT * FROM ${schema}.zaaktype_cmmn_configuration;

-- drop cmmn-only columns
ALTER TABLE ${schema}.zaaktype_configuration
    DROP COLUMN id_case_definition,
    DROP COLUMN gebruikersnaam_behandelaar,
    DROP COLUMN eindatum_gepland_waarschuwing,
    DROP COLUMN uiterlijke_einddatum_afdoening_waarschuwing,
    DROP COLUMN niet_ontvankelijk_resultaattype_uuid,
    DROP COLUMN intake_mail,
    DROP COLUMN afronden_mail,
    DROP COLUMN smartdocuments_ingeschakeld;

-- add discriminator column
CREATE TYPE zaaktype_configuration_type AS ENUM ('CMMN', 'BPMN');
ALTER TABLE ${schema}.zaaktype_configuration
    ADD COLUMN configuration_type zaaktype_configuration_type;

-- mark configured zaaktypes as cmmn
UPDATE ${schema}.zaaktype_configuration
    SET configuration_type = 'CMMN'
    WHERE groep_id IS NOT NULL
    AND groep_id <> '';

-- drop all unconfigured CMMN zaaktype rows
DELETE FROM ${schema}.zaaktype_cmmn_configuration
    WHERE groep_id IS NULL
    OR groep_id = '';

-- drop the common zaaktype_configuration columns in cmmn-only table
ALTER TABLE ${schema}.zaaktype_cmmn_configuration
    DROP COLUMN zaaktype_uuid,
    DROP COLUMN groep_id,
    DROP COLUMN zaaktype_omschrijving,
    DROP COLUMN creatiedatum,
    DROP COLUMN productaanvraagtype,
    DROP COLUMN domein;

-- drop the CMMN sequence
DROP SEQUENCE ${schema}.sq_zaaktype_cmmn_configuration;

-- bump the BPMN IDs to avoid conflicts with CMMN IDs
UPDATE ${schema}.zaaktype_bpmn_configuration
    SET id = id + (SELECT COALESCE(MAX(id), 0) FROM ${schema}.zaaktype_configuration)
    WHERE group_id IS NOT NULL;

-- add BPMN data to zaaktype_configuration
INSERT INTO ${schema}.zaaktype_configuration (
    SELECT
        id,
        zaaktype_uuid,
        group_id,
        zaaktype_omschrijving,
        creatiedatum,
        productaanvraagtype,
        NULL::VARCHAR AS domein,
        'BPMN'::zaaktype_configuration_type
    FROM ${schema}.zaaktype_bpmn_configuration
);

-- drop the common zaaktype_configuration columns in bpmn-only table
ALTER TABLE ${schema}.zaaktype_bpmn_configuration
    DROP COLUMN zaaktype_uuid,
    DROP COLUMN group_id,
    DROP COLUMN zaaktype_omschrijving,
    DROP COLUMN creatiedatum,
    DROP COLUMN productaanvraagtype;

-- drop the BPMN sequence
DROP SEQUENCE ${schema}.sq_zaaktype_bpmn_configuration;

-- create/reset the sequence sq_zaaktype_configuration
DO $$
BEGIN
    -- Create a new sequence with a placeholder start value
    CREATE SEQUENCE ${schema}.sq_zaaktype_configuration
        START WITH 0
        INCREMENT BY 1
        MINVALUE 0
        NO MAXVALUE
        CACHE 1;
    -- Set the sequence start to the last value inserted
    PERFORM setval(
        'sq_zaaktype_configuration',
        (SELECT COALESCE(MAX(id), 0) FROM ${schema}.zaaktype_configuration)
    );
END $$;
