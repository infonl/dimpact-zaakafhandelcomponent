/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

CREATE TABLE ${schema}.verwerkte_productaanvraag
(
    uuid_productaanvraag_object UUID        NOT NULL,
    status                      VARCHAR(20) NOT NULL,
    gestart_op                  TIMESTAMP   NOT NULL,
    CONSTRAINT pk_verwerkte_productaanvraag PRIMARY KEY (uuid_productaanvraag_object)
);
