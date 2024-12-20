/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE
    ${schema}.referentie_waarde ADD COLUMN is_systeem_waarde BOOL DEFAULT FALSE;

COMMENT ON
COLUMN ${schema}.referentie_waarde.is_systeem_waarde IS 'Bepaalt of de referentiewaarde een systeemwaarde is of niet. Systeemwaardes kunnen niet worden aangepast of verwijderd door gebruikers.';

UPDATE
    ${schema}.referentie_waarde
SET
    is_systeem_waarde = TRUE
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'DOMEIN'
    )
    AND naam = 'domein_overig';

UPDATE
    ${schema}.referentie_waarde
SET
    is_systeem_waarde = TRUE
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'COMMUNICATIEKANAAL'
    )
    AND naam = 'E-formulier';

-- set default 'volgorde' for all default COMMUNICATIEKANAAL reference values
UPDATE
    ${schema}.referentie_waarde
SET
    volgorde = 0
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'COMMUNICATIEKANAAL'
    )
    AND naam = 'Balie';

UPDATE
    ${schema}.referentie_waarde
SET
    volgorde = 1
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'COMMUNICATIEKANAAL'
    )
    AND naam = 'E-formulier';

UPDATE
    ${schema}.referentie_waarde
SET
    volgorde = 2
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'COMMUNICATIEKANAAL'
    )
    AND naam = 'E-mail';

UPDATE
    ${schema}.referentie_waarde
SET
    volgorde = 3
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'COMMUNICATIEKANAAL'
    )
    AND naam = 'Intern';

UPDATE
    ${schema}.referentie_waarde
SET
    volgorde = 4
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'COMMUNICATIEKANAAL'
    )
    AND naam = 'Internet';

UPDATE
    ${schema}.referentie_waarde
SET
    volgorde = 4
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'COMMUNICATIEKANAAL'
    )
    AND naam = 'Medewerkersportaal';

UPDATE
    ${schema}.referentie_waarde
SET
    volgorde = 4
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'COMMUNICATIEKANAAL'
    )
    AND naam = 'Post';

UPDATE
    ${schema}.referentie_waarde
SET
    volgorde = 4
WHERE
    id_referentie_tabel =(
        SELECT
            id_referentie_tabel
        FROM
            ${schema}.referentie_tabel
        WHERE
            code = 'COMMUNICATIEKANAAL'
    )
    AND naam = 'Telefoon';
