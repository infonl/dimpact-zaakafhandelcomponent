/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

CREATE TABLE ${schema}.temp_bpmn_procesdefinities_form_keys (
    bpmn_procesdefinitie VARCHAR NOT NULL,
    bpmn_procesdefinitie_versie INTEGER NOT NULL,
    form_key VARCHAR NOT NULL
);

INSERT INTO ${schema}.temp_bpmn_procesdefinities_form_keys
SELECT
    key_,
    version_,
    form_key
FROM (
         SELECT
             key_,
             version_,
             deployment_id_
         FROM flowable.act_re_procdef
         WHERE version_ = (
             SELECT MAX(version_)
             FROM flowable.act_re_procdef AS sub
             WHERE sub.key_ = act_re_procdef.key_
         )
     ) as bpmn_definitions
         INNER JOIN
     (
         SELECT
             name_,
             deployment_id_,
             (regexp_matches(
                     CONVERT_FROM(bytes_, 'UTF8'),
                     'formKey="([^"]+)"',
                     'g'
              ))[1] AS form_key
         FROM flowable.act_ge_bytearray
         WHERE name_ LIKE '%.bpmn'
           AND CONVERT_FROM(bytes_, 'UTF8') LIKE '%formKey%'
     ) as bpmn_form_keys
     ON bpmn_definitions.deployment_id_ = bpmn_form_keys.deployment_id_;

CREATE TABLE ${schema}.bpmn_procesdefinitie_taakformulieren (
    id BIGINT NOT NULL,
    bpmn_procesdefinitie VARCHAR NOT NULL,
    bpmn_procesdefinitie_versie INTEGER NOT NULL,
    naam VARCHAR NOT NULL,
    titel VARCHAR NOT NULL,
    bestandsnaam VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    CONSTRAINT pk_bpmn_procesdefinitie_taakformulieren PRIMARY KEY (id),
    CONSTRAINT un_bpmn_procesdefinitie_taakformulieren UNIQUE (bpmn_procesdefinitie, bpmn_procesdefinitie_versie, naam)
);

CREATE SEQUENCE ${schema}.sq_bpmn_procesdefinitie_taakformulieren
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;

INSERT INTO ${schema}.bpmn_procesdefinitie_taakformulieren
SELECT nextval('${schema}.sq_bpmn_procesdefinitie_taakformulieren'),
       pfk.bpmn_procesdefinitie,
       pfk.bpmn_procesdefinitie_versie,
       pfk.form_key,
       ff.title,
       ff.filename,
       ff.content
FROM ${schema}.temp_bpmn_procesdefinities_form_keys pfk
         INNER JOIN ${schema}.formio_formulier ff
                    ON pfk.form_key = ff.name;

DROP TABLE ${schema}.temp_bpmn_procesdefinities_form_keys;
