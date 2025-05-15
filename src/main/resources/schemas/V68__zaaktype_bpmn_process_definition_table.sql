/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

CREATE TABLE ${schema}.zaaktype_bpmn_process_definition
(
    id                                  BIGINT          NOT NULL,
    zaaktype_uuid                       UUID            NOT NULL,
    bpmn_process_definition_key         VARCHAR(255)    NOT NULL,
    CONSTRAINT pk_zaaktype_bpmn_process_definition_id PRIMARY KEY (id),
    CONSTRAINT un_zaaktype_bpmn_process_definition_zaaktype UNIQUE (zaaktype_uuid)
);

CREATE SEQUENCE ${schema}.sq_zaaktype_bpmn_process_definition START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
