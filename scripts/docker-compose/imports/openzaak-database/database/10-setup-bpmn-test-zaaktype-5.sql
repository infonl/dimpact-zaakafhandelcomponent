-- SQL script that creates the 'BPMN test zaaktype 5' zaaktype in the Open Zaak database.
-- This zaaktype is used for the automatische ontvangstbevestiging BPMN integration tests.

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
    'f8e7d6c5-b4a3-9281-7605-e4f3d2c1b0a9',
    'bpmn-test-zaaktype-5',
    'BPMN test zaaktype 5',
    'BPMN test zaaktype 5',
    'openbaar',
    'BPMN test zaaktype 5',
    'BPMN test zaaktype 5',
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
    'BPMN test zaaktype 5',
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
    'a9b8c7d6-e5f4-3210-9876-543210fedcba',
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
    (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f8e7d6c5-b4a3-9281-7605-e4f3d2c1b0a9'),
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
  'b0c1d2e3-f4a5-6789-b0c1-d2e3f4a56789',
  'Afgerond',
  '',
  12,
  false,
  '',
  '',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f8e7d6c5-b4a3-9281-7605-e4f3d2c1b0a9'),
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
  'f1e2d3c4-b5a6-7890-f1e2-d3c4b5a67890',
  'Melder',
  'initiator',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f8e7d6c5-b4a3-9281-7605-e4f3d2c1b0a9'),
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
  'a2b3c4d5-e6f7-8901-a2b3-c4d5e6f78901',
  'Behandelaar',
  'behandelaar',
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f8e7d6c5-b4a3-9281-7605-e4f3d2c1b0a9'),
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
  'b4c5d6e7-f8a9-0123-b4c5-d6e7f8a90123',
  (SELECT COALESCE(MAX(volgnummer),0) FROM catalogi_zaaktypeinformatieobjecttype) + 1,
  'inkomend',
  (SELECT id FROM catalogi_informatieobjecttype WHERE uuid = 'b1933137-94d6-49bc-9e12-afe712512276'),
  NULL,
  (SELECT id FROM catalogi_zaaktype WHERE uuid = 'f8e7d6c5-b4a3-9281-7605-e4f3d2c1b0a9'),
  '_etag'
);
