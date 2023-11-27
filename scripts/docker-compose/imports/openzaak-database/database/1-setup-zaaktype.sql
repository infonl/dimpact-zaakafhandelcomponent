CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO catalogi_catalogus (_admin_name, uuid, domein, rsin, contactpersoon_beheer_naam, contactpersoon_beheer_telefoonnummer, contactpersoon_beheer_emailadres, _etag) VALUES
    ('zac', '8225508a-6840-413e-acc9-6422af120db1', 'ALG', '002564440', 'ZAC Test Catalogus', '06-12345678', 'noreply@example.com', '_etag');

INSERT INTO authorizations_applicatie (uuid, client_ids, label, heeft_alle_autorisaties) VALUES (uuid_generate_v4(), '{zac_client}', 'ZAC', true);
-- Open Notificaties is not used yet in our Docker Compose set-up
-- INSERT INTO authorizations_applicatie (uuid, client_ids, label, heeft_alle_autorisaties) VALUES (uuid_generate_v4(), '{opennotificaties}', 'Open notificaties', true);
-- Open Formulieren is not used yet in our Docker Compose set-up
-- INSERT INTO authorizations_applicatie (uuid, client_ids, label, heeft_alle_autorisaties) VALUES (uuid_generate_v4(), '{openformulieren}', 'Open Formulieren', true);

INSERT INTO vng_api_common_jwtsecret (identifier, secret) VALUES ('zac_client', 'openzaakZaakafhandelcomponentClientSecret');
-- even-though we do not use Open Notificaties, we do need to set up a JWT secret for it, as this is required by Open Zaak
INSERT INTO vng_api_common_jwtsecret (identifier, secret) VALUES ('opennotificaties', 'openNotificatiesApiSecretKey');
INSERT INTO vng_api_common_jwtsecret (identifier, secret) VALUES ('openformulieren', 'openFormulierenApiSecretKey');

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
  _etag
)
VALUES
(
    1,
  '2023-09-21', -- datum_begin_geldigheid
  NULL,         -- datum_einde_geldigheid
  false,         -- concept
  '448356ff-dcfb-4504-9501-7fe929077c4f', -- uuid
  'melding-evenement-organiseren-behandelen', -- identificatie
  'Melding evenement organiseren behandelen', -- zaaktype_omschrijving
  'Melding evenement organiseren', -- zaaktype_omschrijving_generiek
  'openbaar',   -- vertrouwelijkheidaanduiding
  'Melding evenement organiseren behandelen', -- doel
  'Melding evenement organiseren behandelen', -- aanleiding
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
  'https://selectielijst.openzaak.nl/api/v1/procestypen/7ff2b005-4d84-47fe-983a-732bfa958ff5', -- selectielijst_procestype
  'melding klein evenement', -- referentieproces_naam
  '',           -- referentieproces_link
  1,           -- catalogus_id
  2020,           -- selectielijst_procestype_jaar
  '_etag'       -- _etag
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
  _etag
)
VALUES
(
  4,
  '2b774ae4-68b0-462c-b6a0-e48b861ee148',
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
  1,
  '_etag'
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
  _etag
)
VALUES
(
  5,
  'dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6',
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
  1,
  '_etag'
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
  _etag
)
VALUES
(
  6,
  '060b1651-4795-4982-bf66-584391bf0421',
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
  1,
  '_etag'
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
  _etag
)
VALUES
(
  1, -- id (Placeholder; adjust as needed)
  'c626ada3-52af-4be2-bd24-734a20fc4f9c', -- uuid
  'Afgerond', -- statustype_omschrijving
  '',         -- statustype_omschrijving_generiek
  10,         -- statustypevolgnummer
  false,      -- informeren
  '',         -- statustekst
  '',         -- toelichting
  1,          -- zaaktype_id (Placeholder; adjust as needed)
  '_etag'     -- _etag (Placeholder)
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
  _etag
)
VALUES
(
  2, -- id (Placeholder; adjust as needed)
  '77174c85-0b40-436e-af3c-e596393962f4', -- uuid
  'Heropend',  -- statustype_omschrijving
  '',         -- statustype_omschrijving_generiek
  9,          -- statustypevolgnummer
  false,      -- informeren
  '',         -- statustekst
  '',         -- toelichting
  1,          -- zaaktype_id (Placeholder; adjust as needed)
  '_etag'     -- _etag (Placeholder)
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
  _etag
)
VALUES
(
  3, -- id (Placeholder; adjust as needed)
  '8eb24967-910c-4bf8-9f30-c44653b0c30c', -- uuid
  'In behandeling', -- statustype_omschrijving
  '',              -- statustype_omschrijving_generiek
  8,               -- statustypevolgnummer
  false,           -- informeren
  '',              -- statustekst
  '',              -- toelichting
  1,               -- zaaktype_id (Placeholder; adjust as needed)
  '_etag'          -- _etag (Placeholder)
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
  _etag
)
VALUES
(
  4, -- id (Placeholder; adjust as needed)
  '485677e6-c20c-4a85-af92-ded14bcac8dd', -- uuid
  'Intake',       -- statustype_omschrijving
  '',             -- statustype_omschrijving_generiek
  7,              -- statustypevolgnummer
  false,          -- informeren
  '',             -- statustekst
  '',             -- toelichting
  1,              -- zaaktype_id (Placeholder; adjust as needed)
  '_etag'         -- _etag (Placeholder)
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
  _etag
)
VALUES
(
  1,                               -- id (Placeholder; adjust as needed)
  'd6a6a357-84fc-4cba-b08b-865004f261d6', -- uuid
  'Initiator',                      -- omschrijving
  'initiator',                      -- omschrijving_generiek
  1,                               -- zaaktype_id (Placeholder; adjust as needed)
  '_etag'                          -- _etag (Placeholder)
);

-- For the second JSON object
INSERT INTO catalogi_roltype
(
  id,
  uuid,
  omschrijving,
  omschrijving_generiek,
  zaaktype_id,
  _etag
)
VALUES
(
  2,                               -- id (Placeholder; adjust as needed)
  'f8617909-c166-4f2c-86cc-c0fc44b46725', -- uuid
  'Behandelaar',                    -- omschrijving
  'behandelaar',                    -- omschrijving_generiek
  1,                               -- zaaktype_id (Placeholder; adjust as needed)
  '_etag'                          -- _etag (Placeholder)
);

-- For the third JSON object
INSERT INTO catalogi_roltype
(
  id,
  uuid,
  omschrijving,
  omschrijving_generiek,
  zaaktype_id,
  _etag
)
VALUES
(
  3,                               -- id (Placeholder; adjust as needed)
  '4c4cd850-8332-4bb9-adc4-dd046f0614ad', -- uuid
  'Betrokkene',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  1,                               -- zaaktype_id (Placeholder; adjust as needed)
  '_etag'                          -- _etag (Placeholder)
);

--  INFORMATION OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
-- the informatie objecttype `bijlage` is used in the flow of creating a zaak by ZAC from an incoming 'productaanvraag' notification
INSERT INTO catalogi_informatieobjecttype(id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag) VALUES (1, '2021-10-04', NULL, false, 'efc332f2-be3b-4bad-9e3c-49a6219c92ad', 'e-mail', 'zaakvertrouwelijk', 1, '_etag');
INSERT INTO catalogi_informatieobjecttype(id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag) VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2021-10-04', NULL, false, 'b1933137-94d6-49bc-9e12-afe712512276', 'bijlage', 'zaakvertrouwelijk', 1, '_etag');
INSERT INTO catalogi_zaaktypeinformatieobjecttype(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag) VALUES (1, '405da8a9-7296-439c-a2eb-a470b84f17ee', 1, 'inkomend', 1, NULL, 1, '_etag');
INSERT INTO catalogi_zaaktypeinformatieobjecttype(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag) VALUES ((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '809b5454-45f6-4368-b876-a61775c7e6a7', 2, 'inkomend', 2, NULL, 1, '_etag');


-- ZAAKTYPE 2

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
  _etag
  )
VALUES
  (
    (SELECT id FROM catalogi_zaaktype ORDER BY id DESC LIMIT 1) + 1, -- Assuming auto-increment is not set for id
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
    false, -- opschorting_en_aanhouding_mogelijk
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
    '_etag' -- _etag (Placeholder, assuming it needs to be generated or provided elsewhere)
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
  _etag
)
VALUES
(
  -- Adjust ID as needed
  (SELECT id FROM catalogi_resultaattype ORDER BY id DESC LIMIT 1) + 1,
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
  '_etag'
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_resultaattype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  '00da48aa-9263-4053-bd88-4b9037c9d966', -- UUID
  'Afgebroken', -- Omschrijving
  'https://selectielijst.openzaak.nl/api/v1/resultaattypeomschrijvingen/ce8cf476-0b59-496f-8eee-957a7c6e2506', -- Resultaattypeomschrijving
  'Afgebroken', -- Omschrijving Generiek
  'https://selectielijst.openzaak.nl/api/v1/resultaten/3db49761-7544-4836-a133-3c77db280e90', -- Selectielijstklasse
  'blijvend_bewaren', -- Archiefnominatie
  'P1Y', -- Archiefactietermijn
  'afgehandeld', -- Brondatum Archiefprocedure Afleidingswijze
  '', -- Brondatum Archiefprocedure Datumkenmerk
  false, -- Brondatum Archiefprocedure Einddatum Bekend
  '', -- Brondatum Archiefprocedure Objecttype
  '', -- Brondatum Archiefprocedure Registratie
  NULL, -- Brondatum Archiefprocedure Procestermijn
  'Het afhandelen van een geschil dat door een derde aanhangig wordt gemaakt omdat deze een (vermeend) nadeel heeft ondervonden door het (niet) handelen van de instelling', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_resultaattype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
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
  '_etag' -- Placeholder
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_resultaattype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
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
  '_etag' -- Placeholder
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_statustype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  '6ed4f93f-6a57-4e74-8ef3-14b2704e3d51', -- UUID
  'Afgerond', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  12, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_statustype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  'b9cc579b-b53f-4346-a40f-0eea6b9480fc', -- UUID
  'Aanvullende informatie vereist', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  11, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_statustype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  'ceb69567-7c72-47ed-b5b9-8e8130a69910', -- UUID
  'Onderzoek afgerond', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  10, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_statustype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  'd8850773-3c57-4382-af62-45421b2ab8aa', -- UUID
  'Heropend', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  9, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_statustype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  'a2f5a7ef-e763-4156-9e0b-714c31fe2fe5', -- UUID
  'In behandeling', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  8, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_statustype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  'a469ef16-f874-4ded-8792-eaffe6b7994b', -- UUID
  'Intake', -- Statustype Omschrijving
  '', -- Statustype Omschrijving Generiek
  7, -- Statustypevolgnummer
  false, -- Informeren
  '', -- Statustekst
  '', -- Toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
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
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_roltype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  'f74943b2-0941-495e-94c7-cdd112929506', -- UUID
  'Initiator', -- Omschrijving
  'initiator', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
);


-- For the second JSON object
INSERT INTO catalogi_roltype
(
  id,
  uuid,
  omschrijving,
  omschrijving_generiek,
  zaaktype_id,
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_roltype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  '3f32499e-97f8-4580-8b06-51faf3953206', -- UUID
  'Behandelaar', -- Omschrijving
  'behandelaar', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
);


-- For the third JSON object
INSERT INTO catalogi_roltype
(
  id,
  uuid,
  omschrijving,
  omschrijving_generiek,
  zaaktype_id,
  _etag
)
VALUES
(
  (SELECT id FROM catalogi_roltype ORDER BY id DESC LIMIT 1) + 1, -- Adjust ID as needed
  '3bb6928b-76de-4716-ac5f-fa3d7d6eca36', -- UUID
  'Informatiebeheerder', -- Omschrijving
  'zaakcoordinator', -- Omschrijving Generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), -- Zaaktype ID
  '_etag' -- Placeholder
);



--  ZAAKTYPE 2 INFORMATION OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
-- the informatie objecttype `bijlage` is used in the flow of creating a zaak by ZAC from an incoming 'productaanvraag' notification

-- Factuur
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1 , '2023-11-22', NULL, false, 'eca3ae33-c9f1-4136-a48a-47dc3f4aaaf5', 'factuur', 'openbaar', 1, '_etag');

-- Ontvangstbevestiging
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1 , '2023-11-22', NULL, false, 'bf9a7836-2e29-4db1-9abc-382f2d4a9e70', 'ontvangstbevestiging', 'openbaar', 1, '_etag');

-- Brief
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, 'd01b6502-6c9b-48a0-a5f2-9825a2128952', 'brief', 'openbaar', 1, '_etag');

-- Bewijs
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, '8018c096-28c5-4175-b235-916b0318c6ef', 'bewijs', 'openbaar', 1, '_etag');

-- Afbeelding
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, '37beaaf9-9075-4cc8-b847-a06552324c92', 'afbeelding', 'openbaar', 1, '_etag');

-- Advies
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, '8a106522-c526-427d-83d0-05393e5cac9a', 'advies', 'openbaar', 1, '_etag');

-- Aangeboden bescheiden
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, '9ad666ea-8f17-44a4-aa2c-9e1deb1c9326', 'aangeboden bescheiden', 'openbaar', 1, '_etag');

-- Rapport Intern
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, '390fca6f-4f9a-41f9-998a-3e7e7fe43271', 'rapport Intern', 'openbaar', 1, '_etag');

-- Rapport Extern
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, 'b741de57-6509-456e-94fb-6266c0079356', 'rapport Extern', 'openbaar', 1, '_etag');

-- Opdracht
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, '0a6d8317-593f-4a64-9c18-9f14277e644c', 'opdracht', 'openbaar', 1, '_etag');

-- Offerte
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, '91dc9aab-0393-4ead-bdf7-0d6ff75aa8a7', 'offerte', 'openbaar', 1, '_etag');

-- Besluit
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag)
VALUES ((SELECT id FROM catalogi_informatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '2023-11-22', NULL, false, '7397af15-44d1-4b0d-b7ea-22b20912ed80', 'besluit', 'openbaar', 1, '_etag');

-- ZAAKTYPEN INFORMATIEOBJECTTYPE
-- e-mail
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '96c34d09-475c-41f2-99f6-9ae8123d0815', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'efc332f2-be3b-4bad-9e3c-49a6219c92ad'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');


-- bijlage
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, 'a911bd37-c699-4f0c-8039-6428148fd1f2', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b1933137-94d6-49bc-9e12-afe712512276'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- factuur
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, 'cc40a1dc-f02c-4ffe-8e28-e46e8dbed816', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'eca3ae33-c9f1-4136-a48a-47dc3f4aaaf5'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- ontvangstbevestiging
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '226f2ee4-c188-44ce-833f-2ae6664803ed', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'bf9a7836-2e29-4db1-9abc-382f2d4a9e70'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- brief
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '90465ffb-5731-42cf-be64-2f3a37ea70bb', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'd01b6502-6c9b-48a0-a5f2-9825a2128952'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');


-- bewijs
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '6e8813db-af94-4224-ab3d-ee886fcda954', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '8018c096-28c5-4175-b235-916b0318c6ef'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- afbeelding
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '30175a01-ab65-4c27-a90f-07e1c57f8fab', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '37beaaf9-9075-4cc8-b847-a06552324c92'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- advies
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, 'b9753782-e5bb-40d1-95aa-9aca1ef25bc4', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '8a106522-c526-427d-83d0-05393e5cac9a'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- aangeboden bescheiden
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '704ae7ba-1b65-4eca-b4d1-0c8311871450', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '9ad666ea-8f17-44a4-aa2c-9e1deb1c9326'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- rapport Intern
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '8053c7d0-7489-4b3e-8125-5646e7d2e63c', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '390fca6f-4f9a-41f9-998a-3e7e7fe43271'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- rapport Extern
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '56bd118c-6eac-4dc5-a078-5615a700448f', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b741de57-6509-456e-94fb-6266c0079356'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- opdracht
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '830ee5b3-ca41-40bc-b478-f0010da7ba02', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '0a6d8317-593f-4a64-9c18-9f14277e644c'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- offerte
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, '61f985fa-dcd4-4d6c-8da3-5498f41cb51d', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
(SELECT id FROM catalogi_informatieobjecttype WHERE uuid = '91dc9aab-0393-4ead-bdf7-0d6ff75aa8a7'),
NULL, 
(SELECT id FROM catalogi_zaaktype WHERE uuid = 'fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'), '_etag');

-- besluit
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
((SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY id DESC LIMIT 1) + 1, 'cd2592ba-7f07-4616-91f0-9c4109c7a82b', (SELECT id FROM catalogi_zaaktypeinformatieobjecttype ORDER BY volgnummer DESC LIMIT 1) + 1, 'inkomend', 
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
  '00:00:00', 
  false, 
  '00:00:00', 
  NULL, 
  'Besluit aansprakelijkstelling', 
  (SELECT id FROM catalogi_catalogus WHERE _admin_name = 'zac'), 
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
  '00:00:00', 
  false, 
  '00:00:00', 
  NULL, 
  'Besluit na heroverweging', 
  (SELECT id FROM catalogi_catalogus WHERE _admin_name = 'zac'), 
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




-- Insert productaanvraag PDF document as enkelvoudig informatieobject.
-- This assumes that the PDF in question is available in the OpenZaak container in: '/app/private-media/uploads/2023/10/dummy-test-document.pdf'
-- Note that we use an id of '999' for both the canonical and the enkelvoudig informatieobject records to avoid duplicate key conflicts
-- on subsequent new documents created via the Open Zaak REST API.
-- It appears that Open Zaak uses its cache for this data and does not see this data inserted via the database at startup..
INSERT INTO documenten_enkelvoudiginformatieobjectcanonical (id, lock) VALUES (999, '');
INSERT INTO documenten_enkelvoudiginformatieobject (id, identificatie, bronorganisatie, creatiedatum, titel, vertrouwelijkheidaanduiding, auteur, status, beschrijving, ontvangstdatum, verzenddatum, indicatie_gebruiksrecht, ondertekening_soort, ondertekening_datum, uuid, formaat, taal, bestandsnaam, inhoud, link, integriteit_algoritme, integriteit_waarde, integriteit_datum, versie, begin_registratie, _informatieobjecttype_id, canonical_id, bestandsomvang, _informatieobjecttype_base_url_id, _informatieobjecttype_relative_url) VALUES (999, 'DOCUMENT-2023-0000000001', '002564440', '2023-10-30', 'Dummy test document', 'zaakvertrouwelijk', 'Aanvrager', 'definitief', 'Ingezonden formulier', null, null, false, '', null, '37e16ddc-7992-418c-b5a0-54cf6680329e', 'application/pdf', 'nld', 'dummy-test-document.pdf', 'uploads/2023/10/dummy-test-document.pdf', '', '', '', null, 1, '2023-10-30 11:54:01.849795 +00:00', 2, 999, 1234, null, null);

-- Open Notificaties is not used yet in our Docker Compose set-up
-- UPDATE notifications_notificationsconfig SET api_root = 'http://opennotificaties:8000/api/v1/';

-- Open Formulieren is not used yet in our Docker Compose set-up
-- INSERT INTO zgw_consumers_service(label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file) VALUES ('Open formulieren', 'nrc', 'http://host.docker.internal:8002/api/v1/', 'openzaak', 'openzaak', 'zgw', '', '', 'http://host.docker.internal:8002/api/v1/schema/openapi.yaml', '', '', '', '');
-- Set up the BAG service configuration. This requires that the corresponding variables have been passed on to this script.
INSERT INTO zgw_consumers_service(label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id) VALUES ('BAG', 'orc', :'BAG_API_CLIENT_MP_REST_URL', '', '', 'api_key', 'X-Api-Key', :'BAG_API_KEY', :'BAG_API_CLIENT_MP_REST_URL', '', '', '', '', null, null);
INSERT INTO zgw_consumers_service(label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id) VALUES ('Objects API', 'orc', 'http://objecten-api.local:8000/api/v2/', '', '', 'api_key', 'Authorization', 'Token cd63e158f3aca276ef284e3033d020a22899c728', 'http://objecten-api.local:8000/api/v2/schema/openapi.yaml', '', '', '', '', null, null);
