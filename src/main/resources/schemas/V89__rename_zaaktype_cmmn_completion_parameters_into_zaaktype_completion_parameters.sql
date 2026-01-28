/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE ${schema}.zaaktype_cmmn_completion_parameters RENAME TO zaaktype_completion_parameters;

ALTER SEQUENCE ${schema}.sq_zaaktype_cmmn_completion_parameters RENAME TO sq_zaaktype_completion_parameters;

ALTER TABLE ${schema}.zaaktype_completion_parameters DROP CONSTRAINT fk_zaaktype_cmmn_configuration;

ALTER TABLE ${schema}.zaaktype_completion_parameters
    ADD CONSTRAINT fk_zaaktype_configuration
        FOREIGN KEY (zaaktype_configuration_id)
        REFERENCES ${schema}.zaaktype_configuration(id)
        ON UPDATE CASCADE ON DELETE CASCADE;
