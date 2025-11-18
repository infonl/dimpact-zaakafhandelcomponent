/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
CREATE TABLE ${schema}.zaaktype_configuration (
    LIKE ${schema}.zaaktype_cmmn_configuration INCLUDING ALL
);
CREATE SEQUENCE ${schema}.sq_zaaktype_configuration START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

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

-- add foreign key to zaaktype_configuration
ALTER TABLE ${schema}.zaaktype_cmmn_configuration
    ADD COLUMN zaaktype_configuration_id    BIGINT,
    ADD CONSTRAINT fk_zaaktype_configuration FOREIGN KEY (zaaktype_configuration_id)
        REFERENCES ${schema}.zaaktype_configuration (id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;

-- link CMMN and base zaaktype_configuration
UPDATE ${schema}.zaaktype_cmmn_configuration
    SET zaaktype_configuration_id = id
    WHERE zaaktype_configuration_id IS NULL;

-- make foreign key mandatory
ALTER TABLE ${schema}.zaaktype_cmmn_configuration
    ALTER COLUMN zaaktype_configuration_id SET NOT NULL;

-- add BPMN data to zaaktype_configuration
INSERT INTO ${schema}.zaaktype_configuration (
    SELECT
        (SELECT COALESCE(MAX(id), 0) FROM ${schema}.zaaktype_configuration) + id AS id,
        zaaktype_uuid,
        group_id,
        zaaktype_omschrijving,
        creatiedatum,
        productaanvraagtype,
        NULL::VARCHAR AS domein,
        'BPMN'::zaaktype_configuration_type
    FROM ${schema}.zaaktype_bpmn_configuration
);

-- add foreign key to zaaktype_configuration
ALTER TABLE ${schema}.zaaktype_bpmn_configuration
    ADD COLUMN zaaktype_configuration_id    BIGINT,
    ADD CONSTRAINT fk_zaaktype_configuration FOREIGN KEY (zaaktype_configuration_id)
        REFERENCES ${schema}.zaaktype_configuration (id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;

-- link BPMN and base zaaktype_configuration
UPDATE ${schema}.zaaktype_bpmn_configuration bpmn
    SET zaaktype_configuration_id = z.id
    FROM ${schema}.zaaktype_configuration z
    WHERE bpmn.zaaktype_uuid = z.zaaktype_uuid;

-- drop the common zaaktype_configuration columns in bpmn-only table
ALTER TABLE ${schema}.zaaktype_bpmn_configuration
    DROP COLUMN zaaktype_uuid,
    DROP COLUMN group_id,
    DROP COLUMN zaaktype_omschrijving,
    DROP COLUMN creatiedatum,
    DROP COLUMN productaanvraagtype;
