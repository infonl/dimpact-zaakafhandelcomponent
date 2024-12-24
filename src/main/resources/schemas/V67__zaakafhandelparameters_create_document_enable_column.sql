/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE
    ${schema}.zaakafhandelparameters ADD COLUMN smartdocuments_ingeschakeld BOOL DEFAULT FALSE;

COMMENT ON
COLUMN ${schema}.zaakafhandelparameters.smartdocuments_ingeschakeld IS 'Maak het aanmaken van documenten via SmartDocuments mogelijk voor dit casetype';
