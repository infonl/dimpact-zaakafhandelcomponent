-- SQL script that creates the 'BPMN test zaaktype 3' zaaktype in the Open Zaak database.
-- This zaaktype is used for the sign document BPMN integration tests.

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
    '2025-01-01',
    NULL,
    false,
    'e2b2d4f9-3b02-4b3e-b3d5-d26b85a7f37c',
    'bpmn-test-zaaktype-3',
    'BPMN test zaaktype 3',
    'BPMN test zaaktype 3',
    'openbaar',
    'BPMN test zaaktype 3',
    'BPMN test zaaktype 3',
    '',
    'extern',
    'Indienen',
    'Schade en aansprakelijkheid',
    'Behandelen',
    'P30D',
    NULL,
    true,
    true,
    'P1M',
    '{}',
    false,
    '',
    '{}',
    '2025-01-01',
    '{}',
    'https://selectielijst.openzaak.nl/api/v1/procestypen/1e12ad30-b900-4e7f-b3b7-569673cee0b0',
    'BPMN test zaaktype 3',
    '',
    1,
    2020,
    '_etag',
    '002564440',
    '',
    '',
    '',
    '',
    '',
    ''
);


-- RESULTAATTYPES

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
    'c1d2e3f4-5678-9abc-def0-1234567890ab',
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
    (SELECT id FROM catalogi_zaaktype WHERE uuid = 'e2b2d4f9-3b02-4b3e-b3d5-d26b85a7f37c'),
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
  (SELECT COALESCE(MAX(id),0) FROM catalogi_statustype) + 1,
  'f4a5b6c7-d890-1e2f-3a45-6789012abcde',
  'Afgerond',
  '',
  12,
  false,
  '',
  '',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'e2b2d4f9-3b02-4b3e-b3d5-d26b85a7f37c'),
  '_etag',
  NULL,
  NULL,
  NULL
);


-- ROLTYPEN

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
  'd2e3f4a5-b678-9c0d-ef12-3456789012ab',
  'Melder',
  'initiator',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'e2b2d4f9-3b02-4b3e-b3d5-d26b85a7f37c'),
  '_etag',
  NULL,
  NULL
);

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
  'e3f4a5b6-c789-0d1e-f234-56789012abcd',
  'Behandelaar',
  'behandelaar',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'e2b2d4f9-3b02-4b3e-b3d5-d26b85a7f37c'),
  '_etag',
  NULL,
  NULL
);


-- ZAAKTYPEN INFORMATIEOBJECTTYPE

-- bijlage
INSERT INTO catalogi_zaaktypeinformatieobjecttype
(id, uuid, volgnummer, richting, informatieobjecttype_id, statustype_id, zaaktype_id, _etag)
VALUES
(
  (SELECT COALESCE(MAX(id),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1,
  'a5b6c7d8-e901-2f3a-b456-789012abcdef',
  (SELECT COALESCE(MAX(volgnummer),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1,
  'inkomend',
  (SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b1933137-94d6-49bc-9e12-afe712512276'),
  NULL,
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'e2b2d4f9-3b02-4b3e-b3d5-d26b85a7f37c'),
  '_etag'
);
