## Why

To decide whether a medewerker may access a zaak, ZAC needs two answers:
- is the zaak zaakspecifiek geautoriseerd (zsa)?
- which medewerkers are individually authorised for it?

Today every place that needs these answers works them out on its own:
- `PolicyService` does so in each of its zaak, taak and document overloads;
- `RestZaakConverter` does so to show the zsa indicator;
- the taak and document lists of a zaak do so once per item;
- `ReindexSupportService` does so for the search index.

Each of these makes its own calls to Open Zaak, so opening one zaak asks Open Zaak twice whether it is zsa, and a
list of N taken asks N + 1 times. The check "is the logged-in user one of the geautoriseerde medewerkers" is also
written out six times in `PolicyService`. When this rule changes, all of these places must change together.

[PZ-12905](https://dimpact.atlassian.net/browse/PZ-12905) puts this in one place, as a follow-up to
[PZ-12506](https://dimpact.atlassian.net/browse/PZ-12506). It is a pure refactoring: **no rechten, responses or
search results change.**

## What Changes

- **One place works out the zsa access data of a zaak.** `ZaakspecifiekeAutorisatieService` gets one function that
  returns the existing `ZaakAutorisatieGegevens`: whether the zaak is zsa, plus the geautoriseerde medewerkers,
  which are read only when somebody asks for them. That function replaces the derivation that now lives privately in
  `ReindexSupportService`, so the search index and the rechten use exactly the same answer.
- **One check for "is this user a geautoriseerde medewerker".** `ZaakAutorisatieGegevens` gets this check, and the
  six copies in `PolicyService` are replaced by it. The three overloads that work on a zoekobject build the same
  object from the fields they already have.
- **Callers that already know the answer pass it on instead of asking again.** The live `PolicyService` overloads
  accept a `ZaakAutorisatieGegevens` that was already read. These callers read it once and reuse it:
  - opening a zaak (rechten and the zsa indicator);
  - listing the taken of a zaak;
  - listing the documenten of a zaak.
- **After a write the data is read again.** Where a request changes the zsa marking or the rollen of a zaak, the
  response is built from freshly read data, exactly as today.
- **Out of scope**, as decided by the developer:
  - merging the zaak and taak assignment (write) flows;
  - access checks that are not there today (documenten without zaak context, taak signaleringen, notities, bulk
    verdelen of zaken);
  - OPA rego changes;
  - any change to Java code.

## Capabilities

### New Capabilities

- `zaakspecifieke-autorisatie-toegangsbepaling`: how ZAC works out the zsa access data of a zaak. The rechten and
  responses must stay identical; the data is worked out once per zaak per request where the caller already has it;
  and it is read again after a write.

### Modified Capabilities

None. The rechten that `zaakspecifieke-autorisatie-toegang` and `zaakspecifiek-geautoriseerde-zoekindex` describe do
not change.

## Impact

- **Backend**:
  - `ZaakspecifiekeAutorisatieService`: the new function, taking over the derivation from `ReindexSupportService`.
  - `ZaakAutorisatieGegevens`: the new check.
  - `PolicyService`: the live overloads take an optional `ZaakAutorisatieGegevens`, and all overloads use the shared
    check.
  - `RestZaakConverter`, `RestTaskConverter`, `RestInformatieobjectConverter`: accept the already-read data.
  - `ZaakRestService`, `TaskRestService`, `EnkelvoudigInformatieObjectRestService`: pass it along on the read paths.
- **Open Zaak**: fewer calls on the paths above; no other change.
- **OPA, Solr, frontend, REST API**: no change.
