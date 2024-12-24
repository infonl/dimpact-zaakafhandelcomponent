/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE
    ${schema}.template_group ADD COLUMN zaakafhandelparameters_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_template_group_zaakafhandelparameters FOREIGN KEY(zaakafhandelparameters_id) REFERENCES ${schema}.zaakafhandelparameters(id_zaakafhandelparameters) MATCH SIMPLE ON
    UPDATE
        CASCADE ON
        DELETE
            CASCADE;

CREATE
    INDEX idx_template_group_zaakafhandelparameters_id ON
    ${schema}.template_group
        USING btree(zaakafhandelparameters_id);

COMMENT ON
COLUMN ${schema}.template_group.zaakafhandelparameters_id IS 'ID of the Zaakafhandel parameter';

ALTER TABLE
    ${schema}.template ADD COLUMN zaakafhandelparameters_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_template_zaakafhandelparameters FOREIGN KEY(zaakafhandelparameters_id) REFERENCES ${schema}.zaakafhandelparameters(id_zaakafhandelparameters) MATCH SIMPLE ON
    UPDATE
        CASCADE ON
        DELETE
            CASCADE;

CREATE
    INDEX idx_template_zaakafhandelparameters_id ON
    ${schema}.template
        USING btree(zaakafhandelparameters_id);

COMMENT ON
COLUMN ${schema}.template.zaakafhandelparameters_id IS 'ID of the Zaakafhandel parameter';

ALTER TABLE
    ${schema}.template DROP
        CONSTRAINT fk_template_template_group,
        DROP
            COLUMN id_template_group,
            ADD COLUMN template_group_id BIGINT NOT NULL,
            ADD CONSTRAINT fk_template_template_group_id FOREIGN KEY(template_group_id) REFERENCES ${schema}.template_group(id_template_group) MATCH SIMPLE ON
            UPDATE
                CASCADE ON
                DELETE
                    CASCADE;

COMMENT ON
COLUMN ${schema}.template.template_group_id IS 'ID of the template group this template is part of';
