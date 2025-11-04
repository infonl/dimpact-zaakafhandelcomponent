/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Insert missing zaaktype_cmmn_configuration rows for orphaned BPMN configurations
INSERT INTO ${schema}.zaaktype_cmmn_configuration (
    id,
    zaaktype_uuid,
    zaaktype_omschrijving,
    creatiedatum,
    intake_mail,
    afronden_mail,
    smartdocuments_ingeschakeld
)
SELECT
    nextval('${schema}.sq_zaaktype_cmmn_configuration'),
    bpmn.zaaktype_uuid,
    bpmn.zaaktype_omschrijving,
    NOW(),
    'BESCHIKBAAR_UIT',
    'BESCHIKBAAR_UIT',
    FALSE
FROM ${schema}.zaaktype_bpmn_configuration bpmn
WHERE NOT EXISTS (
    SELECT 1
    FROM ${schema}.zaaktype_cmmn_configuration cmmn
    WHERE cmmn.zaaktype_uuid = bpmn.zaaktype_uuid
);    
    
-- Add foreign key column to BPMN configuration table
ALTER TABLE ${schema}.zaaktype_bpmn_configuration
    ADD COLUMN zaaktype_configuration_id BIGINT,
    DROP CONSTRAINT pk_zaaktype_bpmn_process_definition_id,
    DROP CONSTRAINT un_zaaktype_bpmn_process_definition_zaaktype,
    DROP CONSTRAINT un_zaaktype_bpmn_process_definition_productaanvraagtype;

-- Populate zaaktype_configuration_id based on zaaktypeUuid, selecting the latest by creatiedatum
UPDATE ${schema}.zaaktype_bpmn_configuration bpmn
SET zaaktype_configuration_id = (
    SELECT cmmn.id
        FROM ${schema}.zaaktype_cmmn_configuration cmmn
        WHERE cmmn.zaaktype_uuid = bpmn.zaaktype_uuid
        ORDER BY cmmn.creatiedatum DESC
        LIMIT 1
);

-- Create new table
CREATE TABLE ${schema}.zaaktype_bpmn_configuration_new
(
    id                          BIGINT       NOT NULL,
    zaaktype_configuration_id   BIGINT       NOT NULL,
    zaaktype_uuid               UUID         NOT NULL,
    bpmn_process_definition_key VARCHAR(255) NOT NULL,
    zaaktype_omschrijving       VARCHAR      NOT NULL,
    productaanvraagtype         VARCHAR,
    group_id                    VARCHAR      NOT NULL,
    CONSTRAINT pk_zaaktype_bpmn_process_definition_id PRIMARY KEY (id),
    CONSTRAINT fk_zaaktype_bpmn_configuration_zaakafhandelparameters
        FOREIGN KEY (zaaktype_configuration_id)
        REFERENCES ${schema}.zaaktype_cmmn_configuration(id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE
);

-- Copy data from old BPMN configuration table to the new table
INSERT INTO ${schema}.zaaktype_bpmn_configuration_new
    SELECT id, zaaktype_configuration_id, zaaktype_uuid, bpmn_process_definition_key, zaaktype_omschrijving, productaanvraagtype, group_id
    FROM ${schema}.zaaktype_bpmn_configuration;

-- Drop old table
DROP TABLE ${schema}.zaaktype_bpmn_configuration;

-- Rename new table
ALTER TABLE ${schema}.zaaktype_bpmn_configuration_new
    RENAME TO zaaktype_bpmn_configuration;
