/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- This migration moves smartdocuments_ingeschakeld from the CMMN-specific
-- zaaktype_cmmn_configuration table to the shared zaaktype_configuration table,
-- making SmartDocuments opt-in available for both BPMN and CMMN zaaktypes.
-- The filename is retained for migration history compatibility.
ALTER TABLE ${schema}.zaaktype_configuration
    ADD COLUMN smartdocuments_ingeschakeld BOOLEAN NOT NULL DEFAULT FALSE;

-- Copy existing values from CMMN-specific table to the base table
UPDATE ${schema}.zaaktype_configuration zc
    SET smartdocuments_ingeschakeld = cmmn.smartdocuments_ingeschakeld
    FROM ${schema}.zaaktype_cmmn_configuration cmmn
    WHERE cmmn.id = zc.id;

-- Drop the column from the CMMN-specific table
ALTER TABLE ${schema}.zaaktype_cmmn_configuration
    DROP COLUMN smartdocuments_ingeschakeld;
