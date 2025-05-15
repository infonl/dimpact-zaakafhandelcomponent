/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
INSERT INTO ${schema}.referentie_tabel(id_referentie_tabel, code, naam)
VALUES (NEXTVAL('sq_referentie_tabel'), 'COMMUNICATIEKANAAL', 'Communicatiekanaal');

INSERT INTO ${schema}.referentie_waarde(id_referentie_waarde, id_referentie_tabel, naam)
VALUES (NEXTVAL('sq_referentie_waarde'),
        (SELECT id_referentie_tabel
         FROM ${schema}.referentie_tabel
         WHERE code = 'COMMUNICATIEKANAAL')
           , 'Balie'),
       (NEXTVAL('sq_referentie_waarde'),
        (SELECT id_referentie_tabel
         FROM ${schema}.referentie_tabel
         WHERE code = 'COMMUNICATIEKANAAL')
           , 'E-mail'),
       (NEXTVAL('sq_referentie_waarde'),
        (SELECT id_referentie_tabel
         FROM ${schema}.referentie_tabel
         WHERE code = 'COMMUNICATIEKANAAL')
           , 'E-formulier'),
       (NEXTVAL('sq_referentie_waarde'),
        (SELECT id_referentie_tabel
         FROM ${schema}.referentie_tabel
         WHERE code = 'COMMUNICATIEKANAAL')
           , 'Intern'),
       (NEXTVAL('sq_referentie_waarde'),
        (SELECT id_referentie_tabel
         FROM ${schema}.referentie_tabel
         WHERE code = 'COMMUNICATIEKANAAL')
           , 'Internet'),
       (NEXTVAL('sq_referentie_waarde'),
        (SELECT id_referentie_tabel
         FROM ${schema}.referentie_tabel
         WHERE code = 'COMMUNICATIEKANAAL')
           , 'Medewerkersportaal'),
       (NEXTVAL('sq_referentie_waarde'),
        (SELECT id_referentie_tabel
         FROM ${schema}.referentie_tabel
         WHERE code = 'COMMUNICATIEKANAAL')
           , 'Post'),
       (NEXTVAL('sq_referentie_waarde'),
        (SELECT id_referentie_tabel
         FROM ${schema}.referentie_tabel
         WHERE code = 'COMMUNICATIEKANAAL')
           , 'Telefoon');



