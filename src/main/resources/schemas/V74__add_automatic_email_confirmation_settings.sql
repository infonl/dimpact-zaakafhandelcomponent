/*
 * SPDX-FileCopyrightText: 2025 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

CREATE TABLE ${schema}.automatic_email_confirmation (
     id_automatic_email_confirmation BIGINT NOT NULL,
     id_zaakafhandelparameters BIGINT NOT NULL,
     enabled BOOLEAN DEFAULT FALSE,
     template_name TEXT DEFAULT NULL,
     email_sender TEXT DEFAULT NULL,
     email_reply TEXT DEFAULT NULL,

     CONSTRAINT pk_automatic_email_confirmation PRIMARY KEY (id_automatic_email_confirmation),
     CONSTRAINT fk_automatic_email_confirmation_zaakafhandelparameters FOREIGN KEY (id_zaakafhandelparameters)
         REFERENCES ${schema}.zaakafhandelparameters(id_zaakafhandelparameters)
         MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE SEQUENCE ${schema}.sq_automatic_email_confirmation START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
