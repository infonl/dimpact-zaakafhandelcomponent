/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
-- zaaktype_bpmn_process_definition
ALTER TABLE ${schema}.zaaktype_bpmn_process_definition RENAME TO zaaktype_bpmn_configuration;

ALTER TABLE ${schema}.zaaktype_bpmn_configuration
    RENAME COLUMN naam_groep TO group_id;

-- zaakafhandelparameters
ALTER TABLE ${schema}.zaakafhandelparameters RENAME TO zaaktype_cmmn_configuration;

ALTER TABLE ${schema}.zaaktype_cmmn_configuration
    RENAME COLUMN id_zaakafhandelparameters TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_configuration
    RENAME COLUMN uuid_zaaktype TO zaaktype_uuid;

ALTER TABLE ${schema}.zaaktype_cmmn_configuration
    RENAME COLUMN id_groep TO groep_id;

-- zaakbeeindigparameter
ALTER TABLE ${schema}.zaakbeeindigparameter RENAME TO zaaktype_cmmn_completion_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_completion_parameters
    RENAME COLUMN id_zaakbeeindigparameter TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_completion_parameters
    RENAME COLUMN id_zaakafhandelparameters TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_completion_parameters
    RENAME CONSTRAINT fk_beeindigparameter_afhandelparameters TO fk_zaaktype_cmmn_configuration;

-- humantask_parameters
ALTER TABLE ${schema}.humantask_parameters RENAME TO zaaktype_cmmn_humantask_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_humantask_parameters
    RENAME COLUMN id_humantask_parameters TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_humantask_parameters
    RENAME COLUMN id_zaakafhandelparameters TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_humantask_parameters
    RENAME CONSTRAINT fk_humantask_parameters_zaakafhandelparameters TO fk_zaaktype_cmmn_configuration;

-- mail_template_koppelingen
ALTER TABLE ${schema}.mail_template_koppelingen RENAME TO zaaktype_cmmn_mailtemplate_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_mailtemplate_parameters
    RENAME COLUMN id_mail_template_koppelingen TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_mailtemplate_parameters
    RENAME COLUMN id_zaakafhandelparameters TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_mailtemplate_parameters
    RENAME CONSTRAINT fk_mail_template_koppelingen_zaakafhandelparameters TO fk_zaaktype_cmmn_configuration;

-- usereventlistener_parameters
ALTER TABLE ${schema}.usereventlistener_parameters RENAME TO zaaktype_cmmn_usereventlistener_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_usereventlistener_parameters
    RENAME COLUMN id_usereventlistener_parameters TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_usereventlistener_parameters
    RENAME COLUMN id_zaakafhandelparameters TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_usereventlistener_parameters
    RENAME CONSTRAINT fk_usereventlistener_parameters_zaakafhandelparameters TO fk_zaaktype_cmmn_configuration;

-- zaakafzender
ALTER TABLE ${schema}.zaakafzender RENAME TO zaaktype_cmmn_zaakafzender_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_zaakafzender_parameters
    RENAME COLUMN id_zaakafzender TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_zaakafzender_parameters
    RENAME COLUMN id_zaakafhandelparameters TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_zaakafzender_parameters
    RENAME CONSTRAINT fk_zaakafzender_zaakafhandelparameters TO fk_zaaktype_cmmn_configuration;

-- betrokkene_koppelingen
ALTER TABLE ${schema}.betrokkene_koppelingen RENAME TO zaaktype_cmmn_betrokkene_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_betrokkene_parameters
    RENAME COLUMN id_betrokkene_koppelingen TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_betrokkene_parameters
    RENAME COLUMN id_zaakafhandelparameters TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_betrokkene_parameters
    RENAME CONSTRAINT fk_betrokkene_koppelingen_zaakafhandelparameters TO fk_zaaktype_cmmn_configuration;

-- brp_doelbindingen
ALTER TABLE ${schema}.brp_doelbindingen RENAME TO zaaktype_cmmn_brp_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_brp_parameters
    RENAME COLUMN id_brp_doelbindingen TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_brp_parameters
    RENAME COLUMN id_zaakafhandelparameters TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_brp_parameters
    RENAME CONSTRAINT fk_brp_doelbindingen_zaakafhandelparameters TO fk_zaaktype_cmmn_configuration;

-- automatic_email_confirmation
ALTER TABLE ${schema}.automatic_email_confirmation RENAME TO zaaktype_cmmn_email_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_email_parameters
    RENAME COLUMN id_automatic_email_confirmation TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_email_parameters
    RENAME COLUMN id_zaakafhandelparameters TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_email_parameters
    RENAME CONSTRAINT fk_automatic_email_confirmation_zaakafhandelparameters TO fk_zaaktype_cmmn_configuration;

-- smartdocuments_document_creatie_sjabloon_groep
ALTER TABLE ${schema}.smartdocuments_document_creatie_sjabloon_groep RENAME TO zaaktype_cmmn_smartdocuments_document_template_group_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_smartdocuments_document_template_group_parameters
    RENAME COLUMN id_sjabloon_groep TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_smartdocuments_document_template_group_parameters
    RENAME COLUMN zaakafhandelparameters_id TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_smartdocuments_document_template_group_parameters
    RENAME CONSTRAINT fk_document_creatie_sjabloon_groep_zaakafhandelparameters TO fk_zaaktype_cmmn_configuration;

-- smartdocuments_document_creatie_sjabloon
ALTER TABLE ${schema}.smartdocuments_document_creatie_sjabloon RENAME TO zaaktype_cmmn_smartdocuments_document_template_parameters;

ALTER TABLE ${schema}.zaaktype_cmmn_smartdocuments_document_template_parameters
    RENAME COLUMN id_sjabloon TO id;

ALTER TABLE ${schema}.zaaktype_cmmn_smartdocuments_document_template_parameters
    RENAME COLUMN zaakafhandelparameters_id TO zaaktype_configuration_id;

ALTER TABLE ${schema}.zaaktype_cmmn_smartdocuments_document_template_parameters
    RENAME CONSTRAINT fk_document_creatie_sjabloon_zaakafhandelparameters TO fk_zaaktype_cmmn_configuration;

ALTER TABLE ${schema}.zaaktype_cmmn_smartdocuments_document_template_parameters
    RENAME CONSTRAINT fk_document_creatie_sjabloon_sjabloon_groep_id TO fk_smartdocuments_document_template_parameters_id;

-- 1) sq_zaaktype_bpmn_process_definition -> sq_zaaktype_bpmn_configuration
ALTER SEQUENCE ${schema}.sq_zaaktype_bpmn_process_definition
    RENAME TO sq_zaaktype_bpmn_configuration;

-- 2) sq_zaakafhandelparameters -> sq_zaaktype_cmmn_configuration
ALTER SEQUENCE ${schema}.sq_zaakafhandelparameters
    RENAME TO sq_zaaktype_cmmn_configuration;

-- 3) sq_zaakbeeindigparameter -> sq_zaaktype_cmmn_completion_parameters
ALTER SEQUENCE ${schema}.sq_zaakbeeindigparameter
    RENAME TO sq_zaaktype_cmmn_completion_parameters;

-- 4) sq_humantask_parameters -> sq_zaaktype_cmmn_humantask_parameters
ALTER SEQUENCE ${schema}.sq_humantask_parameters
    RENAME TO sq_zaaktype_cmmn_humantask_parameters;

-- 5) sq_mail_template_koppelingen -> sq_zaaktype_cmmn_mailtemplate_parameters
ALTER SEQUENCE ${schema}.sq_mail_template_koppelingen
    RENAME TO sq_zaaktype_cmmn_mailtemplate_parameters;

-- 6) sq_usereventlistener_parameters -> sq_zaaktype_cmmn_usereventlistener_parameters
ALTER SEQUENCE ${schema}.sq_usereventlistener_parameters
    RENAME TO sq_zaaktype_cmmn_usereventlistener_parameters;

-- 7) sq_zaakafzender -> sq_zaaktype_cmmn_zaakafzender_parameters
ALTER SEQUENCE ${schema}.sq_zaakafzender
    RENAME TO sq_zaaktype_cmmn_zaakafzender_parameters;

-- 8) sq_betrokkene_koppelingen -> sq_zaaktype_cmmn_betrokkene_parameters
ALTER SEQUENCE ${schema}.sq_betrokkene_koppelingen
    RENAME TO sq_zaaktype_cmmn_betrokkene_parameters;

-- 9) sq_brp_doelbindingen -> sq_zaaktype_cmmn_brp_parameters
ALTER SEQUENCE ${schema}.sq_brp_doelbindingen
    RENAME TO sq_zaaktype_cmmn_brp_parameters;

-- 10) sq_automatic_email_confirmation -> sq_zaaktype_cmmn_email_parameters
ALTER SEQUENCE ${schema}.sq_automatic_email_confirmation
    RENAME TO sq_zaaktype_cmmn_email_parameters;

ALTER SEQUENCE ${schema}.sq_sd_document_creatie_sjabloon_groep
    RENAME TO sq_zaaktype_cmmn_smartdocuments_document_template_group_parameters;

ALTER SEQUENCE ${schema}.sq_sd_document_creatie_sjabloon
    RENAME TO sq_zaaktype_cmmn_smartdocuments_document_template_parameters;