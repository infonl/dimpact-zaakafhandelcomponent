-- SQL script that creates the 'Melding evenement organiseren behandelen' zaaktype in the Open Zaak database

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
  '_etag',       -- _etag,
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_resultaattype) + 1,
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
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  'c626ada3-52af-4be2-bd24-734a20fc4f9c', -- uuid
  'Afgerond', -- statustype_omschrijving
  '',         -- statustype_omschrijving_generiek
  11,         -- statustypevolgnummer
  false,      -- informeren
  '',         -- statustekst
  '',         -- toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
        '22b82146-af11-42cf-96bd-bea47057abd4', -- UUID
        'Aanvullende informatie vereist', -- Statustype Omschrijving
        '', -- Statustype Omschrijving Generiek
        10, -- Statustypevolgnummer
        false, -- Informeren
        '', -- Statustekst
        '', -- Toelichting
        (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  '77174c85-0b40-436e-af3c-e596393962f4', -- uuid
  'Heropend',  -- statustype_omschrijving
  '',         -- statustype_omschrijving_generiek
  9,          -- statustypevolgnummer
  false,      -- informeren
  '',         -- statustekst
  '',         -- toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  '8eb24967-910c-4bf8-9f30-c44653b0c30c', -- uuid
  'In behandeling', -- statustype_omschrijving
  '',              -- statustype_omschrijving_generiek
  8,               -- statustypevolgnummer
  false,           -- informeren
  '',              -- statustekst
  '',              -- toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  '485677e6-c20c-4a85-af92-ded14bcac8dd', -- uuid
  'Intake',       -- statustype_omschrijving
  '',             -- statustype_omschrijving_generiek
  7,              -- statustypevolgnummer
  false,          -- informeren
  '',             -- statustekst
  '',             -- toelichting
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  'd6a6a357-84fc-4cba-b08b-865004f261d6', -- uuid
  'Melder',                      -- omschrijving
  'initiator',                      -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  'f8617909-c166-4f2c-86cc-c0fc44b46725', -- uuid
  'Behandelaar',                    -- omschrijving
  'behandelaar',                    -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  '4c4cd850-8332-4bb9-adc4-dd046f0614ad', -- uuid
  'Belanghebbende',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  'b14cf056-0480-4060-a376-1dd522a50431', -- uuid
  'Medeaanvrager',                     -- omschrijving
  'mede_initiator',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  '35b9428f-80c3-41cd-98f5-a27b706bcf18', -- uuid
  'Contactpersoon',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  'c5d13ecf-c703-4f12-91b9-eb7f15b90329', -- uuid
  'Gemachtigde',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  '5e507487-d81f-4dc1-95c7-b6efd1b61741', -- uuid
  'Plaatsvervanger',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
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
  '24da3f16-2554-4a51-847d-46aacc876648', -- uuid
  'Bewindvoerder',                     -- omschrijving
  'belanghebbende',                 -- omschrijving_generiek
  (SELECT id FROM catalogi_zaaktype WHERE uuid = '448356ff-dcfb-4504-9501-7fe929077c4f'), -- Zaaktype ID
  '_etag',                          -- _etag (Placeholder)
  NULL,                             -- datum_begin_geldigheid
  NULL                              -- datum_einde_geldigheid
);

-- INFORMATIE OBJECT TYPES
-- ZAC required the informatie objecttype `e-mail` to be present (note the case sensitivity). Also see the 'ConfiguratieService.java' class in the ZAC code base.
-- the informatie objecttype `bijlage` is used in the flow of creating a zaak by ZAC from an incoming 'productaanvraag' notification
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie) VALUES (1, '2021-10-04', NULL, false, 'efc332f2-be3b-4bad-9e3c-49a6219c92ad', 'e-mail', 'zaakvertrouwelijk', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');
INSERT INTO catalogi_informatieobjecttype (id, datum_begin_geldigheid, datum_einde_geldigheid, concept, uuid, omschrijving, vertrouwelijkheidaanduiding, catalogus_id, _etag, omschrijving_generiek_definitie, omschrijving_generiek_herkomst, omschrijving_generiek_hierarchie, omschrijving_generiek_informatieobjecttype, omschrijving_generiek_opmerking, trefwoord, informatieobjectcategorie) VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_informatieobjecttype) + 1, '2021-10-04', NULL, false, 'b1933137-94d6-49bc-9e12-afe712512276', 'bijlage', 'zaakvertrouwelijk', 1, '_etag', '', '', '', '', '', '{}', 'onbekend');

-- ZAAK INFORMATIE OBJECT TYPES
INSERT INTO catalogi_zaaktypeinformatieobjecttype (id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag) VALUES (1, '405da8a9-7296-439c-a2eb-a470b84f17ee', 1, 'inkomend', 1, NULL, 1, '_etag');
INSERT INTO catalogi_zaaktypeinformatieobjecttype (id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag) VALUES ((SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1, '809b5454-45f6-4368-b876-a61775c7e6a7', 2, 'inkomend', 2, NULL, 1, '_etag');
