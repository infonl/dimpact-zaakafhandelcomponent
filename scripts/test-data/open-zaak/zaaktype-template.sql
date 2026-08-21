/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- SQL script that creates a zaaktype in the Open Zaak database

-- note that we currently use the public https://selectielijst.openzaak.nl/ VNG Selectielijst service here
INSERT INTO catalogi_zaaktype
(
    id,
    datum_begin_geldigheid,
    datum_einde_geldigheid,
    concept,
    uuid,
    identificatie,
    zaaktype_omschrijving,
    zaaktype_omschrijving_generiek,
    vertrouwelijkheidaanduiding,
    doel,
    aanleiding,
    toelichting,
    indicatie_intern_of_extern,
    handeling_initiator,
    onderwerp,
    handeling_behandelaar,
    doorlooptijd_behandeling,
    servicenorm_behandeling,
    opschorting_en_aanhouding_mogelijk,
    verlenging_mogelijk,
    verlengingstermijn,
    trefwoorden,
    publicatie_indicatie,
    publicatietekst,
    verantwoordingsrelatie,
    versiedatum,
    producten_of_diensten,
    selectielijst_procestype,
    referentieproces_naam,
    referentieproces_link,
    catalogus_id,
    selectielijst_procestype_jaar,
    _etag,
    verantwoordelijke,
    broncatalogus_domein,
    broncatalogus_rsin,
    broncatalogus_url,
    bronzaaktype_identificatie,
    bronzaaktype_omschrijving,
    bronzaaktype_url
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktype) + 1, -- Assuming auto-increment is not set for id
        '2026-08-01', -- datum_begin_geldigheid
        NULL, -- datum_einde_geldigheid
        false, -- concept
        '[[ZAAKTYPE_UUID]]', -- uuid (derived from the URL)
        'load-test-zaaktype-[[ZAAKTYPE_NUMBER]]', -- identificatie
        'Load test zaaktype [[ZAAKTYPE_NUMBER]]', -- zaaktype_omschrijving
        'Load test zaaktype [[ZAAKTYPE_NUMBER]]', -- zaaktype_omschrijving_generiek
        'openbaar', -- vertrouwelijkheidaanduiding
        'Load test', -- doel
        'Load test', -- aanleiding
        '', -- toelichting
        'extern', -- indicatie_intern_of_extern
        'Indienen', -- handeling_initiator
        'Toezien en handhaven', -- onderwerp
        'Behandelen', -- handeling_behandelaar
        'P14D', -- doorlooptijd_behandeling
        NULL, -- servicenorm_behandeling
        false, -- opschorting_en_aanhouding_mogelijk
        false, -- verlenging_mogelijk
        NULL, -- verlengingstermijn
        '{}', -- trefwoorden (empty array)
        false, -- publicatie_indicatie
        '', -- publicatietekst
        '{}', -- verantwoordingsrelatie (empty array)
        '2023-10-01', -- versiedatum
        '{}', -- producten_of_diensten (empty array)
        'https://selectielijst.openzaak.nl/api/v1/procestypen/7ff2b005-4d84-47fe-983a-732bfa958ff5', -- selectielijst_procestype
        'Load test zaaktype [[ZAAKTYPE_NUMBER]]', -- referentieproces_naam
        '', -- referentieproces_link
        1, -- catalogus_id, assuming a lookup is required
        2020, -- selectielijst_procestype_jaar (assuming this remains constant)
        '_etag', -- _etag (Placeholder, assuming it needs to be generated or provided elsewhere)
        '002564440',    -- verantwoordelijke
        '',            -- broncatalogus_domein
        '',            -- broncatalogus_rsin
        '',            -- broncatalogus_url
        '',            -- bronzaaktype_identificatie
        '',            -- bronzaaktype_omschrijving
        ''             -- bronzaaktype_url
    );


-- RESULTAATTYPES

-- For the first JSON object
INSERT INTO catalogi_resultaattype
(
    id,
    uuid,
    omschrijving,
    resultaattypeomschrijving,
    omschrijving_generiek,
    selectielijstklasse,
    archiefnominatie,
    archiefactietermijn,
    brondatum_archiefprocedure_afleidingswijze,
    brondatum_archiefprocedure_datumkenmerk,
    brondatum_archiefprocedure_einddatum_bekend,
    brondatum_archiefprocedure_objecttype,
    brondatum_archiefprocedure_registratie,
    brondatum_archiefprocedure_procestermijn,
    toelichting,
    zaaktype_id,
    _etag,
    indicatie_specifiek,
    procesobjectaard,
    procestermijn,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        -- Adjust ID as needed
        (SELECT COALESCE(MAX(id),0) FROM catalogi_resultaattype) + 1,
        '[[RESULTAATTYPE_1_UUID]]', -- UUID
        'Geweigerd', -- Omschrijving
        'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/1f750958-431c-4916-bc01-af5d3a753b41', -- Resultaattypeomschrijving
        'Geweigerd', -- Omschrijving Generiek
        'https://selectielijst.openzaak.nl/api/v1/resultaten/f572cb0e-244a-4682-b57e-0c044c468387', -- Selectielijstklasse
        'vernietigen', -- Archiefnominatie
        'P5Y', -- Archiefactietermijn
        'afgehandeld', -- Brondatum Archiefprocedure Afleidingswijze
        '', -- Brondatum Archiefprocedure Datumkenmerk
        false, -- Brondatum Archiefprocedure Einddatum Bekend
        '', -- Brondatum Archiefprocedure Objecttype
        '', -- Brondatum Archiefprocedure Registratie
        NULL, -- Brondatum Archiefprocedure Procestermijn
        '', -- Toelichting
        -- Assuming zaaktype_id needs to be retrieved from the URL, adjust as needed
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'),
        '_etag',
        NULL,
        '',
        NULL,
        NULL,
        NULL
    );

-- For the second JSON object
INSERT INTO catalogi_resultaattype
(
    id,
    uuid,
    omschrijving,
    resultaattypeomschrijving,
    omschrijving_generiek,
    selectielijstklasse,
    archiefnominatie,
    archiefactietermijn,
    brondatum_archiefprocedure_afleidingswijze,
    brondatum_archiefprocedure_datumkenmerk,
    brondatum_archiefprocedure_einddatum_bekend,
    brondatum_archiefprocedure_objecttype,
    brondatum_archiefprocedure_registratie,
    brondatum_archiefprocedure_procestermijn,
    toelichting,
    zaaktype_id,
    _etag,
    indicatie_specifiek,
    procesobjectaard,
    procestermijn,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_resultaattype) + 1, -- Adjust ID as needed
        '[[RESULTAATTYPE_2_UUID]]', -- UUID
        'Verleend',
        'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/f7d2dc14-1b71-4179-aed3-4e7abcfbeb0d',
        'Verleend',
        'https://selectielijst.openzaak.nl/api/v1/resultaten/5038528b-0eb7-4502-a415-a3093987d69b',
        'vernietigen',
        'P1Y',
        'afgehandeld',
        '',
        false,
        '',
        '',
        NULL,
        '',
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'),
        '_etag',
        NULL,
        '',
        NULL,
        NULL,
        NULL
    );


-- STATUSTYPES
-- For the first JSON object
INSERT INTO catalogi_statustype
(
    id,
    uuid,
    statustype_omschrijving,
    statustype_omschrijving_generiek,
    statustypevolgnummer,
    informeren,
    statustekst,
    toelichting,
    zaaktype_id,
    _etag,
    doorlooptijd,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_statustype) + 1, -- Adjust ID as needed
        '[[STATUSTYPE_1_UUID]]', -- UUID
        'Intake', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        1, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'), -- Zaaktype ID
        '_etag', -- Placeholder
        NULL,            -- doorlooptijd
        NULL,            -- datum_begin_geldigheid
        NULL             -- datum_einde_geldigheid
    );

-- For the second JSON object
INSERT INTO catalogi_statustype
(
    id,
    uuid,
    statustype_omschrijving,
    statustype_omschrijving_generiek,
    statustypevolgnummer,
    informeren,
    statustekst,
    toelichting,
    zaaktype_id,
    _etag,
    doorlooptijd,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_statustype) + 1, -- Adjust ID as needed
        '[[STATUSTYPE_2_UUID]]', -- UUID
        'In behandeling', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        2, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'), -- Zaaktype ID
        '_etag', -- Placeholder
        NULL,            -- doorlooptijd
        NULL,            -- datum_begin_geldigheid
        NULL             -- datum_einde_geldigheid
    );

-- For the third JSON object
INSERT INTO catalogi_statustype
(
    id,
    uuid,
    statustype_omschrijving,
    statustype_omschrijving_generiek,
    statustypevolgnummer,
    informeren,
    statustekst,
    toelichting,
    zaaktype_id,
    _etag,
    doorlooptijd,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_statustype) + 1, -- Adjust ID as needed
        '[[STATUSTYPE_3_UUID]]', -- UUID
        'Wacht op aanvullende informatie', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        3, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'), -- Zaaktype ID
        '_etag',    -- _etag (Placeholder)
        NULL,       -- doorlooptijd
        NULL,       -- datum_begin_geldigheid
        NULL        -- datum_einde_geldigheid
    );

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- For the fourth JSON object
INSERT INTO catalogi_statustype
(
    id,
    uuid,
    statustype_omschrijving,
    statustype_omschrijving_generiek,
    statustypevolgnummer,
    informeren,
    statustekst,
    toelichting,
    zaaktype_id,
    _etag,
    doorlooptijd,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_statustype) + 1, -- Adjust ID as needed
        '[[STATUSTYPE_4_UUID]]', -- UUID
        'Heropend', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        4, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'), -- Zaaktype ID
        '_etag',    -- _etag (Placeholder)
        NULL,       -- doorlooptijd
        NULL,       -- datum_begin_geldigheid
        NULL        -- datum_einde_geldigheid
    );

-- For the fifth JSON object
INSERT INTO catalogi_statustype
(
    id,
    uuid,
    statustype_omschrijving,
    statustype_omschrijving_generiek,
    statustypevolgnummer,
    informeren,
    statustekst,
    toelichting,
    zaaktype_id,
    _etag,
    doorlooptijd,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_statustype) + 1, -- Adjust ID as needed
        '[[STATUSTYPE_5_UUID]]', -- UUID
        'Afgerond', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        5, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'), -- Zaaktype ID
        '_etag', -- Placeholder
        NULL,            -- doorlooptijd
        NULL,            -- datum_begin_geldigheid
        NULL             -- datum_einde_geldigheid
    );


-- ROLTYPEN
-- Note that these rol types must be known to ZAC as defined in the 'AardVanRol' Java enum in the ZAC code base.

-- For the first JSON object
INSERT INTO catalogi_roltype
(
    id,
    uuid,
    omschrijving,
    omschrijving_generiek,
    zaaktype_id,
    _etag,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
        '[[ROLTYPE_1_UUID]]', -- UUID
        'Initiator', -- Omschrijving
        'initiator', -- Omschrijving Generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'), -- Zaaktype ID
        '_etag',                          -- _etag (Placeholder)
        NULL,                             -- datum_begin_geldigheid
        NULL                              -- datum_einde_geldigheid
    );

-- For the second JSON object
INSERT INTO catalogi_roltype
(
    id,
    uuid,
    omschrijving,
    omschrijving_generiek,
    zaaktype_id,
    _etag,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
        '[[ROLTYPE_2_UUID]]', -- UUID
        'Behandelaar', -- Omschrijving
        'behandelaar', -- Omschrijving Generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'), -- Zaaktype ID
        '_etag',                          -- _etag (Placeholder)
        NULL,                             -- datum_begin_geldigheid
        NULL                              -- datum_einde_geldigheid
    );

-- For the third JSON object
INSERT INTO catalogi_roltype
(
    id,
    uuid,
    omschrijving,
    omschrijving_generiek,
    zaaktype_id,
    _etag,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
        '[[ROLTYPE_3_UUID]]', -- UUID
        'Belanghebbende', -- Omschrijving
        'belanghebbende', -- Omschrijving Generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'), -- Zaaktype ID
        '_etag',                          -- _etag (Placeholder)
        NULL,                             -- datum_begin_geldigheid
        NULL                              -- datum_einde_geldigheid
    );


--  ZAAKTYPE INFORMATION OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
-- the informatie objecttype `bijlage` is used in the flow of creating a zaak by ZAC from an incoming 'productaanvraag' notification


-- ZAAKTYPEN INFORMATIEOBJECTTYPE
-- e-mail
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
    ((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '[[ZAAKTYPEINFORMATIEOBJECTTYPE_1_UUID]]', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
     (SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'efc332f2-be3b-4bad-9e3c-49a6219c92ad'),
     NULL,
     (SELECT id FROM catalogi_zaaktype WHERE uuid = '[[ZAAKTYPE_UUID]]'), '_etag');
