/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- The deadline warning windows, the confirmation email parameters, the zaakafzenders and the mailtemplate koppelingen
-- describe the zaak and not the process engine, so they move from the CMMN configuration to the zaaktype
-- configuration. Since V100 every CMMN configuration id is also a zaaktype configuration id, so no row blocks the
-- re-pointed foreign keys.

ALTER TABLE ${schema}.zaaktype_configuration
    ADD COLUMN einddatum_gepland_waarschuwing INTEGER,
    ADD COLUMN uiterlijke_einddatum_afdoening_waarschuwing INTEGER;

UPDATE ${schema}.zaaktype_configuration base
    SET einddatum_gepland_waarschuwing = cmmn.eindatum_gepland_waarschuwing,
        uiterlijke_einddatum_afdoening_waarschuwing = cmmn.uiterlijke_einddatum_afdoening_waarschuwing
    FROM ${schema}.zaaktype_cmmn_configuration cmmn
    WHERE cmmn.id = base.id;

ALTER TABLE ${schema}.zaaktype_cmmn_configuration
    DROP COLUMN eindatum_gepland_waarschuwing,
    DROP COLUMN uiterlijke_einddatum_afdoening_waarschuwing;

ALTER TABLE ${schema}.zaaktype_cmmn_email_parameters RENAME TO zaaktype_email_parameters;
ALTER TABLE ${schema}.zaaktype_email_parameters
    DROP CONSTRAINT fk_zaaktype_cmmn_configuration,
    ADD CONSTRAINT fk_zaaktype_configuration FOREIGN KEY (zaaktype_configuration_id)
        REFERENCES ${schema}.zaaktype_configuration (id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ${schema}.zaaktype_email_parameters
    RENAME CONSTRAINT un_zaaktype_cmmn_email_parameters_zaaktype_configuration
    TO un_zaaktype_email_parameters_zaaktype_configuration;
ALTER SEQUENCE ${schema}.sq_zaaktype_cmmn_email_parameters RENAME TO sq_zaaktype_email_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_zaakafzender_parameters RENAME TO zaaktype_zaakafzender_parameters;
ALTER TABLE ${schema}.zaaktype_zaakafzender_parameters
    DROP CONSTRAINT fk_zaaktype_cmmn_configuration,
    ADD CONSTRAINT fk_zaaktype_configuration FOREIGN KEY (zaaktype_configuration_id)
        REFERENCES ${schema}.zaaktype_configuration (id)
        MATCH SIMPLE ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER SEQUENCE ${schema}.sq_zaaktype_cmmn_zaakafzender_parameters RENAME TO sq_zaaktype_zaakafzender_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_mailtemplate_parameters RENAME TO zaaktype_mailtemplate_parameters;
ALTER TABLE ${schema}.zaaktype_mailtemplate_parameters
    DROP CONSTRAINT fk_zaaktype_cmmn_configuration,
    ADD CONSTRAINT fk_zaaktype_configuration FOREIGN KEY (zaaktype_configuration_id)
        REFERENCES ${schema}.zaaktype_configuration (id)
        MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER SEQUENCE ${schema}.sq_zaaktype_cmmn_mailtemplate_parameters RENAME TO sq_zaaktype_mailtemplate_parameters;
