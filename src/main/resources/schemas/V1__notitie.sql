/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
CREATE
    SEQUENCE ${schema}.notitie_sq
START WITH
    1 INCREMENT BY 1 NO MAXVALUE NO MINVALUE cache 1;

CREATE
    TABLE
        ${schema}.notitie(
            id BIGINT NOT NULL,
            zaak_uuid uuid,
            tekst text NOT NULL,
            tijdstip_laatste_wijziging DATE NOT NULL,
            gebruikersnaam_medewerker VARCHAR NOT NULL,
            CONSTRAINT notitie_pk PRIMARY KEY(id)
        );
