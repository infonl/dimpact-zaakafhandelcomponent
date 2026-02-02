/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE ${schema}.zaaktype_configuration ADD COLUMN niet_ontvankelijk_resultaattype_uuid uuid NULL;

UPDATE ${schema}.zaaktype_configuration ztc
SET niet_ontvankelijk_resultaattype_uuid = cmmn.niet_ontvankelijk_resultaattype_uuid
FROM ${schema}.zaaktype_configuration cmmn
WHERE ztc.id = cmmn.id
  AND cmmn.niet_ontvankelijk_resultaattype_uuid IS NOT NULL;

ALTER TABLE ${schema}.zaaktype_cmmn_configuration DROP COLUMN niet_ontvankelijk_resultaattype_uuid;
