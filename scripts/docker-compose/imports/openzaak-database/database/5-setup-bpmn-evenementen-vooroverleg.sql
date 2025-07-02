-- SQL script that creates the 'Indienen aansprakelijkstelling door derden behandelen' zaaktype in the Open Zaak database

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
    '2025-07-01', -- datum_begin_geldigheid
    NULL, -- datum_einde_geldigheid
    false, -- concept
    '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a', -- uuid (derived from the URL)
    'bpmn-evenementen-vooroverleg', -- identificatie
    'BPMN Evenementen Vooroverleg', -- zaaktype_omschrijving
    'BPMN Evenementen Vooroverleg', -- zaaktype_omschrijving_generiek
    'openbaar', -- vertrouwelijkheidaanduiding
    'BPMN Evenementen Vooroverleg', -- doel
    'BPMN Evenementen Vooroverleg', -- aanleiding
    '', -- toelichting
    'extern', -- indicatie_intern_of_extern
    'Indienen', -- handeling_initiator
    'Evenementen Vooroverleg', -- onderwerp
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
    '2025-07-01', -- versiedatum
    '{}', -- producten_of_diensten (empty array)
    'https://selectielijst.openzaak.nl/api/v1/procestypen/1e12ad30-b900-4e7f-b3b7-569673cee0b0', -- selectielijst_procestype
    'BPMN Evenementen Vooroverleg', -- referentieproces_naam
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
  '63426748-cf56-4b55-8cbb-16f7537a0c73', -- UUID
  'Geweigerd', -- Omschrijving
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/1f750958-431c-4916-bc01-af5d3a753b41', -- Resultaattypeomschrijving
  'Geweigerd', -- Omschrijving Generiek
  'https://selectielijst.openzaak.nl/api/v1/resultaten/2431e1ee-cacd-44af-bd12-ea7280a8928b', -- Selectielijstklasse
  'vernietigen', -- Archiefnominatie
  'P1Y', -- Archiefactietermijn
  'afgehandeld', -- Brondatum Archiefprocedure Afleidingswijze
  '', -- Brondatum Archiefprocedure Datumkenmerk
  false, -- Brondatum Archiefprocedure Einddatum Bekend
  '', -- Brondatum Archiefprocedure Objecttype
  '', -- Brondatum Archiefprocedure Registratie
  NULL, -- Brondatum Archiefprocedure Procestermijn
  '', -- Toelichting
  -- Assuming zaaktype_id needs to be retrieved from the URL, adjust as needed
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'),
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
    '8a865de4-5927-4609-80f1-dda59fc36ecf',
    'Verleend',
    'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/45a3fcff-4898-4d63-8dcc-cb3fdc506b08',
    'Verleend',
    'https://selectielijst.openzaak.nl/api/v1/resultaten/f49b00e6-0612-4480-aaf5-8390be88d9f8',
    'vernietigen',
    'P1Y',
    'afgehandeld',
    '',
    false,
    '',
    '',
    NULL,
    '',
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_resultaattype) + 1, -- Adjust ID as needed
  '0f0f76b6-42a0-4f1b-b5f8-f89cfbc10532', -- UUID
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
  '',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'),
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
  '0778a4f2-8676-480e-be56-29863a5eda7a', -- UUID
  'Toegekend', -- Omschrijving
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/fb65d251-1518-4185-865f-b8bdcfad07b1', -- Resultaattypeomschrijving
  'Toegekend', -- Omschrijving Generiek
  'https://selectielijst.openzaak.nl/api/v1/resultaten/2431e1ee-cacd-44af-bd12-ea7280a8928b', -- Selectielijstklasse
  'vernietigen', -- Archiefnominatie
  'P1Y', -- Archiefactietermijn
  'afgehandeld', -- Brondatum Archiefprocedure Afleidingswijze
  '', -- Brondatum Archiefprocedure Datumkenmerk
  false, -- Brondatum Archiefprocedure Einddatum Bekend
  '', -- Brondatum Archiefprocedure Objecttype
  '', -- Brondatum Archiefprocedure Registratie
  NULL, -- Brondatum Archiefprocedure Procestermijn
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
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
  '2d279ed8-031e-40aa-b421-f96e7049efbd', -- UUID
  'Buiten behandeling', -- Omschrijving
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/ce8cf476-0b59-496f-8eee-957a7c6e2506', -- Resultaattypeomschrijving
  'Afgebroken', -- Omschrijving Generiek
  'https://selectielijst.openzaak.nl/api/v1/resultaten/949daaef-57e6-43aa-a1f2-4aeea5d4f7fc', -- Selectielijstklasse
  'vernietigen', -- Archiefnominatie
  'P1Y', -- Archiefactietermijn
  'afgehandeld', -- Brondatum Archiefprocedure Afleidingswijze
  '', -- Brondatum Archiefprocedure Datumkenmerk
  false, -- Brondatum Archiefprocedure Einddatum Bekend
  '', -- Brondatum Archiefprocedure Objecttype
  '', -- Brondatum Archiefprocedure Registratie
  NULL, -- Brondatum Archiefprocedure Procestermijn
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
  '_etag',
  NULL,
  '',
  NULL,
  NULL,
  NULL
);


-- STATUSTYPES
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
  '775a2303-1c20-4642-93e7-d6c469e2e92c', -- UUID
  'In behandeling', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  2, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
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
        '7236e8cd-2a41-478c-b925-be94edaefbc1', -- UUID
        'Aanvulling vereist', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        3, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
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
        '8675fd2d-7a40-4f87-9532-afbbf70dc341', -- UUID
        'Advies gevraagd', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        4, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
        '_etag', -- Placeholder
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  'cd3df4c2-36bd-45c0-a104-2ec292755a93', -- UUID
  'Melder', -- Omschrijving
  'initiator', -- Omschrijving Generiek
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '8f5a8ba6-bbf4-488b-ac84-cf1f05c224b5', -- UUID
  'Behandelaar', -- Omschrijving
  'behandelaar', -- Omschrijving Generiek
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '17d6b84c-603c-455a-822c-b9cf04e98c39', -- UUID
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  'dcf02e22-a0a7-44e4-a5da-972fa30e7cf2', -- uuid
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '9928ae6e-6fd8-45a8-9e95-85b2f65b416a', -- uuid
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '59ac30ab-75d1-4169-8d0e-68349927f2e7', -- uuid
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  'bb6f6d9d-d5c8-49d2-9715-bbf12f7c5e9d', -- uuid
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_roltype) + 1,
  '058329b7-b228-41c2-9718-d8cad8eaa54e', -- uuid
  'Bewindvoerder',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), -- Zaaktype ID
  '_etag',                          -- _etag (Placeholder)
  NULL,                             -- datum_begin_geldigheid
  NULL                              -- datum_einde_geldigheid
);


--  ZAAKTYPE 2 INFORMATION OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
-- the informatie objecttype `bijlage` is used in the flow of creating a zaak by ZAC from an incoming 'productaanvraag' notification

-- Factuur
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1 , '2023-11-22', NULL, false, '113dad01-35e6-4716-8011-3be5e331d5c7', 'factuur', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Ontvangstbevestiging
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1 , '2023-11-22', NULL, false, '09a6a36f-11bd-44be-9414-c70f48b78e03', 'ontvangstbevestiging', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Brief
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'd80f1835-792c-4428-bb7d-32a0595ef521', 'brief', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Bewijs
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'ff6747bd-7a06-4f13-9b68-0678020345c9', 'bewijs', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Afbeelding
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'aa1a5a75-e492-415c-9b8d-a1a3cbfb5903', 'afbeelding', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Advies
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '3fd707db-ae38-4889-bf7b-bb31032ae6ff', 'advies', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');


-- ZAAKTYPEN INFORMATIEOBJECTTYPE
-- e-mail
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '5d2768e4-5fa7-433b-a6e9-4074e4067c76', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'efc332f2-be3b-4bad-9e3c-49a6219c92ad'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');

-- bijlage
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '7961ee49-789f-45b6-855a-df732bab140b', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b1933137-94d6-49bc-9e12-afe712512276'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');

-- factuur
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'ef511583-f3ba-4bff-bf6b-0da07abac52a', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '113dad01-35e6-4716-8011-3be5e331d5c7'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');

-- ontvangstbevestiging
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'a5b4fe0f-f9e8-49db-8c14-64b33bc85f47', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '09a6a36f-11bd-44be-9414-c70f48b78e03'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');

-- brief
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '3206b2be-557e-47db-ac08-77ed96d5db1f', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'd80f1835-792c-4428-bb7d-32a0595ef521'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');

-- bewijs
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '43e07f37-11fa-41cc-882b-328506a374f3', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'ff6747bd-7a06-4f13-9b68-0678020345c9'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');

-- afbeelding
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '70012645-a564-46b9-b44c-f9fa2cdbe320', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'aa1a5a75-e492-415c-9b8d-a1a3cbfb5903'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');

-- advies
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '5347b526-f4e2-4e92-ae29-e3fed92e0647', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '3fd707db-ae38-4889-bf7b-bb31032ae6ff'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'), '_etag');
