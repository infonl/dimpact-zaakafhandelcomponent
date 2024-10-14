/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

CREATE TABLE ${schema}.formio_formulier
(
    id_formio_formulier BIGINT  NOT NULL,
    name                VARCHAR NOT NULL,
    title               VARCHAR NOT NULL,
    filename            VARCHAR NOT NULL,
    content             VARCHAR NOT NULL,
    CONSTRAINT pk_formio_formulier PRIMARY KEY (id_formio_formulier),
    CONSTRAINT un_formio_formulier_name UNIQUE (name)
);

CREATE SEQUENCE ${schema}.sq_formio_formulier START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
