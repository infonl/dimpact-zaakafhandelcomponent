/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE ${schema}.zaaktype_cmmn_betrokkene_parameters RENAME TO zaaktype_betrokkene_parameters;
ALTER SEQUENCE ${schema}.sq_zaaktype_cmmn_betrokkene_parameters RENAME TO sq_zaaktype_betrokkene_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_brp_parameters RENAME TO zaaktype_brp_parameters;
ALTER SEQUENCE ${schema}.sq_zaaktype_cmmn_brp_parameters RENAME TO sq_zaaktype_brp_parameters;
