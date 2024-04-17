/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

INSERT INTO ${schema}.referentie_tabel( id_referentie_tabel
                                      , code
                                      , naam)
VALUES ( NEXTVAL('sq_referentie_tabel')
       , 'COMMUNICATIEKANAAL'
       , 'Communicatiekanaal');

INSERT INTO ${schema}.referentie_waarde( id_referentie_waarde
                                       , id_referentie_tabel
                                       , naam)
VALUES ( NEXTVAL('sq_referentie_waarde')
       , (SELECT id_referentie_tabel
          FROM ${schema}.referentie_tabel
          WHERE code = 'COMMUNICATIEKANAAL')
       , 'E-formulier');
