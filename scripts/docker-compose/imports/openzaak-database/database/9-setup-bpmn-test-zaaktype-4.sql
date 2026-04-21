-- SQL script that creates the 'BPMN test zaaktype 4' zaaktype in the Open Zaak database.
-- This zaaktype is used for the suspend/resume BPMN integration tests.

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
    'f5a7b8c9-d0e1-2345-f012-345678901bcd',
    'bpmn-test-zaaktype-4',
    'BPMN test zaaktype 4',
    'BPMN test zaaktype 4',
    'openbaar',
    'BPMN test zaaktype 4',
    'BPMN test zaaktype 4',
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
    'BPMN test zaaktype 4',
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
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
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
    (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f5a7b8c9-d0e1-2345-f012-345678901bcd'),
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
  'b2c3d4e5-f6a7-8901-bcde-f01234567891',
  'Afgerond',
  '',
  12,
  false,
  '',
  '',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f5a7b8c9-d0e1-2345-f012-345678901bcd'),
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
  'c3d4e5f6-a7b8-9012-cdef-012345678901',
  'Melder',
  'initiator',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f5a7b8c9-d0e1-2345-f012-345678901bcd'),
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
  'd4e5f6a7-b8c9-0123-def0-123456789012',
  'Behandelaar',
  'behandelaar',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f5a7b8c9-d0e1-2345-f012-345678901bcd'),
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
  'e5f6a7b8-c9d0-1234-ef01-234567890123',
  (SELECT COALESCE(MAX(volgnummer),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1,
  'inkomend',
  (SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b1933137-94d6-49bc-9e12-afe712512276'),
  NULL,
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f5a7b8c9-d0e1-2345-f012-345678901bcd'),
  '_etag'
);
