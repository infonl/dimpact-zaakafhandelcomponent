/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

ALTER TABLE ${schema}.template_group
    ADD COLUMN id_zaakafhandelparameters BIGINT NOT NULL;

CREATE INDEX idx_template_group_id_zaakafhandelparameters
    ON ${schema}.template_group USING btree (id_zaakafhandelparameters);

ALTER TABLE ${schema}.template_group
    ADD CONSTRAINT fk_template_group_zaakafhandelparameters
        FOREIGN KEY (id_zaakafhandelparameters)
            REFERENCES ${schema}.zaakafhandelparameters (id_zaakafhandelparameters)
                MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;

COMMENT ON COLUMN ${schema}.template_group.id_zaakafhandelparameters IS 'ID of the Zaakafhandel parameter';


ALTER TABLE ${schema}.template
    ADD COLUMN id_zaakafhandelparameters BIGINT NOT NULL;

CREATE INDEX idx_template_id_zaakafhandelparameters
    ON ${schema}.template USING btree (id_zaakafhandelparameters);

ALTER TABLE ${schema}.template
    ADD CONSTRAINT fk_template_zaakafhandelparameters
        FOREIGN KEY (id_zaakafhandelparameters)
            REFERENCES ${schema}.zaakafhandelparameters (id_zaakafhandelparameters)
                MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;

COMMENT ON COLUMN ${schema}.template.id_zaakafhandelparameters IS 'ID of the Zaakafhandel parameter';
