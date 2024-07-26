/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- add column is_systeem_waarde to referentie_tabel table to indicate if a referentie tabel should be considered a system value
-- and should be treated as read-only in the application
ALTER TABLE ${schema}.referentie_tabel
ADD COLUMN is_systeem_tabel BOOL default false;

UPDATE ${schema}.referentie_tabel
SET is_systeem_tabel = TRUE
WHERE code = 'ADVIES';

UPDATE ${schema}.referentie_tabel
SET is_systeem_tabel = TRUE
WHERE code = 'AFZENDER';

UPDATE ${schema}.referentie_tabel
SET is_systeem_tabel = TRUE
WHERE code = 'COMMUNICATIEKANAAL';

UPDATE ${schema}.referentie_tabel
SET is_systeem_tabel = TRUE
WHERE code = 'DOMEIN';

UPDATE ${schema}.referentie_tabel
SET is_systeem_tabel = TRUE
WHERE code = 'SERVER_ERROR_ERROR_PAGINA_TEKST';
