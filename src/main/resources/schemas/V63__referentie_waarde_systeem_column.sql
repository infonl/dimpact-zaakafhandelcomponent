/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- add column is_systeem_waarde to referentie_waarde table to indicate if a referentie waarde should be considered a system value
-- and should be treated as read-only in the application
ALTER TABLE ${schema}.referentie_waarde
    ADD COLUMN is_systeem_waarde BOOL default false;

UPDATE ${schema}.referentie_waarde
SET is_systeem_waarde = TRUE
WHERE id_referentie_tabel = (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'DOMEIN')
AND naam = 'domein_overig';

UPDATE ${schema}.referentie_waarde
SET is_systeem_waarde = TRUE
WHERE id_referentie_tabel = (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'COMMUNICATIEKANAAL')
  AND naam = 'E-formulier';



