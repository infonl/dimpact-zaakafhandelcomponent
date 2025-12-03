/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE ${schema}.zaaktype_cmmn_betrokkene_parameters RENAME TO zaaktype_betrokkene_parameters;
ALTER TABLE ${schema}.zaaktype_betrokkene_parameters DROP CONSTRAINT fk_zaaktype_cmmn_configuration;
ALTER TABLE ${schema}.zaaktype_betrokkene_parameters ADD CONSTRAINT fk_zaaktype_configuration FOREIGN KEY (zaaktype_configuration_id)
    REFERENCES ${schema}.zaaktype_configuration(id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER SEQUENCE ${schema}.sq_zaaktype_cmmn_betrokkene_parameters RENAME TO sq_zaaktype_betrokkene_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_brp_parameters RENAME TO zaaktype_brp_parameters;
ALTER TABLE ${schema}.zaaktype_brp_parameters DROP CONSTRAINT fk_zaaktype_cmmn_configuration;
ALTER TABLE ${schema}.zaaktype_brp_parameters ADD CONSTRAINT fk_zaaktype_configuration FOREIGN KEY (zaaktype_configuration_id)
    REFERENCES ${schema}.zaaktype_configuration(id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER SEQUENCE ${schema}.sq_zaaktype_cmmn_brp_parameters RENAME TO sq_zaaktype_brp_parameters;
