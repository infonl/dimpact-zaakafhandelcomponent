/*
 * SPDX-FileCopyrightText: 2025 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

CREATE TABLE ${schema}.brp_doelbindingen (
     id_brp_doelbindingen BIGINT NOT NULL,
     id_zaakafhandelparameters BIGINT NOT NULL,
     zoekWaarde TEXT DEFAULT '',
     raadpleegWaarde TEXT DEFAULT '',

     CONSTRAINT pk_brp_doelbindingen PRIMARY KEY (id_brp_doelbindingen),
     CONSTRAINT fk_brp_doelbindingen_zaakafhandelparameters FOREIGN KEY (id_zaakafhandelparameters)
         REFERENCES ${schema}.zaakafhandelparameters(id_zaakafhandelparameters)
         MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE SEQUENCE ${schema}.sq_brp_doelbindingen START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- Add values to the new table for existing zaakafhandelparameters
INSERT INTO ${schema}.brp_doelbindingen (id_brp_doelbindingen, id_zaakafhandelparameters, zoekWaarde, raadpleegWaarde)
SELECT
    nextval('${schema}.sq_brp_doelbindingen'), -- Generate IDs using the sequence
    id_zaakafhandelparameters,
    '', -- Default value for zoekWaarde
    ''  -- Default value for raadpleegWaarde
FROM
    ${schema}.zaakafhandelparameters
WHERE
    id_zaakafhandelparameters NOT IN (
        SELECT id_zaakafhandelparameters
        FROM ${schema}.brp_doelbindingen
    );
