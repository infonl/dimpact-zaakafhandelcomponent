/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Remove obsolete old IAM 'domein' column and related index from 'zaaktype_configuration' table
DROP INDEX zaaktype_configuration_domein_idx;

ALTER TABLE ${schema}.zaaktype_configuration DROP COLUMN domein;

-- Remove obsolete old IAM 'DOMEIN' reference table value records
DELETE FROM ${schema}.referentie_waarde
    WHERE id_referentie_tabel = (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'DOMEIN');

-- Remove obsolete old IAM 'DOMEIN' reference table record
DELETE FROM ${schema}.referentie_tabel WHERE code = 'DOMEIN';
