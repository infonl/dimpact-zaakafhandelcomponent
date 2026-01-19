-- SQL script that creates the 'Test zaaktype 1' zaaktype in the Open Zaak database

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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktype) + 1,
        '2025-07-01', -- datum_begin_geldigheid
        NULL,         -- datum_einde_geldigheid
        false,         -- concept
        '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a', -- uuid
        'zaaktype-test-1', -- identificatie
        'Test zaaktype 1', -- zaaktype_omschrijving
        'Test zaaktype 1', -- zaaktype_omschrijving_generiek
        'openbaar',   -- vertrouwelijkheidaanduiding
        'Test zaaktype 1', -- doel
        'Test zaaktype 1', -- aanleiding
        '',           -- toelichting
        'extern',     -- indicatie_intern_of_extern
        'Melden',     -- handeling_initiator
        'Openbare orde & veiligheid', -- onderwerp
        'Behandelen', -- handeling_behandelaar
        'P14D',       -- doorlooptijd_behandeling
        NULL,         -- servicenorm_behandeling
        false,        -- opschorting_en_aanhouding_mogelijk
        false,        -- verlenging_mogelijk
        NULL,         -- verlengingstermijn
        '{}',         -- trefwoorden
        false,        -- publicatie_indicatie
        '',           -- publicatietekst
        '{}',         -- verantwoordingsrelatie
        '2025-07-01', -- versiedatum
        '{}',         -- producten_of_diensten
        'https://selectielijst.openzaak.nl/api/v1/procestypen/7ff2b005-4d84-47fe-983a-732bfa958ff5', -- selectielijst_procestype
        'melding klein evenement', -- referentieproces_naam
        '',           -- referentieproces_link
        1,           -- catalogus_id
        2020,           -- selectielijst_procestype_jaar
        '_etag',       -- _etag,
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_resultaattype) + 1,
        '51fd9ca2-3f7c-424e-ab1e-5963cb82a134',
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
        'Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen',
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_resultaattype) + 1,
        'f940861c-f8f8-4e45-8317-a6175561af0a',
        'Geweigerd',
        'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/1f750958-431c-4916-bc01-af5d3a753b41',
        'Geweigerd',
        'https://selectielijst.openzaak.nl/api/v1/resultaten/f572cb0e-244a-4682-b57e-0c044c468387',
        'vernietigen',
        'P5Y',
        'afgehandeld',
        '',
        false,
        '',
        '',
        NULL,
        'Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen',
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',
        NULL,
        '',
        NULL,
        NULL,
        NULL
    );

-- For the third JSON object
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_resultaattype) + 1,
        '31f56eaa-4515-437e-a3ab-9f7f71e8ee6f',
        'Afgebroken',
        'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/ce8cf476-0b59-496f-8eee-957a7c6e2506',
        'Afgebroken',
        'https://selectielijst.openzaak.nl/api/v1/resultaten/0d978967-6bf2-452f-951a-c16bff338f42',
        'vernietigen',
        'P1Y',
        'afgehandeld',
        '',
        false,
        '',
        '',
        NULL,
        'Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen.',
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
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
        'a10045ce-eef9-4ff6-bc13-a8f22f5222a1', -- uuid
        'Afgerond', -- statustype_omschrijving
        '',         -- statustype_omschrijving_generiek
        11,         -- statustypevolgnummer
        false,      -- informeren
        '',         -- statustekst
        '',         -- toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',    -- _etag (Placeholder)
        NULL,       -- doorlooptijd
        NULL,       -- datum_begin_geldigheid
        NULL        -- datum_einde_geldigheid
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
        'c937fc2b-49bb-4aab-a51c-62016d595f58', -- UUID
        'Wacht op aanvullende informatie', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        10, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
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
        '214353e0-74da-40db-8ac5-c97bc6a308db', -- uuid
        'Heropend',  -- statustype_omschrijving
        '',         -- statustype_omschrijving_generiek
        9,          -- statustypevolgnummer
        false,      -- informeren
        '',         -- statustekst
        '',         -- toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',    -- _etag (Placeholder)
        NULL,       -- doorlooptijd
        NULL,       -- datum_begin_geldigheid
        NULL        -- datum_einde_geldigheid
    );

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
        'd1157a43-3ba9-4666-b0b2-62c71189b4be', -- uuid
        'In behandeling', -- statustype_omschrijving
        '',              -- statustype_omschrijving_generiek
        8,               -- statustypevolgnummer
        false,           -- informeren
        '',              -- statustekst
        '',              -- toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',         -- _etag (Placeholder)
        NULL,            -- doorlooptijd
        NULL,            -- datum_begin_geldigheid
        NULL             -- datum_einde_geldigheid
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
        '5d15d3f8-edef-44ef-8374-c711a4307ed8', -- uuid
        'Intake',       -- statustype_omschrijving
        '',             -- statustype_omschrijving_generiek
        7,              -- statustypevolgnummer
        false,          -- informeren
        '',             -- statustekst
        '',             -- toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',         -- _etag (Placeholder)
        NULL,            -- doorlooptijd
        NULL,            -- datum_begin_geldigheid
        NULL             -- datum_einde_geldigheid
    );

-- PROPERTIES (eigenschappen)
-- no properties are defined for this zaaktype

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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,                                -- id (Placeholder; adjust as needed)
        '27aafb8e-e14d-41cd-964c-441b65f80c8a', -- uuid
        'Melder',                      -- omschrijving
        'initiator',                      -- omschrijving_generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,                                -- id (Placeholder; adjust as needed)
        '4937f242-dd6f-4c96-85ff-fa4098e8316e', -- uuid
        'Behandelaar',                    -- omschrijving
        'behandelaar',                    -- omschrijving_generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,                                -- id (Placeholder; adjust as needed)
        'd10797e4-d06c-40b4-89dc-c6fc874a5501', -- uuid
        'Belanghebbende',                     -- omschrijving
        'belanghebbende',                 -- omschrijving_generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',                          -- _etag (Placeholder)
        NULL,                             -- datum_begin_geldigheid
        NULL                              -- datum_einde_geldigheid
    );

-- For the 4th JSON object
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,                                -- id (Placeholder; adjust as needed)
        '250a172f-e454-4cd0-87eb-eceb84f29cf6', -- uuid
        'Medeaanvrager',                     -- omschrijving
        'mede_initiator',                 -- omschrijving_generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',                          -- _etag (Placeholder)
        NULL,                             -- datum_begin_geldigheid
        NULL                              -- datum_einde_geldigheid
    );

-- For the 5th JSON object
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,                                -- id (Placeholder; adjust as needed)
        '1843230c-e333-472b-8cf3-36ba810561f1', -- uuid
        'Contactpersoon',                     -- omschrijving
        'belanghebbende',                 -- omschrijving_generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',                          -- _etag (Placeholder)
        NULL,                             -- datum_begin_geldigheid
        NULL                              -- datum_einde_geldigheid
    );

-- For the 6th JSON object
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,                                -- id (Placeholder; adjust as needed)
        '63cf63bc-ad0b-4239-a927-65f6e2c9268c', -- uuid
        'Gemachtigde',                     -- omschrijving
        'belanghebbende',                 -- omschrijving_generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',                          -- _etag (Placeholder)
        NULL,                             -- datum_begin_geldigheid
        NULL                              -- datum_einde_geldigheid
    );

-- For the 7th JSON object
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,                                -- id (Placeholder; adjust as needed)
        '77ab428e-9a18-4fdb-98d4-a894028164e4', -- uuid
        'Plaatsvervanger',                     -- omschrijving
        'belanghebbende',                 -- omschrijving_generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',                          -- _etag (Placeholder)
        NULL,                             -- datum_begin_geldigheid
        NULL                              -- datum_einde_geldigheid
    );

-- For the 8th JSON object
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
        (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,                                -- id (Placeholder; adjust as needed)
        '5678bb11-591f-4fe9-9bee-ebf2363d6dfc', -- uuid
        'Bewindvoerder',                     -- omschrijving
        'belanghebbende',                 -- omschrijving_generiek
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag',                          -- _etag (Placeholder)
        NULL,                             -- datum_begin_geldigheid
        NULL                              -- datum_einde_geldigheid
    );

-- INFORMATIE OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
-- the informatie objecttype `bijlage` is used in the flow of creating a zaak by ZAC from an incoming 'productaanvraag' notification
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie) VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2021-10-04', NULL, false, 'ca993ed7-5da1-4b4c-acad-ba64978acefc', 'e-mail', 'zaakvertrouwelijk', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie) VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2021-10-04', NULL, false, '4a689f8a-11d3-4ddd-ae26-00fb258305a5', 'bijlage', 'zaakvertrouwelijk', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- ZAAK INFORMATIE OBJECT TYPES
INSERT INTO catalogi_zaaktypeinformatieobjecttype (id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag) VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '683f36bf-27e4-4138-8d62-6643547ffd1a', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', (SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'ca993ed7-5da1-4b4c-acad-ba64978acefc'), NULL, (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');
INSERT INTO catalogi_zaaktypeinformatieobjecttype (id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag) VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '3cdd33c3-941a-457f-9b16-2dfe5e2cff14', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', (SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '4a689f8a-11d3-4ddd-ae26-00fb258305a5'), NULL, (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');
