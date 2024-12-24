/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE
    ${schema}.referentie_tabel ADD COLUMN is_systeem_tabel BOOL DEFAULT FALSE;

COMMENT ON
COLUMN ${schema}.referentie_tabel.is_systeem_tabel IS 'Bepaalt of de referentietabel een systeemtabel is of niet. Systeemtabellen kunnen niet worden verwijderd door gebruikers.';

UPDATE
    ${schema}.referentie_tabel
SET
    is_systeem_tabel = TRUE
WHERE
    code = 'ADVIES';

UPDATE
    ${schema}.referentie_tabel
SET
    is_systeem_tabel = TRUE
WHERE
    code = 'AFZENDER';

UPDATE
    ${schema}.referentie_tabel
SET
    is_systeem_tabel = TRUE
WHERE
    code = 'COMMUNICATIEKANAAL';

UPDATE
    ${schema}.referentie_tabel
SET
    is_systeem_tabel = TRUE
WHERE
    code = 'DOMEIN';

UPDATE
    ${schema}.referentie_tabel
SET
    is_systeem_tabel = TRUE
WHERE
    code = 'SERVER_ERROR_ERROR_PAGINA_TEKST';
