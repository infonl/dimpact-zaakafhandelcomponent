/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

ALTER TABLE ${schema}.zaaktype_configuration
    ADD COLUMN IF NOT EXISTS gebruikersnaam_behandelaar VARCHAR(255);

UPDATE ${schema}.zaaktype_configuration zc
SET gebruikersnaam_behandelaar = cmmn.gebruikersnaam_behandelaar
FROM ${schema}.zaaktype_cmmn_configuration cmmn
WHERE zc.id = cmmn.id
  AND cmmn.gebruikersnaam_behandelaar IS NOT NULL;

ALTER TABLE ${schema}.zaaktype_cmmn_configuration
DROP COLUMN IF EXISTS gebruikersnaam_behandelaar;
