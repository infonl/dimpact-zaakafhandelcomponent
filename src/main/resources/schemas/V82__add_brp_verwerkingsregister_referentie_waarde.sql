/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- add referentie tabellen voor BRP doelbindingen
INSERT INTO ${schema}.referentie_tabel(id_referentie_tabel, code, naam, is_systeem_tabel)
VALUES
    (NEXTVAL('sq_referentie_tabel'), 'BRP_VERWERKINGSREGISTER_WAARDE', 'BRP Verwerkingsregister', true);

-- add referentie waarden voor BRP doelbindingen (BRP_DOELBINDING_ZOEK_WAARDE)
INSERT INTO ${schema}.referentie_waarde(id_referentie_waarde, id_referentie_tabel, naam, is_systeem_waarde)
VALUES
    (NEXTVAL('sq_referentie_waarde'),
     (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'BRP_VERWERKINGSREGISTER_WAARDE'),
     'Algemeen',
     true);
