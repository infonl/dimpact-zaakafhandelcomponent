/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

ALTER TABLE ${schema}.zaaktype_bpmn_configuration
    ADD COLUMN creatiedatum TIMESTAMP WITH TIME ZONE NOT NULL default NOW(),
    DROP CONSTRAINT pk_zaaktype_bpmn_process_definition_id,
    DROP CONSTRAINT un_zaaktype_bpmn_process_definition_zaaktype,
    DROP CONSTRAINT un_zaaktype_bpmn_process_definition_productaanvraagtype;

CREATE INDEX idx_zaaktype_bpmn_configuration_creatiedatum ON ${schema}.zaaktype_bpmn_configuration USING btree (creatiedatum);
