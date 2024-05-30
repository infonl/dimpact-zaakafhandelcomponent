/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

CREATE TABLE ${schema}.template_group
(
    id_template_group BIGINT                   NOT NULL,
    smartdocuments_id VARCHAR                  NOT_NULL,
    naam              VARCHAR                  NOT NULL,
    creatiedatum      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_template_group PRIMARY KEY (id_template_group)
);

CREATE SEQUENCE ${schema}.sq_template_group START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE INDEX idx_template_group_smartdocuments_id ON ${schema}.template_group USING btree (smartdocuments_id);
CREATE INDEX idx_template_group_naam ON ${schema}.template_group USING btree (naam);
CREATE INDEX idx_template_group_creatiedatum ON ${schema}.template_group USING btree (creatiedatum);

CREATE TABLE ${schema}.template
(
    id_template       BIGINT                   NOT NULL,
    smartdocuments_id VARCHAR                  NOT_NULL,
    id_template       BIGINT                   NOT_NULL,
    naam              VARCHAR                  NOT NULL,
    creatiedatum      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_template PRIMARY KEY (id_template)
);

CREATE SEQUENCE ${schema}.sq_template START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE INDEX idx_template_smartdocuments_id ON ${schema}.template USING btree (smartdocuments_id);
CREATE INDEX idx_template_id_template ON ${schema}.template USING btree (id_template);
CREATE INDEX idx_template_naam ON ${schema}.template USING btree (naam);
CREATE INDEX idx_template_creatiedatum ON ${schema}.template USING btree (creatiedatum);
