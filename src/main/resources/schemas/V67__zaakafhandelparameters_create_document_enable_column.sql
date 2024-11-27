/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

ALTER TABLE ${schema}.zaakafhandelparameters
    ADD COLUMN document_maken_ingeschakeld BOOL default false;
COMMENT ON COLUMN ${schema}.zaakafhandelparameters.document_maken_ingeschakeld IS 'Maak het aanmaken van documenten voor dit zaaktype mogelijk';
