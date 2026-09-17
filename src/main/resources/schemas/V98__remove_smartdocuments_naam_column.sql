/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- The persisted template/group name is never read as source of truth any more: the current name is
-- always resolved live from SmartDocuments, matched by id, both when generating a document and when
-- displaying the mapping. The column is therefore dead weight.
ALTER TABLE ${schema}.zaaktype_smartdocuments_document_template_group_parameters
    DROP COLUMN naam;

ALTER TABLE ${schema}.zaaktype_smartdocuments_document_template_parameters
    DROP COLUMN naam;
