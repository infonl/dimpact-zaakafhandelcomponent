/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

UPDATE ${schema}.mail_template
SET onderwerp = REPLACE(onderwerp, '{ZAAK_TYPE}', '{ZAAKTYPE_OMSCHRIJVING}'),
    body      = REPLACE(body, '{ZAAK_TYPE}', '{ZAAKTYPE_OMSCHRIJVING}')
WHERE onderwerp LIKE '%{ZAAK_TYPE}%'
   OR body LIKE '%{ZAAK_TYPE}%';
