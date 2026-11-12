## Context

See proposal.md for the motivation. The current state of the three endpoints:

- `GET /rest/zaken/zaaktypes-for-creation` (`ZaakRestService.listZaaktypesForZaakCreation`) maps each offered
  zaaktype with `RestZaaktypeConverter.convert`. That converter builds a full `RestZaaktype`, including a
  `RestZaaktypeConfiguration` with `inclusiefRelaties = true`: plan item settings, zaakbeeindig parameters,
  mailtemplate koppelingen, zaakafzenders and the niet-ontvankelijk resultaattype. The frontend reads only
  `uuid` and `omschrijving` from the list (`zaak-create.component.ts`, `zaak-create-koppeling.ts`,
  `zaak-link.component.ts`). After the choice it reads `vertrouwelijkheidaanduiding`,
  `zaakafhandelparameters.defaultGroepId`, `defaultBehandelaarId` and `betrokkeneKoppelingen.isKvkKoppelenEnabled`
  / `isBrpKoppelenEnabled`. The create request sends the whole `RestZaaktype` back, but `createZaak` reads only
  `restZaak.zaaktype.uuid`.
- `GET /rest/health-check/zaaktypes` returns `RestZaaktypeInrichtingscheck` with a `RestZaaktypeOverzicht` of ten
  fields. The page reads `omschrijving`, `doel` and `beginGeldigheid` of the zaaktype, and every check result
  (the expanded row lists a finding per failed check).
- `GET /rest/zaakafhandelparameters` (`ZaaktypeConfigurationRestService.listZaaktypeConfigurations`) returns a full
  `RestZaaktypeConfiguration` per zaaktype with `inclusiefRelaties = false`. Per item it reads the zaaktype again
  from the ZTC cache, and asks `ZaakspecifiekeAutorisatieService.isZaakspecifiekAutoriseerbaar`, which looks up an
  eigenschap and a roltype in the ZTC. The page reads `isValide`, `creatiedatum`, `processDefinition.key` / `naam`
  and the zaaktype fields `uuid`, `identificatie`, `omschrijving`, `doel`, `isNuGeldig`, `beginGeldigheid`,
  `eindeGeldigheid`.

`RestZaaktype`, `RestZaaktypeOverzicht` and `RestZaaktypeConfiguration` are also used by the zaak detail, by
`GET /rest/zaken/gekoppelde-zaken/zaaktypen`, and by the read and write of one configuration. Those stay as they
are.

## Goals / Non-Goals

**Goals:**
- Each of the three list endpoints sends one small, dedicated list item type.
- The Zaak aanmaken form reads the settings of one zaaktype after the user chose it.
- No visible change on the four screens.

**Non-Goals:**
- `GET /rest/zaken/gekoppelde-zaken/zaaktypen` also sends full `RestZaaktype` items. PZ-13414 does not name it.
  It is a candidate for a follow-up item with the same approach.
- Paging of these lists, or caching on the server.
- A repository-wide convention for list payloads. This change does not add one, so it adds no ADR.
- Changes to `RestZaaktype`, `RestZaaktypeConverter`, `RestZaaktypeOverzicht` or `RestZaaktypeConfiguration`.

## Decisions

### D1. A dedicated list item type per endpoint, not a shared summary

Each endpoint gets its own response type with exactly the fields its page reads:

| Endpoint | New type (suggested name) | Fields |
|---|---|---|
| `GET /zaaktypen?startbaar=true` | `RestZaaktypeKeuze` | `uuid`, `omschrijving` |
| `GET /zaaktypen/{uuid}` | `RestZaaktypeAanmaakInstellingen` | `uuid`, `omschrijving`, `vertrouwelijkheidaanduiding`, `defaultGroepId`, `defaultBehandelaarId`, `isKvkKoppelenEnabled`, `isBrpKoppelenEnabled` |
| `GET /health-check/zaaktypes` | `RestZaaktypeInrichtingscheck.zaaktype` becomes `RestInrichtingscheckZaaktype` | `uuid`, `omschrijving`, `doel`, `beginGeldigheid` |
| `GET /zaakafhandelparameters` | `RestZaaktypeConfigurationOverzicht` | `zaaktype` (`RestZaaktypeBeheerOverzicht`: `uuid`, `identificatie`, `omschrijving`, `doel`, `isNuGeldig`, `beginGeldigheid`, `eindeGeldigheid`), `processDefinition` (`RestProcessDefinitionOverzicht`: `key`, `naam`), `creatiedatum`, `isValide` |

The list items keep the nested `zaaktype` and `processDefinition` objects, so the table, filter and sort code of
the admin pages only changes its types, not its property paths.

*Alternative:* one shared `RestZaaktypeSamenvatting` for all lists. Rejected: the Inrichtingscheck page would still
receive three fields that it does not show, which the acceptance criteria rule out, and a shared type grows again
when one page needs one more field.

*Alternative:* keep the types and leave fields `null` (JSON-B skips nulls). Rejected: the OpenAPI spec and the
generated frontend types would still promise fields that never arrive.

The settings of the chosen zaaktype carry the two koppeling booleans flat, and do not reuse
`RestBetrokkeneKoppelingen`: that type also has an `id` and a back reference to the configuration.

### D2. One predicate decides what is offered for zaak creation

The list and the new read endpoint use one private check in `ZaaktypeRestService`: not a concept, valid today, the user
may start a zaak of it, and `isValidForZaakCreation` (configuration valid and the process engine ready). The read
endpoint first checks the start right with `assertPolicy` (HTTP 403), then the other conditions, and throws a
`NotFoundException` (HTTP 404) when the zaaktype is not offered. An unknown UUID also gives 404. This keeps the
list and the read in step: the form cannot read settings of a zaaktype that the list would not show.

*Alternative:* return the settings of any zaaktype the user may start. Rejected: it would answer for a zaaktype
that cannot be used to create a zaak.

### D3. The form fetches the settings with a TanStack query keyed by the chosen UUID

`ZakenService` gets `readZaaktypeForCreationQuery(uuid)` built with `zacQueryClient.GET`, following the existing
`listZaaktypesForCreationQuery`. `ZaakCreateComponent` holds the chosen `RestZaaktypeKeuze` in the form, and an
`injectQuery` on the settings, enabled when a zaaktype is chosen. The logic that now runs synchronously in
`caseTypeSelected` and in the `valueChanges` subscription (set vertrouwelijkheidaanduiding, enable groep, select
default groep and behandelaar, enable or clear the initiator, apply the copied initiator) runs when the settings
for the chosen UUID arrive. `canAddInitiator` and `initiatorAllowedByZaaktype` read the settings, not the list
item. The submit button stays disabled while the settings of the chosen zaaktype load. A failed read shows the
error dialog of the query client, as for other queries, and the zaaktype stays chosen without settings, so the
form cannot be submitted.

The create request sends `zaaktype: { uuid }`. `RestZaak.zaaktype` stays `RestZaaktype`, whose only required field
is `uuid`, so the POST contract does not change.

*Alternative:* fetch the settings inside `createZaak` on the server and drop them from the form. Rejected: the form
shows the default groep, behandelaar and vertrouwelijkheidaanduiding before the user submits.

### D4. The configuration list converts from the zaaktype already in hand

`listZaaktypeConfigurations` already has each `ZaakType` from `listZaaktypen`. A new converter function builds the
`RestZaaktypeConfigurationOverzicht` from that zaaktype and the stored configuration (or an unsaved one). It
reads the process definition name through `RestProcessDefinitionConverter.toRestProcessDefinition(binding,
inclusiefRelaties = false)`, which caches per request, and skips `isZaakspecifiekAutoriseerbaar`, SmartDocuments
and the other settings. The list therefore does fewer ZTC lookups as well as sending less data. `isValide` keeps
using `ZaaktypeConfiguration.isValidForZaakCreation()`, so it agrees with the health check.

### D5. Two code layers, split by screen

Layer 1 changes the two admin lists, backend and frontend together. Layer 2 changes zaak creation, backend and
frontend together. Each endpoint change alters the generated frontend types, so a backend-only layer would not
compile the frontend. The split by screen keeps the small, mechanical admin change apart from the zaak creation
change, which moves logic in the form to the moment the settings arrive.

### D6. The zaaktype choice is served from `/rest/zaaktypen`

The list and the read move to a new `ZaaktypeRestService` under `/rest/zaaktypen`. The list becomes
`GET /rest/zaaktypen?startbaar=true` and the read becomes `GET /rest/zaaktypen/{zaaktypeUuid}`. A zaaktype is then
addressed as a zaaktype, and "offered for zaak creation" is a filter on the collection instead of a path under
`/zaken`. Without `startbaar`, the collection lists every zaaktype of the default catalogus as the same list item, in
the order of the ZTC. With `startbaar=false`, it lists the zaaktypes that `startbaar=true` leaves out. The read keeps
the checks of D2, so it answers only for a zaaktype that `startbaar=true` offers.

The other endpoints that serve zaaktype data, such as `/zaken/statustypes/{zaaktypeUUID}` and
`/zaakafhandelparameters/{zaaktypeUUID}`, stay where they are. Moving them is outside PZ-13414.

*Alternative:* keep `/rest/zaken/zaaktypes-for-creation` and add the read under it. Rejected: it names a resource
after the form that reads it, and a later move to `/rest/zaaktypen` would break the new read again.

## Risks / Trade-offs

- [The form reacts later to a zaaktype choice, which can break the order of the groep prefill, behandelaar
  prefill and initiator handling.] → The specs of `ZaakCreateComponent` and `ZaakLinkComponent` cover each step
  with the settings response from `HttpTestingController`, including a quick switch between two zaaktypes where
  the first response arrives after the second choice; only settings for the zaaktype that is still chosen are
  applied.
- [Without `startbaar`, the collection lists zaaktypes that the user may not start.] → An item carries only the
  UUID and the omschrijving, which are catalogue data and not zaak data. The frontend reads only
  `startbaar=true`.
- [One more request when the user chooses a zaaktype.] → The request is small, and TanStack caches it per UUID, so
  choosing the same zaaktype again does not call the server.
- [The main spec `zaaktype-configuration` still has the requirement "The configuration REST contract stays
  unchanged", which the active change `unify-zaaktype-configuration-frontend` removes.] → This change only adds a
  requirement. When it is archived before that change, the main spec briefly holds both; the new requirement is
  the more specific one for the list.
- [The e2e tests for zaak creation choose a zaaktype and fill the form at once.] → Playwright waits for the
  enabled submit button and the filled fields, so the extra request only adds waiting time. Run the zaak creation
  e2e features against layer 2.

## Migration Plan

No data migration. The frontend and backend are deployed together in one image, and only this repository calls
the endpoints, so each layer changes both sides at once. Rollback is a revert of the layer.

## Assumptions

- The type names in D1 are suggestions; the implementer may pick other names that follow the repository
  conventions, as long as the JSON field names stay as listed.
- The inrichtingscheck results stay complete: every result feeds a finding in the expanded row of the page, so
  each is "needed for the list".
- `id` of the configuration is not needed by the Zaakafhandelparameters list: the row links by zaaktype UUID.
- `processEngine` is not needed in the configuration list: the page filters on `key` and shows `naam`.
- The read endpoint answers 403 before 404 when the user may not start the zaaktype, also when the zaaktype is
  not offered for another reason.
- The list for zaak creation keeps the order that the ZTC client returns.
- `GET /rest/zaken/gekoppelde-zaken/zaaktypen` stays unchanged (see Non-Goals).
- No test-data script in `scripts/test-data/` calls the changed endpoints; the create request that
  `zac_testdata.py` sends already names the zaaktype by UUID.
- No design is linked to the item, and no screen changes.
