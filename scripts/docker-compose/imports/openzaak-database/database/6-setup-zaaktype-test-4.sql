-- SQL script that creates the 'Test zaaktype 4' zaaktype in the Open Zaak database
--
-- This zaaktype is missing:
--    * email zaak information object
--    * initiator role
--    * behandelaar role

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
  '2023-09-21', -- datum_begin_geldigheid
  NULL,         -- datum_einde_geldigheid
  false,         -- concept
  'bba94114-4a7f-42f3-8242-de47cd1d7412', -- uuid
  'test-zaaktype-4', -- identificatie
  'Test zaaktype 4', -- zaaktype_omschrijving
  'Test zaaktype 4', -- zaaktype_omschrijving_generiek
  'openbaar',   -- vertrouwelijkheidaanduiding
  'Test zaaktype 4', -- doel
  'Test zaaktype 4', -- aanleiding
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
  '2023-09-21', -- versiedatum
  '{}',         -- producten_of_diensten
  'https://selectielijst.openzaak.nl/api/v1/procestypen/94dc0901-1e63-44f3-9814-fb6d5bed8e2c', -- selectielijst_procestype
  'Test zaaktype 4', -- referentieproces_naam
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
  '3800b5aa-b411-4c2a-853c-3f74421be799',
  'Verleend',
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/6db8ab4c-521c-466e-a431-004ff27d3e02',
  'Verleend',
  'https://selectielijst.openzaak.nl/api/v1/resultaten/62d04b16-26f6-44e2-8a88-8961a8baac70',
  'vernietigen',
  'P1Y',
  'afgehandeld',
  '',
  false,
  '',
  '',
  NULL,
  'Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  '888f5e24-aa68-4f56-9661-167704f38c4e',
  'Geweigerd',
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/30b1bbca-8d16-4b83-8e29-7f7d1a40d039',
  'Geweigerd',
  'https://selectielijst.openzaak.nl/api/v1/resultaten/ba71a1cc-68f2-4079-a08a-6114a17f5334',
  'vernietigen',
  'P5Y',
  'afgehandeld',
  '',
  false,
  '',
  '',
  NULL,
  'Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  'a8ce52b6-eab4-430b-b8ea-d8a33e387bbc',
  'Afgebroken',
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/b17e4b8a-58d9-482f-a00d-59e29a33ad09',
  'Afgebroken',
  'https://selectielijst.openzaak.nl/api/v1/resultaten/f944e903-708d-4e4f-b167-7402b3e94b32',
  'vernietigen',
  'P1Y',
  'afgehandeld',
  '',
  false,
  '',
  '',
  NULL,
  'Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen.',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  'bdb56337-fe07-4780-8da1-4f1fc14f8ff4', -- UUID
  'test-resultaat-eigenschap', -- Omschrijving
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/61531b41-d81c-4257-9543-8580c74a93a0', -- Resultaattypeomschrijving
  'Afgebroken', -- Omschrijving Generiek
  'https://selectielijst.openzaak.nl/api/v1/resultaten/55b399a3-78a5-4503-8165-b0f83a9fc47e', -- Selectielijstklasse
  'vernietigen', -- Archiefnominatie
  'P1Y', -- Archiefactietermijn
  'eigenschap', -- Brondatum Archiefprocedure Afleidingswijze
  'test-eigenschap', -- Brondatum Archiefprocedure Datumkenmerk
  false, -- Brondatum Archiefprocedure Einddatum Bekend
  '', -- Brondatum Archiefprocedure Objecttype
  '', -- Brondatum Archiefprocedure Registratie
  NULL, -- Brondatum Archiefprocedure Procestermijn
  'test-resultaat-eigenschap', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  '155c8b94-16f4-469f-bc7e-67ebe1ffae36', -- uuid
  'Afgerond', -- statustype_omschrijving
  '',         -- statustype_omschrijving_generiek
  11,         -- statustypevolgnummer
  false,      -- informeren
  '',         -- statustekst
  '',         -- toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
        '4157c119-a405-48e1-bb0f-e056365235a7', -- UUID
        'Wacht op aanvullende informatie', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        10, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  'dd42f130-a355-45b2-8f3d-075626e72401', -- uuid
  'Heropend',  -- statustype_omschrijving
  '',         -- statustype_omschrijving_generiek
  9,          -- statustypevolgnummer
  false,      -- informeren
  '',         -- statustekst
  '',         -- toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  'e261b252-4334-42e9-91e0-0479e71bc5eb', -- uuid
  'In behandeling', -- statustype_omschrijving
  '',              -- statustype_omschrijving_generiek
  8,               -- statustypevolgnummer
  false,           -- informeren
  '',              -- statustekst
  '',              -- toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  '44a48811-1704-44b4-91f7-6fa9552ec24c', -- uuid
  'Intake',       -- statustype_omschrijving
  '',             -- statustype_omschrijving_generiek
  7,              -- statustypevolgnummer
  false,          -- informeren
  '',             -- statustekst
  '',             -- toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
  '_etag',         -- _etag (Placeholder)
  NULL,            -- doorlooptijd
  NULL,            -- datum_begin_geldigheid
  NULL             -- datum_einde_geldigheid
);

-- PROPERTIES (eigenschappen)

-- For the first JSON object eigenschap specificatie
INSERT INTO catalogi_eigenschapspecificatie (id, groep, formaat, lengte, kardinaliteit, waardenverzameling)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_eigenschapspecificatie) + 1, 'test eigenschap groep', 'datum', 8, 1, '{}');


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
  'a3a4f5ee-561b-4bce-a341-b34f9d966d27', -- UUID
  'test-eigenschap',-- eigenschapnaam
  'test-eigenschap',-- definitie
  '',-- toelichting
  1,-- specificatie_van_eigenschap_id
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'),-- zaaktype_id
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,                                -- id (Placeholder; adjust as needed)
  '438152d2-aff0-411e-82f6-6dc79a1bc2e1', -- uuid
  'Adviseur',                      -- omschrijving
  'adviseur',               -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  '52291c49-8444-466a-9ebc-098b9d76ceb2', -- uuid
  'Zaakcoördinator',               -- omschrijving
  'zaakcoordinator',        -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  '117f2335-a4e1-44d3-aed3-554eaae9ea71', -- uuid
  'Belanghebbende',                 -- omschrijving
  'belanghebbende',         -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  '35ac86e6-2dab-47c5-bbdd-907509c60aa0', -- uuid
  'Medeaanvrager',                 -- omschrijving
  'mede_initiator',        -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  'e6d7fc21-cf97-4e30-aa18-7932b446ead3', -- uuid
  'Contactpersoon',                -- omschrijving
  'belanghebbende',        -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  '5bb07cc9-27f8-4a06-8b3f-2aa77c6e6240', -- uuid
  'Gemachtigde',                    -- omschrijving
  'belanghebbende',         -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  '708cc714-1d4e-43df-a4ba-98b4f4be20ea', -- uuid
  'Plaatsvervanger',                -- omschrijving
  'belanghebbende',         -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
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
  '338378bf-42e7-4c99-be5c-719c45272660', -- uuid
  'Bewindvoerder',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'bba94114-4a7f-42f3-8242-de47cd1d7412'), -- Zaaktype ID
  '_etag',                          -- _etag (Placeholder)
  NULL,                             -- datum_begin_geldigheid
  NULL                              -- datum_einde_geldigheid
);

-- INFORMATIE OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2021-10-04', NULL, false, '93821925-cb44-44ec-a52b-c70ceb7c3f7d', 'e-mail', 'zaakvertrouwelijk', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- ZAAK INFORMATIE OBJECT TYPES
INSERT INTO catalogi_zaaktypeinformatieobjecttype (id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'b902ed90-3621-41c9-836d-70506060c4e5', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', (SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '93821925-cb44-44ec-a52b-c70ceb7c3f7d'), NULL, 1, '_etag');

