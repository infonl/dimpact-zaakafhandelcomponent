/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE
    ${schema}.template_group RENAME TO smartdocuments_document_creation_template_group;

ALTER SEQUENCE sq_template_group RENAME TO sq_document_creation_template_group;

ALTER TABLE
    ${schema}.smartdocuments_document_creation_template_group RENAME CONSTRAINT fk_template_group_zaakafhandelparameters TO fk_document_creation_template_group_zaakafhandelparameters;

ALTER INDEX idx_template_group_smartdocuments_id RENAME TO idx_document_creation_template_group_smartdocuments_id;

ALTER INDEX idx_template_group_name RENAME TO idx_document_creation_template_group_name;

ALTER INDEX idx_template_group_creation_date RENAME TO idx_document_creation_template_group_creation_date;

ALTER INDEX idx_template_group_zaakafhandelparameters_id RENAME TO idx_document_creation_template_group_zaakafhandelparameters_id;

ALTER TABLE
    ${schema}.template RENAME TO smartdocuments_document_creation_template;

ALTER SEQUENCE sq_template RENAME TO sq_document_creation_template;

ALTER TABLE
    ${schema}.smartdocuments_document_creation_template RENAME CONSTRAINT fk_template_zaakafhandelparameters TO fk_document_creation_template_zaakafhandelparameters;

ALTER TABLE
    ${schema}.smartdocuments_document_creation_template RENAME CONSTRAINT fk_template_template_group_id TO fk_document_creation_template_template_group_id;

ALTER INDEX idx_template_smartdocuments_id RENAME TO idx_document_creation_template_smartdocuments_id;

ALTER INDEX idx_template_name RENAME TO idx_document_creation_template_name;

ALTER INDEX idx_template_creation_date RENAME TO idx_document_creation_template_creation_date;

ALTER INDEX idx_template_zaakafhandelparameters_id RENAME TO idx_document_creation_template_zaakafhandelparameters_id;

CREATE
    INDEX idx_document_creation_template_template_group_id ON
    ${schema}.smartdocuments_document_creation_template
        USING btree(template_group_id);
