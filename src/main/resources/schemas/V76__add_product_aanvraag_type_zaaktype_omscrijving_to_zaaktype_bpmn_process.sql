/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

ALTER TABLE ${schema}.zaaktype_bpmn_process_definition
    ADD COLUMN zaaktype_omschrijving VARCHAR,
    ADD COLUMN productaanvraagtype VARCHAR;

UPDATE ${schema}.zaaktype_bpmn_process_definition
    SET zaaktype_omschrijving = 'na'
    WHERE zaaktype_omschrijving IS NULL;

ALTER TABLE ${schema}.zaaktype_bpmn_process_definition
    ALTER COLUMN zaaktype_omschrijving SET NOT NULL;
