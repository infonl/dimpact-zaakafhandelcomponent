/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

CREATE TABLE ${schema}.template_group
(
    id_template_group        BIGINT                   NOT NULL,
    smartdocuments_id        VARCHAR                  NOT NULL,
    naam                     VARCHAR                  NOT NULL,
    parent_template_group_id BIGINT,
    creatiedatum             TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_template_group PRIMARY KEY (id_template_group)
);

CREATE SEQUENCE ${schema}.sq_template_group START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE INDEX idx_template_group_smartdocuments_id ON ${schema}.template_group USING btree (smartdocuments_id);
CREATE INDEX idx_template_group_naam ON ${schema}.template_group USING btree (naam);
CREATE INDEX idx_template_group_creatiedatum ON ${schema}.template_group USING btree (creatiedatum);
COMMENT ON COLUMN ${schema}.template_group.id_template_group IS 'Unique ID for the template group';
COMMENT ON COLUMN ${schema}.template_group.smartdocuments_id IS 'ID for the template group in SmartDocuments';
COMMENT ON COLUMN ${schema}.template_group.naam IS 'Name of the template group';
COMMENT ON COLUMN ${schema}.template_group.parent_template_group_id IS 'ID of the parent template group (or NULL for root)';
COMMENT ON COLUMN ${schema}.template_group.creatiedatum IS 'Date on which the template group was stored in this table';

CREATE TABLE ${schema}.template
(
    id_template       BIGINT                   NOT NULL,
    smartdocuments_id VARCHAR                  NOT NULL,
    id_template_group BIGINT                   NOT NULL,
    naam              VARCHAR                  NOT NULL,
    creatiedatum      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_template PRIMARY KEY (id_template),
    CONSTRAINT un_id_template_id_template_group UNIQUE (id_template, id_template_group),
    CONSTRAINT fk_template_template_group FOREIGN KEY (id_template)
        REFERENCES ${schema}.template_group (id_template_group)
            MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE SEQUENCE ${schema}.sq_template START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE INDEX idx_template_smartdocuments_id ON ${schema}.template USING btree (smartdocuments_id);
CREATE INDEX idx_template_id_template_group ON ${schema}.template USING btree (id_template_group);
CREATE INDEX idx_template_naam ON ${schema}.template USING btree (naam);
CREATE INDEX idx_template_creatiedatum ON ${schema}.template USING btree (creatiedatum);
COMMENT ON COLUMN ${schema}.template.id_template IS 'Unique ID for the template';
COMMENT ON COLUMN ${schema}.template.smartdocuments_id IS 'ID for the template in SmartDocuments';
COMMENT ON COLUMN ${schema}.template.id_template_group IS 'ID of the template group this template is part of';
COMMENT ON COLUMN ${schema}.template.naam IS 'Name of the template';
COMMENT ON COLUMN ${schema}.template.creatiedatum IS 'Date on which the template was stored in this table';
