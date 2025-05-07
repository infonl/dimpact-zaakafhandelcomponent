/*
 * SPDX-FileCopyrightText: 2025 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

CREATE TABLE ${schema}.betrokkene_koppelingen (
     id_betrokkene_koppelingen BIGINT NOT NULL,
     id_zaakafhandelparameters BIGINT NOT NULL,
     brpKoppelen BOOLEAN DEFAULT FALSE,
     kvkKoppelen BOOLEAN DEFAULT FALSE,

     CONSTRAINT pk_betrokkene_koppelingen PRIMARY KEY (id_betrokkene_koppelingen),
     CONSTRAINT fk_betrokkene_koppelingen_zaakafhandelparameters FOREIGN KEY (id_zaakafhandelparameters)
         REFERENCES ${schema}.zaakafhandelparameters(id_zaakafhandelparameters)
         MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE SEQUENCE ${schema}.sq_betrokkene_koppelingen START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- Add values to the new table for existing zaakafhandelparameters
INSERT INTO ${schema}.betrokkene_koppelingen (id_zaakafhandelparameters, brpKoppelen, kvkKoppelen)
SELECT
    id_zaakafhandelparameters,
    FALSE, -- Default value for brpKoppelen
    FALSE  -- Default value for kvkKoppelen
FROM
    ${schema}.zaakafhandelparameters
WHERE
    id_zaakafhandelparameters NOT IN (
        SELECT id_zaakafhandelparameters
        FROM ${schema}.betrokkene_koppelingen
    );
