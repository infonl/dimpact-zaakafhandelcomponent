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
    '2023-10-01', -- datum_begin_geldigheid
    NULL, -- datum_einde_geldigheid
    false, -- concept
    'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425', -- uuid (derived from the URL)
    'indienen-aansprakelijkstelling-behandelen', -- identificatie
    'Indienen aansprakelijkstelling door derden behandelen', -- zaaktype_omschrijving
    'Indienen aansprakelijkstelling', -- zaaktype_omschrijving_generiek
    'openbaar', -- vertrouwelijkheidaanduiding
    'Indienen aansprakelijkstelling door derden behandelen', -- doel
    'Indienen aansprakelijkstelling door derden behandelen', -- aanleiding
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
    '2023-10-01', -- versiedatum
    '{}', -- producten_of_diensten (empty array)
    'https://selectielijst.openzaak.nl/api/v1/procestypen/1e12ad30-b900-4e7f-b3b7-569673cee0b0', -- selectielijst_procestype
    'Indienen aansprakelijkstelling door derden', -- referentieproces_naam
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


-- RESULTTYPES (Resultaat type)

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
  'd47f2f3b-b169-4dc3-a6d6-3fb615de42f3', -- UUID
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
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'),
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
  '00da48aa-9263-4053-bd88-4b9037c9d966', -- UUID
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
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'),
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
  'b0ed0590-a1fe-4448-9f9a-9e8e848be727', -- UUID
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
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  '101b5071-b48a-4f51-8f32-eeb32e28f179', -- UUID
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
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  '6ed4f93f-6a57-4e74-8ef3-14b2704e3d51', -- UUID
  'Afgerond', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  12, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  'b9cc579b-b53f-4346-a40f-0eea6b9480fc', -- UUID
  'Wacht op aanvullende informatie', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  11, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  'ceb69567-7c72-47ed-b5b9-8e8130a69910', -- UUID
  'Onderzoek afgerond', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  10, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  'd8850773-3c57-4382-af62-45421b2ab8aa', -- UUID
  'Heropend', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  9, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  'a2f5a7ef-e763-4156-9e0b-714c31fe2fe5', -- UUID
  'In behandeling', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  8, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  'a469ef16-f874-4ded-8792-eaffe6b7994b', -- UUID
  'Intake', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  7, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  'f74943b2-0941-495e-94c7-cdd112929506', -- UUID
  'Melder', -- Omschrijving
  'initiator', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  '3f32499e-97f8-4580-8b06-51faf3953206', -- UUID
  'Behandelaar', -- Omschrijving
  'behandelaar', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  '3bb6928b-76de-4716-ac5f-fa3d7d6eca36', -- UUID
  'Belanghebbende',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  'e49a634b-731c-4460-93f4-e919686811aa', -- uuid
  'Medeaanvrager',                     -- omschrijving
  'mede_initiator',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  'ca31355e-abbf-4675-8700-9d167b194db1', -- uuid
  'Contactpersoon',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  '4b473a85-5516-441f-8d7d-57512c6b6833', -- uuid
  'Gemachtigde',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  '74799b20-0350-457d-8773-a0f1ab16b299', -- uuid
  'Plaatsvervanger',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
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
  '966ddb36-6989-4635-8a37-d7af980a37a6', -- uuid
  'Bewindvoerder',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag',                          -- _etag (Placeholder)
  NULL,                             -- datum_begin_geldigheid
  NULL                              -- datum_einde_geldigheid
);


--  ZAAKTYPE 2 INFORMATION OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
-- the informatie objecttype `bijlage` is used in the flow of creating a zaak by ZAC from an incoming 'productaanvraag' notification


-- Factuur
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1 , '2023-11-22', NULL, false, 'eca3ae33-c9f1-4136-a48a-47dc3f4aaaf5', 'factuur', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');



-- Ontvangstbevestiging
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1 , '2023-11-22', NULL, false, 'bf9a7836-2e29-4db1-9abc-382f2d4a9e70', 'ontvangstbevestiging', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Brief
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'd01b6502-6c9b-48a0-a5f2-9825a2128952', 'brief', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Bewijs
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '8018c096-28c5-4175-b235-916b0318c6ef', 'bewijs', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Afbeelding
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '37beaaf9-9075-4cc8-b847-a06552324c92', 'afbeelding', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Advies
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '8a106522-c526-427d-83d0-05393e5cac9a', 'advies', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Aangeboden bescheiden
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '9ad666ea-8f17-44a4-aa2c-9e1deb1c9326', 'aangeboden bescheiden', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Rapport Intern
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '390fca6f-4f9a-41f9-998a-3e7e7fe43271', 'rapport Intern', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Rapport Extern
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, 'b741de57-6509-456e-94fb-6266c0079356', 'rapport Extern', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Opdracht
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '0a6d8317-593f-4a64-9c18-9f14277e644c', 'opdracht', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Offerte
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '91dc9aab-0393-4ead-bdf7-0d6ff75aa8a7', 'offerte', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- Besluit
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie)
VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2023-11-22', NULL, false, '7397af15-44d1-4b0d-b7ea-22b20912ed80', 'besluit', 'openbaar', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- ZAAKTYPEN INFORMATIEOBJECTTYPE
-- e-mail
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '96c34d09-475c-41f2-99f6-9ae8123d0815', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'efc332f2-be3b-4bad-9e3c-49a6219c92ad'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');


-- bijlage
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'a911bd37-c699-4f0c-8039-6428148fd1f2', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b1933137-94d6-49bc-9e12-afe712512276'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- factuur
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'cc40a1dc-f02c-4ffe-8e28-e46e8dbed816', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'eca3ae33-c9f1-4136-a48a-47dc3f4aaaf5'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- ontvangstbevestiging
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '226f2ee4-c188-44ce-833f-2ae6664803ed', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'bf9a7836-2e29-4db1-9abc-382f2d4a9e70'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- brief
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '90465ffb-5731-42cf-be64-2f3a37ea70bb', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'd01b6502-6c9b-48a0-a5f2-9825a2128952'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');


-- bewijs
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '6e8813db-af94-4224-ab3d-ee886fcda954', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '8018c096-28c5-4175-b235-916b0318c6ef'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- afbeelding
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '30175a01-ab65-4c27-a90f-07e1c57f8fab', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '37beaaf9-9075-4cc8-b847-a06552324c92'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- advies
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'b9753782-e5bb-40d1-95aa-9aca1ef25bc4', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '8a106522-c526-427d-83d0-05393e5cac9a'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- aangeboden bescheiden
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '704ae7ba-1b65-4eca-b4d1-0c8311871450', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '9ad666ea-8f17-44a4-aa2c-9e1deb1c9326'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- rapport Intern
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '8053c7d0-7489-4b3e-8125-5646e7d2e63c', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '390fca6f-4f9a-41f9-998a-3e7e7fe43271'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- rapport Extern
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '56bd118c-6eac-4dc5-a078-5615a700448f', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b741de57-6509-456e-94fb-6266c0079356'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- opdracht
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '830ee5b3-ca41-40bc-b478-f0010da7ba02', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '0a6d8317-593f-4a64-9c18-9f14277e644c'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- offerte
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '61f985fa-dcd4-4d6c-8da3-5498f41cb51d', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '91dc9aab-0393-4ead-bdf7-0d6ff75aa8a7'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- besluit
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, 'cd2592ba-7f07-4616-91f0-9c4109c7a82b', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend',
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '7397af15-44d1-4b0d-b7ea-22b20912ed80'),
NULL,
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');


INSERT INTO catalogi_besluittype
(id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, omschrijving_generiek, besluitcategorie, reactietermijn, publicatie_indicatie, publicatietekst, publicatietermijn, toelichting, catalogus_id, _etag)
VALUES
(
  1,
  '2023-10-01',
  NULL,
  false,
  '1a282535-09cc-480c-a5cf-cef0a76a1c5b',
  'Besluit aansprakelijkstelling',
  'besluit-aansprakelijkstelling',
  'besluit',
  'P2D', -- 2 days
  true,
  '00:00:00',
  'P1D', -- 1 day
  'Besluit aansprakelijkstelling',
  (SELECT id FROM catalogi_catalogus WHERE naam = 'zac'),
  '_etag'
);


INSERT INTO catalogi_besluittype
(id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, omschrijving_generiek, besluitcategorie, reactietermijn, publicatie_indicatie, publicatietekst, publicatietermijn, toelichting, catalogus_id, _etag)
VALUES
(
  2,
  '2023-10-01',
  NULL,
  false,
  '3951f642-3a40-445e-907d-e1ae1f90b156',
  'Besluit na heroverweging',
  'besluit-na-heroverweging',
  'besluit',
  'P2D', -- 2 days
  true,
  '00:00:00',
  'P1D', -- 1 day
  'Besluit na heroverweging',
  (SELECT id FROM catalogi_catalogus WHERE naam = 'zac'),
  '_etag'
);

INSERT INTO catalogi_besluittype_zaaktypen
(id, besluittype_id, zaaktype_id)
VALUES
(
  1,
  (SELECT id FROM catalogi_besluittype WHERE omschrijving = 'Besluit aansprakelijkstelling'),
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425')
);

INSERT INTO catalogi_besluittype_zaaktypen
(id, besluittype_id, zaaktype_id)
VALUES
(
  2,
  (SELECT id FROM catalogi_besluittype WHERE omschrijving = 'Besluit na heroverweging'),
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425')
);

INSERT INTO catalogi_besluittype_informatieobjecttypen
(id, besluittype_id, informatieobjecttype_id)
VALUES
(
  1,
  (SELECT id FROM catalogi_besluittype WHERE omschrijving = 'Besluit aansprakelijkstelling'),
  (SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '7397af15-44d1-4b0d-b7ea-22b20912ed80')
);

INSERT INTO catalogi_besluittype_informatieobjecttypen
(id, besluittype_id, informatieobjecttype_id)
VALUES
(
  2,
  (SELECT id FROM catalogi_besluittype WHERE omschrijving = 'Besluit na heroverweging'),
  (SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '7397af15-44d1-4b0d-b7ea-22b20912ed80')
);
