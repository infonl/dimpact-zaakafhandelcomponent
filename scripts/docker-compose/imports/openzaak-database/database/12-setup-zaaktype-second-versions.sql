-- SQL script that adds a second version of 'Test zaaktype 1' and 'BPMN test zaaktype 1' to the Open Zaak database,
-- with their own resultaattypen. A second version is valid from 2099 only, so it does not replace the first version
-- for creating zaken; the integration tests publish it to ZAC with a zaaktype notification.

-- Second version of 'Test zaaktype 1'
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
        '2099-01-01', -- datum_begin_geldigheid
        NULL,         -- datum_einde_geldigheid
        false,         -- concept
        'ffa8a2a5-44ab-4644-ae6e-1d3c6d122e5a', -- uuid
        'zaaktype-test-1', -- identificatie
        'Test zaaktype 1', -- zaaktype_omschrijving
        'Generieke omschrijving van test zaaktype 1', -- zaaktype_omschrijving_generiek
        'openbaar',   -- vertrouwelijkheidaanduiding
        'Test zaaktype 1', -- doel
        'Test zaaktype 1', -- aanleiding
        '',           -- toelichting
        'extern',     -- indicatie_intern_of_extern
        'Melden',     -- handeling_initiator
        'Openbare orde & veiligheid', -- onderwerp
        'Behandelen', -- handeling_behandelaar
        'P1Y',        -- doorlooptijd_behandeling
        NULL,         -- servicenorm_behandeling
        false,        -- opschorting_en_aanhouding_mogelijk
        false,        -- verlenging_mogelijk
        NULL,         -- verlengingstermijn
        '{}',         -- trefwoorden
        false,        -- publicatie_indicatie
        '',           -- publicatietekst
        '{}',         -- verantwoordingsrelatie
        '2099-01-01', -- versiedatum
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
        '9fddf33e-0996-46ef-ad8c-1afa0df08637',
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
        (SELECT id FROM catalogi_zaaktype WHERE uuid = 'ffa8a2a5-44ab-4644-ae6e-1d3c6d122e5a'), -- Zaaktype ID
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
        '19d9ad5d-d341-4179-a0e3-52b3d53e8965',
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
        (SELECT id FROM catalogi_zaaktype WHERE uuid = 'ffa8a2a5-44ab-4644-ae6e-1d3c6d122e5a'), -- Zaaktype ID
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
        '4132de6c-ed50-496e-a22e-536d9c03feba',
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
        (SELECT id FROM catalogi_zaaktype WHERE uuid = 'ffa8a2a5-44ab-4644-ae6e-1d3c6d122e5a'), -- Zaaktype ID
        '_etag',
        NULL,
        '',
        NULL,
        NULL,
        NULL
    );

-- Second version of 'BPMN test zaaktype 1'
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
    '2099-01-01', -- datum_begin_geldigheid
    NULL, -- datum_einde_geldigheid
    false, -- concept
    'dd8744be-0f6e-4f0b-b017-f5fbf02be0dd', -- uuid (derived from the URL)
    'bpmn-test-zaaktype-1', -- identificatie
    'BPMN test zaaktype 1', -- zaaktype_omschrijving
    'Generieke omschrijving van BPMN test zaaktype 1', -- zaaktype_omschrijving_generiek
    'openbaar', -- vertrouwelijkheidaanduiding
    'BPMN test zaaktype 1', -- doel
    'BPMN test zaaktype 1', -- aanleiding
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
    '2099-01-01', -- versiedatum
    '{}', -- producten_of_diensten (empty array)
    'https://selectielijst.openzaak.nl/api/v1/procestypen/1e12ad30-b900-4e7f-b3b7-569673cee0b0', -- selectielijst_procestype
    'BPMN test zaaktype 1', -- referentieproces_naam
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
    '2b92903a-2cb9-4028-b0f8-f034b3ee9bed', -- UUID
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
    (SELECT id FROM catalogi_zaaktype WHERE uuid = 'dd8744be-0f6e-4f0b-b017-f5fbf02be0dd'),
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
    '7749ad06-f1b3-41f5-abe9-789960af0fbc',
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
    (SELECT id FROM catalogi_zaaktype WHERE uuid = 'dd8744be-0f6e-4f0b-b017-f5fbf02be0dd'), -- Zaaktype ID
    '_etag',
    NULL,
    '',
    NULL,
    NULL,
    NULL
);
