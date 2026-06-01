/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Remove the system value flag from rows in the BRP protocollering reference tables
-- so that municipalities can delete these values.
-- The tables themselves remain system tables (is_systeem_tabel stays true).
UPDATE ${schema}.referentie_waarde
SET is_systeem_waarde = false
WHERE id_referentie_tabel IN (
    SELECT id_referentie_tabel
    FROM ${schema}.referentie_tabel
    WHERE code IN (
        'BRP_DOELBINDING_ZOEK_WAARDE',
        'BRP_DOELBINDING_RAADPLEEG_WAARDE',
        'BRP_VERWERKINGSREGISTER_WAARDE'
    )
);
