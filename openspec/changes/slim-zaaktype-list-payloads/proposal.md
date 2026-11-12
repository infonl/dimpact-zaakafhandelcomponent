## Why

Three REST endpoints that return a list of zaaktypes send far more data per zaaktype than the page that reads the
list uses. `GET /rest/zaken/zaaktypes-for-creation` sends the full zaaktype with its complete zaaktype
configuration, including the zaakbeeindig parameters, mailtemplate koppelingen, zaakafzenders and the plan item
settings, for every zaaktype that the user may start. The Zaak aanmaken form only needs the name and the UUID to
fill its dropdown. The two admin lists, Inrichtingscheck and Zaakafhandelparameters, also send fields that their
tables do not show. With many zaaktypes this makes the responses large and the pages slow (PZ-13414, epic
PZ-12602 Schaalbaarheid & Performance).

## What Changes

- **BREAKING (internal API)** `GET /rest/zaaktypen?startbaar=true` replaces `GET /rest/zaken/zaaktypes-for-creation`,
  and returns, per zaaktype, only its UUID and its omschrijving. The list keeps the same zaaktypes, in the same
  order. Without `startbaar`, `GET /rest/zaaktypen` lists every zaaktype of the default catalogus as the same item.
- New endpoint `GET /rest/zaaktypen/{zaaktypeUuid}` returns the settings that the Zaak
  aanmaken form applies after the user chose a zaaktype: the vertrouwelijkheidaanduiding, the default groep, the
  default behandelaar, and the KVK and BRP koppelingen for the initiator. It answers only for a zaaktype that the
  list offers to the user.
- The Zaak aanmaken form, also when it is opened from the Zaak koppelen sidebar, fetches these settings after the
  user chose a zaaktype, and applies them as it does now. It sends only the UUID of the zaaktype when it creates
  the zaak.
- **BREAKING (internal API)** `GET /rest/health-check/zaaktypes` returns, per zaaktype, only the UUID,
  omschrijving, doel and begin geldigheid of the zaaktype, together with all inrichtingscheck results.
- **BREAKING (internal API)** `GET /rest/zaakafhandelparameters` returns a list item per zaaktype with only the
  fields that the Zaakafhandelparameters table shows and filters on. `GET /rest/zaakafhandelparameters/{uuid}`
  and `PUT /rest/zaakafhandelparameters/{uuid}` keep the full `RestZaaktypeConfiguration`.
- Zaak aanmaken, gekoppelde zaak aanmaken, Inrichtingscheck and Zaakafhandelparameters work as before for the
  user.
- Only this repository calls these endpoints, so no external client breaks.

## Capabilities

### New Capabilities
- `zaak-creation-zaaktype-choice`: the list of zaaktypes offered for zaak creation, and the settings of the chosen
  zaaktype that the Zaak aanmaken form reads after the choice.

### Modified Capabilities
- `zaaktype-inrichtingscheck`: the list of inrichtingschecks carries only the zaaktype fields that the page shows.
- `zaaktype-configuration`: the list of zaaktype configurations carries only the fields that the
  Zaakafhandelparameters page shows.

## Impact

- Backend: a new `ZaaktypeRestService` for `/rest/zaaktypen`, which takes over `listZaaktypesForZaakCreation` from
  `ZaakRestService` and adds the read endpoint, `HealthCheckRestService`,
  `ZaaktypeConfigurationRestService.listZaaktypeConfigurations`, new REST models and their converters.
  `RestZaaktype`, `RestZaaktypeConverter`, `RestZaaktypeOverzicht` and `RestZaaktypeConfiguration` stay, because the
  zaak detail, the koppelen zaaktype list and the configuration edit page still use them.
- Generated OpenAPI spec and the generated frontend types.
- Frontend: `ZakenService`, `ZaakCreateComponent`, `zaak-create-koppeling.ts`, `ZaakLinkComponent`,
  `InrichtingscheckComponent`, `ParametersComponent` and their specs.
- Integration tests in `ZaakRestServiceTest`, `HealthCheckRestServiceTest` and `ZaaktypeConfigurationRestServiceTest`.
- The test-data scripts in `scripts/test-data/` do not call these list endpoints and do not change.
