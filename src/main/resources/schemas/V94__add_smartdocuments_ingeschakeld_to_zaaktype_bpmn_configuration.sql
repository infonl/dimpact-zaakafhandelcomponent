/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Move smartdocuments_ingeschakeld from zaaktype_cmmn_configuration to the base zaaktype_configuration table
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