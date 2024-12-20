/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
DROP
    TABLE
        IF EXISTS ${schema}.smartdocuments_document_creation_template_group CASCADE;

DROP
    TABLE
        IF EXISTS ${schema}.smartdocuments_document_creation_template CASCADE;

CREATE
    TABLE
        ${schema}.smartdocuments_document_creatie_sjabloon_groep(
            id_sjabloon_groep BIGINT NOT NULL,
            smartdocuments_id VARCHAR NOT NULL,
            naam VARCHAR NOT NULL,
            parent_id BIGINT,
            aanmaakdatum TIMESTAMP WITH TIME ZONE NOT NULL,
            zaakafhandelparameters_id BIGINT NOT NULL,
            informatie_object_type_uuid UUID NOT NULL,
            CONSTRAINT pk_sjabloon_groep PRIMARY KEY(id_sjabloon_groep),
            CONSTRAINT fk_document_creatie_sjabloon_groep_zaakafhandelparameters FOREIGN KEY(zaakafhandelparameters_id) REFERENCES ${schema}.zaakafhandelparameters MATCH SIMPLE ON
            UPDATE
                CASCADE ON
                DELETE
                    CASCADE
        );

CREATE
    SEQUENCE ${schema}.sq_sd_document_creatie_sjabloon_groep
START WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE
    INDEX idx_document_creatie_sjabloon_groep_smartdocuments_id ON
    ${schema}.smartdocuments_document_creatie_sjabloon_groep(smartdocuments_id);

CREATE
    INDEX idx_document_creatie_sjabloon_groep_name ON
    ${schema}.smartdocuments_document_creatie_sjabloon_groep(naam);

CREATE
    INDEX idx_document_creatie_sjabloon_groep_creation_date ON
    ${schema}.smartdocuments_document_creatie_sjabloon_groep(aanmaakdatum);

CREATE
    INDEX idx_document_creation_template_group_zaakafhandelparameters_id ON
    ${schema}.smartdocuments_document_creatie_sjabloon_groep(zaakafhandelparameters_id);

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon_groep.id_sjabloon_groep IS 'Unieke ID voor de sjabloongroep';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon_groep.smartdocuments_id IS 'ID voor de sjabloongroep in SmartDocuments';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon_groep.naam IS 'Naam van de sjabloongroep';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon_groep.parent_id IS 'ID van de parent sjabloongroep (of NULL voor root)';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon_groep.aanmaakdatum IS 'Datum waarop de sjabloongroep in deze tabel is opgeslagen';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon_groep.zaakafhandelparameters_id IS 'ID of the Zaakafhandel parameter';

CREATE
    TABLE
        ${schema}.smartdocuments_document_creatie_sjabloon(
            id_sjabloon BIGINT NOT NULL,
            smartdocuments_id VARCHAR NOT NULL,
            naam VARCHAR NOT NULL,
            aanmaakdatum TIMESTAMP WITH TIME ZONE NOT NULL,
            sjabloon_groep_id BIGINT NOT NULL,
            zaakafhandelparameters_id BIGINT NOT NULL,
            informatie_object_type_uuid UUID NOT NULL,
            CONSTRAINT pk_sjabloon PRIMARY KEY(id_sjabloon),
            CONSTRAINT fk_document_creatie_sjabloon_zaakafhandelparameters FOREIGN KEY(zaakafhandelparameters_id) REFERENCES ${schema}.zaakafhandelparameters(id_zaakafhandelparameters) MATCH SIMPLE ON
            UPDATE
                CASCADE ON
                DELETE
                    CASCADE,
                    CONSTRAINT fk_document_creatie_sjabloon_sjabloon_groep_id FOREIGN KEY(sjabloon_groep_id) REFERENCES ${schema}.smartdocuments_document_creatie_sjabloon_groep MATCH SIMPLE ON
                    UPDATE
                        CASCADE ON
                        DELETE
                            CASCADE
        );

CREATE
    SEQUENCE ${schema}.sq_sd_document_creatie_sjabloon
START WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE
    INDEX idx_document_creatie_sjabloon_smartdocuments_id ON
    ${schema}.smartdocuments_document_creatie_sjabloon(smartdocuments_id);

CREATE
    INDEX idx_document_creatie_sjabloon_name ON
    ${schema}.smartdocuments_document_creatie_sjabloon(naam);

CREATE
    INDEX idx_document_creatie_sjabloon_creatie_date ON
    ${schema}.smartdocuments_document_creatie_sjabloon(aanmaakdatum);

CREATE
    INDEX idx_document_creation_template_zaakafhandelparameters_id ON
    ${schema}.smartdocuments_document_creatie_sjabloon(zaakafhandelparameters_id);

CREATE
    INDEX idx_document_creatie_sjabloon_sjabloon_groep_id ON
    ${schema}.smartdocuments_document_creatie_sjabloon(sjabloon_groep_id);

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon.id_sjabloon IS 'Unieke ID voor de sjabloon';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon.smartdocuments_id IS 'ID voor de sjabloon in SmartDocuments';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon.naam IS 'Naam van de sjabloon';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon.aanmaakdatum IS 'Datum waarop de sjabloon in deze tabel is opgeslagen';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon.zaakafhandelparameters_id IS 'ID of the Zaakafhandel parameter';

COMMENT ON
COLUMN ${schema}.smartdocuments_document_creatie_sjabloon.sjabloon_groep_id IS 'ID van de sjabloongroep waar deze sjabloon deel van uitmaakt';
