/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
INSERT
    INTO
        ${schema}.referentie_tabel(
            id_referentie_tabel,
            code,
            naam
        )
    VALUES(
        NEXTVAL('sq_referentie_tabel'),
        'SERVER_ERROR_ERROR_PAGINA_TEKST',
        'Server error error pagina tekst'
    );
