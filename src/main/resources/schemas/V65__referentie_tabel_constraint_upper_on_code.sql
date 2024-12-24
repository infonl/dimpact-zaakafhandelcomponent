/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE
    ${schema}.referentie_tabel ADD CONSTRAINT up_referentie_tabel_code CHECK(
        UPPER( code )= code
    );
