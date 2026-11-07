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
    '2025-01-01', -- datum_begin_geldigheid
    NULL, -- datum_einde_geldigheid
    false, -- concept
    '26076928-ce07-4d5d-8638-c2d276f6caca', -- uuid (derived from the URL)
    'bpmn-test-zaaktype', -- identificatie
    'BPMN test zaaktype', -- zaaktype_omschrijving
    'BPMN test zaaktype', -- zaaktype_omschrijving_generiek
    'openbaar', -- vertrouwelijkheidaanduiding
    'BPMN test zaaktype', -- doel
    'BPMN test zaaktype', -- aanleiding
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
    'BPMN test zaaktype', -- referentieproces_naam
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
  '940a8805-1117-45e8-838a-c3874b780996', -- UUID
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
  'Het afhandelen van een geschil dat door een derde aanhangig wordt gemaakt omdat deze een (vermeend) nadeel heeft ondervonden door het (niet) handelen van de instelling', -- Toelichting
  -- Assuming zaaktype_id needs to be retrieved from the URL, adjust as needed
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'),
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
  '82442c7f-05f2-4e9d-a0ae-c038344809af', -- UUID
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
  'Het afhandelen van een geschil dat door een derde aanhangig wordt gemaakt omdat deze een (vermeend) nadeel heeft ondervonden door het (niet) handelen van de instelling',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'),
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
  '9dbda017-0945-4fbb-8c53-ab336292ccb9', -- UUID
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
  'Het afhandelen van een geschil dat door een derde aanhangig wordt gemaakt omdat deze een (vermeend) nadeel heeft ondervonden door het (niet) handelen van de instelling', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  'a1f99809-74d0-4973-bd4a-356d8c8462ca', -- UUID
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
  'Het afhandelen van een geschil dat door een derde aanhangig wordt gemaakt omdat deze een (vermeend) nadeel heeft ondervonden door het (niet) handelen van de instelling', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  '4ec71651-2039-427c-9d22-b31615738de0', -- UUID
  'Afgerond', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  12, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  '35c18981-7104-4088-a0e5-be402d1b3c39', -- UUID
  'Wacht op aanvullende informatie', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  11, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  'cfbae35e-b106-4e29-9011-057dcc0fafc8', -- UUID
  'Onderzoek afgerond', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  10, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  '5d38db5b-588b-4f1d-8c8e-0978a2d26609', -- UUID
  'Heropend', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  9, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  '9ebd3a4d-0c05-46b4-82bf-caff6cf1119e', -- UUID
  'In behandeling', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  8, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  'a5574210-c112-4935-8b37-a9a52413fa92', -- UUID
  'Intake', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  7, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  'fcc9db56-6684-4696-9f46-c62224c0866a', -- UUID
  'Melder', -- Omschrijving
  'initiator', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  '351814c8-aff7-4151-9d20-376b3411acb2', -- UUID
  'Behandelaar', -- Omschrijving
  'behandelaar', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  '051d5db6-c725-4d22-a7fd-a75c5628dc54', -- UUID
  'Belanghebbende',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  'd1245029-1991-4ae5-b932-44ec257a35a8', -- uuid
  'Medeaanvrager',                     -- omschrijving
  'mede_initiator',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  'e69266e6-b8e6-4767-99de-e56aa598c24e', -- uuid
  'Contactpersoon',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  'b98fda66-b4b4-4801-a812-5452b74adfc4', -- uuid
  'Gemachtigde',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  '65eaa4a6-da78-41be-9af5-8be8549dffc4', -- uuid
  'Plaatsvervanger',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
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
  '6541c52a-7bda-4fca-af35-0ecdf3dcc27a', -- uuid
  'Bewindvoerder',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), -- Zaaktype ID
  '_etag',                          -- _etag (Placeholder)
  NULL,                             -- datum_begin_geldigheid
  NULL                              -- datum_einde_geldigheid
);


--  ZAAKTYPE 2 INFORMATION OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
-- the informatie objecttype `bijlage` is used in the flow of creating a zaak by ZAC from an incoming 'productaanvraag' notification

-- Factuur
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1 , '2023-11-22', NULL, false, 'a8dfd5b8-8657-48bf-b624-f962709f6e19', 'factuur', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Ontvangstbevestiging
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1 , '2023-11-22', NULL, false, '12dbb9de-6b5c-4649-b9f3-06e6190f2cc6', 'ontvangstbevestiging', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Brief
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'ce22f2f5-d8d1-4c6e-8649-3b24f6c2c38a', 'brief', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Bewijs
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '8ca36dd0-7da4-498b-b095-12ac50d13677', 'bewijs', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Afbeelding
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'e30d5680-cce3-4e8a-b895-4d358d354198', 'afbeelding', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Advies
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'ecdb5ee6-846e-4afe-bb87-bee2a87109a9', 'advies', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');


-- ZAAKTYPEN INFORMATIEOBJECTTYPE
-- e-mail
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '601340f6-c7d4-49bb-9c0f-a5a6d23e0d07', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'efc332f2-be3b-4bad-9e3c-49a6219c92ad'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), '_etag');

-- bijlage
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'c37522f0-a73a-4b14-89bc-6a8b37fb9200', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b1933137-94d6-49bc-9e12-afe712512276'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), '_etag');

-- factuur
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '5a6d0e86-25a8-4b2d-8ae3-5309a0e3f0a5', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'a8dfd5b8-8657-48bf-b624-f962709f6e19'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), '_etag');

-- ontvangstbevestiging
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '2ce1b340-7e92-42ad-a763-19c62c411577', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '12dbb9de-6b5c-4649-b9f3-06e6190f2cc6'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), '_etag');

-- brief
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'aad35ca2-9532-4d7c-bcac-0bae4be836ab', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'ce22f2f5-d8d1-4c6e-8649-3b24f6c2c38a'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), '_etag');

-- bewijs
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '44ef324d-7762-40ca-8c06-ea083dbe06fb', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '8ca36dd0-7da4-498b-b095-12ac50d13677'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), '_etag');

-- afbeelding
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '75d6a734-e56e-43b9-af15-e3f97a008815', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'e30d5680-cce3-4e8a-b895-4d358d354198'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), '_etag');

-- advies
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'cf1e8015-748b-4acb-aee7-c34afbf4f7ee', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'ecdb5ee6-846e-4afe-bb87-bee2a87109a9'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = '26076928-ce07-4d5d-8638-c2d276f6caca'), '_etag');


