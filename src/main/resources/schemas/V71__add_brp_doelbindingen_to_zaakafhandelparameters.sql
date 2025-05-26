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

-- add referentie tabellen voor BRP doelbindingen
INSERT INTO ${schema}.referentie_tabel(id_referentie_tabel, code, naam, is_systeem_tabel)
VALUES
    (NEXTVAL('sq_referentie_tabel'), 'BRP_DOELBINDING_ZOEK_WAARDE', 'BRP Doelbinding Zoekwaarde', true),
    (NEXTVAL('sq_referentie_tabel'), 'BRP_DOELBINDING_RAADPLEEG_WAARDE', 'BRP Doelbinding Raadpleegwaarde', true);

-- add referentie waarden voor BRP doelbindingen (BRP_DOELBINDING_ZOEK_WAARDE)
INSERT INTO ${schema}.referentie_waarde(id_referentie_waarde, id_referentie_tabel, naam, is_systeem_waarde)
VALUES
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_ZOEK_WAARDE'),
     'BRPACT-ZoekenAlgemeen',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_ZOEK_WAARDE'),
     'BRPACT-ZoekenAlgemeenAdresNL',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_ZOEK_WAARDE'),
     'BRPACT-ZoekenAlgemeenGezag',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_ZOEK_WAARDE'),
     'BRPACT-ZoekenAlgemeenAdresNLGezag',
     true);

-- add referentie waarden voor BRP doelbindingen (BRP_DOELBINDING_RAADPLEEG_WAARDE)
INSERT INTO ${schema}.referentie_waarde (id_referentie_waarde, id_referentie_tabel, naam, is_systeem_waarde)
VALUES
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-AanschrijvenZakelijkGerechtigde',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-AlgemeneTaken',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-AlleTakenSpontaan',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-APV',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-Burgerzaken',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-DrankHoreca',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-DispensatieNietOpenbareArchiefstukken',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-FormulierenAanvragerBasis',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-GehandicaptenParkeerkaart',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-GemeentelijkeBelastingen',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-GevondenVoorwerpen',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-Huisvesting',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-HuisvestingAdres',
     true),
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-JeugdJeugdige',
     true),
    -- Continue adding entries for other items
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_DOELBINDING_RAADPLEEG_WAARDE'),
     'BRPACT-WOZ',
     true);

