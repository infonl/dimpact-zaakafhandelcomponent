-- SQL script that creates the 'Test zaaktype 4' zaaktype in the Open Zaak database

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
    '2026-07-23', -- datum_begin_geldigheid
    NULL, -- datum_einde_geldigheid
    false, -- concept
    '4f46d270-c4d8-4cfe-a3a1-cb86ae102656', -- uuid (derived from the URL)
    'test-zaaktype-4', -- identificatie
    'Test zaaktype 4', -- zaaktype_omschrijving
    'Test zaaktype 4', -- zaaktype_omschrijving_generiek
    'openbaar', -- vertrouwelijkheidaanduiding
    'Testen afhandelwijze brondatum', -- doel
    'Testen afhandelwijze brondatum', -- aanleiding
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
    'https://selectielijst.openzaak.nl/api/v1/procestypen/c844637e-6393-4202-b030-e1bffb08a9b0', -- selectielijst_procestype
    'Test zaaktype 4', -- referentieproces_naam
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
  '629af8ed-d09b-46a9-961d-72054dc93dcd', -- UUID
  'Niet opgelegd', -- Omschrijving
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/7cb315fb-4f7b-4a43-aca1-e4522e4c73b3', -- Resultaattypeomschrijving
  'Afgehandeld', -- Omschrijving Generiek
  'https://selectielijst.openzaak.nl/api/v1/resultaten/f3fa6648-6cfe-47c5-916f-669e18ea9113', -- Selectielijstklasse
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
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'),
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
        '82070946-aa8f-4e92-b4ba-1a053402e65a', -- UUID
        'Opgelegd - Termijn',
        'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/ce8cf476-0b59-496f-8eee-957a7c6e2506',
        'Verleend',
        'https://selectielijst.openzaak.nl/api/v1/resultaten/7a2728e6-3d77-4b67-9b6d-13e19d39455e',
        'vernietigen',
        'P5Y',
        'termijn',
        '',
        false,
        '',
        '',
        '1Y',
        '',
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'),
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_resultaattype) + 1, -- Adjust ID as needed
  'cb841dcb-c2ad-4142-af98-1de706a82ec1', -- UUID
  'Opgelegd - Eigenschap',
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/ce8cf476-0b59-496f-8eee-957a7c6e2506',
  'Verleend',
  'https://selectielijst.openzaak.nl/api/v1/resultaten/7a2728e6-3d77-4b67-9b6d-13e19d39455e',
  'vernietigen',
  'P5Y',
  'eigenschap',
  'brondatum',
  false,
  '',
  '',
  NULL,
  '',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'),
  '_etag',
  NULL,
  '',
  NULL,
  NULL,
  NULL
);

-- For the fourth JSON object
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
        'f9fba302-16d5-473d-921b-867f5641a688', -- UUID
        'Opgelegd - Ingang besluit',
        'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/ce8cf476-0b59-496f-8eee-957a7c6e2506',
        'Verleend',
        'https://selectielijst.openzaak.nl/api/v1/resultaten/7a2728e6-3d77-4b67-9b6d-13e19d39455e',
        'vernietigen',
        'P5Y',
        'ingangsdatum_besluit',
        '',
        false,
        '',
        '',
        NULL,
        '',
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'),
        '_etag',
        NULL,
        '',
        NULL,
        NULL,
        NULL
    );

-- For the fifth JSON object
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
        '101a1b48-0e74-44c6-be5a-98e5cb06a3ea', -- UUID
        'Opgelegd - Verval besluit',
        'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/ce8cf476-0b59-496f-8eee-957a7c6e2506',
        'Verleend',
        'https://selectielijst.openzaak.nl/api/v1/resultaten/7a2728e6-3d77-4b67-9b6d-13e19d39455e',
        'vernietigen',
        'P5Y',
        'vervaldatum_besluit',
        '',
        false,
        '',
        '',
        NULL,
        '',
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'),
        '_etag',
        NULL,
        '',
        NULL,
        NULL,
        NULL
    );

-- For the sixth JSON object
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
        '68c98532-4608-4eb6-997d-9fcfbf941349', -- UUID
        'Opgelegd - Hoofdzaak',
        'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/ce8cf476-0b59-496f-8eee-957a7c6e2506',
        'Verleend',
        'https://selectielijst.openzaak.nl/api/v1/resultaten/7a2728e6-3d77-4b67-9b6d-13e19d39455e',
        'vernietigen',
        'P5Y',
        'hoofdzaak',
        '',
        false,
        '',
        '',
        NULL,
        '',
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'),
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
  'eed4a04b-1850-4f10-a87d-06ae0d868c7f', -- UUID
  'Intake', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  1, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'), -- Zaaktype ID
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
  '8e856905-4fd6-4718-b3ce-fd1f81b32bd4', -- UUID
  'In behandeling', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  2, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'), -- Zaaktype ID
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
  '92db7bce-646e-461c-b0d0-99725a4a7527', -- UUID
  'Wacht op aanvullende informatie', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  3, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'), -- Zaaktype ID
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
  '5d943629-cfbb-42fb-99be-eac8701fbb06', -- UUID
  'Heropend', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  4, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'), -- Zaaktype ID
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
  '7ae4cf13-cece-4d81-82e0-2448c55f7d6c', -- UUID
  'Afgerond', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  5, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'), -- Zaaktype ID
  '_etag', -- Placeholder
  NULL,            -- doorlooptijd
  NULL,            -- datum_begin_geldigheid
  NULL             -- datum_einde_geldigheid
);


-- PROPERTIES (eigenschappen)
-- no properties are defined for this zaaktype

-- For the first JSON object eigenschap specificatie
INSERT INTO catalogi_eigenschapspecificatie (id, groep, formaat, lengte, kardinaliteit, waardenverzameling) VALUES(100, 'datum', 'datum', '8', '1', '{}');


-- For the first JSON object eigenschap
INSERT INTO catalogi_eigenschap
(
    id,
    uuid,
    eigenschapnaam,
    definitie,
    toelichting,
    specificatie_van_eigenschap_id,
    zaaktype_id,
    _etag,
    statustype_id,
    datum_begin_geldigheid,
    datum_einde_geldigheid
)
VALUES
    (
        (SELECT COALESCE(MAX(id),0) FROM catalogi_eigenschap) + 1, -- Adjust ID as needed
        'b15816d7-1a97-4e98-af35-245258f91465', -- UUID
        'brondatum',-- eigenschapnaam
        'Einddatum van het gebiedsverbod',-- definitie
        '',-- toelichting
        1,-- specificatie_van_eigenschap_id
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'),-- zaaktype_id
        '_etag',-- _etag
        NULL,-- statustype_id
        NULL,-- datum_begin_geldigheid
        NULL-- datum_einde_geldigheid
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
  '06b05af2-2858-4276-a431-83f3a3a69255', -- UUID
  'Melder', -- Omschrijving
  'initiator', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'), -- Zaaktype ID
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
  '1f91eb5e-6eea-45fb-b37a-993609ecfca2', -- UUID
  'Behandelaar', -- Omschrijving
  'behandelaar', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'), -- Zaaktype ID
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
  'd22d7103-db6a-4a14-afcf-57ccfd932a81', -- UUID
  'Belanghebbende', -- Omschrijving
  'belanghebbende', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'), -- Zaaktype ID
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
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'cd099eb3-2084-476c-8c4a-af48d8d1986e', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'efc332f2-be3b-4bad-9e3c-49a6219c92ad'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'), '_etag');


-- Gekoppeld besluittype
INSERT INTO catalogi_besluittype_zaaktypen
(id, besluittype_id, zaaktype_id)
VALUES
    (
        3,
        (SELECT id FROM catalogi_besluittype WHERE omschrijving = 'Besluit na heroverweging'),
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656')
    );


-- add a parent-child (hoofdzaak-deelzaaktype) relation between the two zaaktypes created previously
-- so that we can test the functionality in ZAC to manage hoofdzaak - deelzaak relations
INSERT INTO catalogi_zaaktype_deelzaaktypen (id, from_zaaktype_id, to_zaaktype_id)
VALUES
    (
        2,
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656'),
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '4f46d270-c4d8-4cfe-a3a1-cb86ae102656')
    );
