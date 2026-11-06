## Context

`HealthCheckService.controleerZaaktypeResultaattypeInrichting` already loops over the resultaattypen and collects
those with afleidingswijze `ingangsdatum_besluit` or `vervaldatum_besluit`. `ZaaktypeInrichtingscheck.isValide`
drives the page, and `CmmnProcessBinding.isZaaktypeReady` uses `controleerZaaktype(...).isValide`, which is why
an invalid CMMN zaaktype cannot be used to create a zaak. BPMN zaaktypes never go through this check (see the
open change `inrichtingscheck-zonder-bpmn`, which also hides them from the page), so "CMMN only" follows
without extra code.

## Goals / Non-Goals

**Goals:** warn about `ander_datumkenmerk` and `zaakobject` resultaattypes in the inrichtingscheck, block zaak
creation for that zaaktype, document it.

**Non-Goals:** supporting these afleidingswijzen; changing how a zaak is closed; BPMN zaaktypes.

## Decisions

- `ZaaktypeInrichtingscheck` gets `resultaattypesMetNietOndersteundeBrondatum: MutableList<String?>`, filled next to
  `resultaattypesMetVerplichtBesluit`, and `isValide` requires it to be empty. A list, not a boolean, so the error
  can name the resultaattypes, as the besluittype error does.
- The REST model exposes the list; the frontend shows the error when it is non-empty, like the besluittype error.
- The error follows the existing `health-check.zaaktype.*` i18n key pattern, in `nl.json` and `en.json`. The row
  also needs a category label in the collapsed title column (`resultaattype`), as for the other errors.

## Risks / Trade-offs

- Existing CMMN zaaktypes with such a resultaattype can no longer be used until fixed in Open Zaak. This is the
  intent of the item.

## Assumptions

- The Jira item says a design of the error text exists, but it links none and none was available. The text is
  assumed: nl "Resultaattype(s) \"<b>{{resultaattypes}}</b>\" hebben een afhandelwijze brondatum die ZAC niet
  ondersteunt ('Ander datumkenmerk' of 'Zaakobject')"; en equivalent. Replace it if the design differs.
- The alignment of the error messages is "improved" according to the item, but nothing says what to change. It is
  out of scope here.
- Only resultaattypen found by `readResultaattypen` (the zaaktype's own, valid ones, as today) are checked.
- The layout stays as it is: no new column or screen, so there is no `Design` section.
