/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

ALTER TABLE ${schema}.zaaktype_bpmn_process_definition
    ADD COLUMN zaaktype_omschrijving VARCHAR,
    ADD COLUMN product_aanvraag_type VARCHAR;
