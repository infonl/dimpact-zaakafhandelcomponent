-- SQL script that creates the 'BPMN test zaaktype 2' zaaktype in the Open Zaak database

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
    '2025-01-01', -- datum_begin_geldigheid
    NULL, -- datum_einde_geldigheid
    false, -- concept
    '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e', -- uuid (derived from the URL)
    'bpmn-test-zaaktype-2', -- identificatie
    'BPMN test zaaktype 2', -- zaaktype_omschrijving
    'BPMN test zaaktype 2', -- zaaktype_omschrijving_generiek
    'openbaar', -- vertrouwelijkheidaanduiding
    'BPMN test zaaktype 2', -- doel
    'BPMN test zaaktype 2', -- aanleiding
    '', -- toelichting
    'extern', -- indicatie_intern_of_extern
    'Indienen', -- handeling_initiator
    'Schade en aansprakelijkheid', -- onderwerp
    'Behandelen', -- handeling_behandelaar
    'P30D', -- doorlooptijd_behandeling
    NULL, -- servicenorm_behandeling
    true, -- opschorting_en_aanhouding_mogelijk
    true, -- verlenging_mogelijk
    'P1M', -- verlengingstermijn
    '{}', -- trefwoorden (empty array)
    false, -- publicatie_indicatie
    '', -- publicatietekst
    '{}', -- verantwoordingsrelatie (empty array)
    '2025-01-01', -- versiedatum
    '{}', -- producten_of_diensten (empty array)
    'https://selectielijst.openzaak.nl/api/v1/procestypen/1e12ad30-b900-4e7f-b3b7-569673cee0b0', -- selectielijst_procestype
    'BPMN test zaaktype 2', -- referentieproces_naam
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
    (SELECT COALESCE(MAX(id),0) FROM catalogi_resultaattype) + 1, -- Adjust ID as needed
    '4f9da4cd-a910-4f85-98ca-adb33e215f43', -- UUID
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
    'fakeToelichtingAfgebroken',
    (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'),
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
    '0424b087-e83b-41d3-9484-8796ac441f70', -- UUID
    'Verleend',
    'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/f7d2dc14-1b71-4179-aed3-4e7abcfbeb0d',
    'Verleend',
    'https://selectielijst.openzaak.nl/api/v1/resultaten/85c22ea4-b276-456b-8a80-d03c5c3795ba',
    'vernietigen',
    'P1Y',
    'afgehandeld',
    '',
    false,
    '',
    '',
    NULL,
    'fakeToelichtingVerleend',
    (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  'cfe60e27-3dc4-4542-b7d6-383304281224', -- UUID
  'Afgerond', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  12, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  '623363af-e32c-4e20-8041-0ab0699a6263', -- UUID
  'Wacht op aanvullende informatie', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  11, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  '560f62fa-2860-4cf4-b792-17321baaeaef', -- UUID 
  'Onderzoek afgerond', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  10, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  'fa3395eb-8060-4394-9324-b5f7b335b9c5', -- UUID
  'Heropend', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  9, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
  '_etag',    -- _etag (Placeholder)
  NULL,       -- doorlooptijd
  NULL,       -- datum_begin_geldigheid
  NULL        -- datum_einde_geldigheid
);

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
  '35effce0-3571-4382-9a6d-53533a425765', -- UUID
  'In behandeling', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  8, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
  '_etag', -- Placeholder
  NULL,            -- doorlooptijd
  NULL,            -- datum_begin_geldigheid
  NULL             -- datum_einde_geldigheid
);

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
  '71debca7-503b-4747-88af-9350ec8069dd', -- UUID
  'Intake', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  7, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
  '_etag',    -- _etag (Placeholder)
  NULL,       -- doorlooptijd
  NULL,       -- datum_begin_geldigheid
  NULL        -- datum_einde_geldigheid
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '61f753dc-d544-4333-82d4-f5e7e7ae5492', -- UUID
  'Melder', -- Omschrijving
  'initiator', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  '04703163-3daf-4dd8-a084-d7d2d33cf86b', -- UUID
  'Behandelaar', -- Omschrijving
  'behandelaar', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  '3dcc3837-f4f9-4333-a005-c0739c527780', -- UUID
  'Belanghebbende',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '56dfa069-cf16-483e-b173-d98fc76dde70', -- uuid 
  'Medeaanvrager',                     -- omschrijving
  'mede_initiator',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '94241ad8-2f61-4213-977b-0b7f73521b98', -- uuid 
  'Contactpersoon',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '3c2e9c67-8836-4659-90a4-92c7ac03484e', -- uuid
  'Gemachtigde',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '960a66ad-9f13-4adc-917a-2e9b85f40959', -- uuid 
  'Plaatsvervanger',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '1704168e-6660-450e-a3c9-298e31a9a41c', -- uuid 
  'Bewindvoerder',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), -- Zaaktype ID
  '_etag',                          -- _etag (Placeholder)
  NULL,                             -- datum_begin_geldigheid
  NULL                              -- datum_einde_geldigheid
);


--  ZAAKTYPE 2 INFORMATION OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
-- the informatie objecttype `bijlage` is used in the flow of creating a zaak by ZAC from an incoming 'productaanvraag' notification

-- Factuur
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1 , '2023-11-22', NULL, false, '5b77bc76-1c43-4408-8281-1b12f16b76c3', 'factuur', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Ontvangstbevestiging
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1 , '2023-11-22', NULL, false, 'f7aae4f0-19f7-415a-bb14-9027f9c55507', 'ontvangstbevestiging', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Brief
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '32e06e3a-8794-4b36-8965-99d4d54c6496', 'brief', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Bewijs
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'e8117a18-6854-4337-8436-c28d45c99316', 'bewijs', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Afbeelding
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '88a17f0e-07dd-462f-af36-740a58e54ccd', 'afbeelding', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Advies
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'c62636ae-4c46-4b22-a0e3-3cdd1ade094e', 'advies', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');


-- ZAAKTYPEN INFORMATIEOBJECTTYPE
-- e-mail
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'fae0a3c8-2363-4a43-9083-56b84d57caaa', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'efc332f2-be3b-4bad-9e3c-49a6219c92ad'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), '_etag');

-- bijlage
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '40b32d0d-2a97-4aba-8a4e-78cb8be2c952', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b1933137-94d6-49bc-9e12-afe712512276'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), '_etag');

-- factuur
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '73d3f3ed-7c51-4b68-8b3b-8e74cfb77185', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'a8dfd5b8-8657-48bf-b624-f962709f6e19'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), '_etag');

-- ontvangstbevestiging
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'd0550419-acab-4bd3-b6e8-cc8f04107557', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '12dbb9de-6b5c-4649-b9f3-06e6190f2cc6'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), '_etag');

-- brief
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '37296bdb-ce60-4339-a2a0-11d827118ecc', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'ce22f2f5-d8d1-4c6e-8649-3b24f6c2c38a'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), '_etag');

-- bewijs
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'e492b920-baa7-4e41-ad69-2559d4967cd2', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '8ca36dd0-7da4-498b-b095-12ac50d13677'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), '_etag');

-- afbeelding
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'cd7fbfbe-60ce-41e5-9f7d-9faa4be0e6e2', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'e30d5680-cce3-4e8a-b895-4d358d354198'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), '_etag');

-- advies
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '196aeea8-edf0-482b-ba98-a303420a3e05', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'ecdb5ee6-846e-4afe-bb87-bee2a87109a9'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e'), '_etag');
