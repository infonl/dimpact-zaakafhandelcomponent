# About ZAC

ZAC (zaakafhandelcomponent) is a Dutch case management app for municipalities. Employees handle
*zaken* (cases) and *taken* (tasks). The UI is Angular with Angular Material. It follows the browser
language (Dutch or English); your tests run with a Dutch browser, so look up labels in
`src/main/app/src/assets/i18n/nl.json`, and the templates under `src/main/app/src/app/`. The Keycloak
login page stays in English.

Backend changes in `src/main/kotlin` and `src/main/java` (Java to Kotlin conversions, package moves) are
usually `not-testable`, unless the PR changes what a user sees.

## Pages

| Path | What |
|---|---|
| `/` | Dashboard with cards (mijn zaken, mijn taken, signaleringen) |
| `/zaken/werkvoorraad`, `/zaken/mijn`, `/zaken/afgehandeld` | Zaak lists |
| `/zaken/create` | Start a new zaak |
| `/zaken/<ZAAK-yyyy-n>` | Zaak detail: gegevens, taken, documenten, notities, betrokkenen |
| `/taken/werkvoorraad`, `/taken/mijn` | Taak lists |
| `/documenten/inbox`, `/documenten/ontkoppelde` | Document inboxes |
| `/productaanvragen/inbox` | Product requests inbox |
| `/persoon/<id>`, `/bedrijf/<id>` | Person and company detail |
| `/signaleringen/settings` | Signalering settings of the logged-in user |
| `/fout` | Error pages, e.g. the 403 page |
| `/admin/...` | Beheer: zaaktype configuration, groepen, mail templates, referentietabellen |

Search is the "Zoeken" field in the toolbar; Enter opens the results in a side panel. It has no route.

## Test users

Their exact rights are unknown; bob can open the admin pages.

| Helper name | Display name | Group |
|---|---|---|
| `bob` | E2etest User1 | Test groep A |
| `oscar` | E2etest User2 | Test groep B |

## Test data

Zaaktypes "Zaaktype voor e2e testen" (CMMN) and "Zaaktype voor BPMN e2e testen" (BPMN), test person
Héndrika Janse with BSN 999993896, postcode 1112GV.

- Create zaken only with `createZaak(page, { type, omschrijving })` from `helpers.ts`; the omschrijving
  then starts with "QA Agent". A BPMN zaak is assigned to bob.
- Create as few zaken as you can: only for tests about zaak behaviour, and one zaak per spec file,
  created in `test.beforeAll` and shared by its tests. Pages and lists need no zaak of your own.
- Don't abort or close your zaken yourself, unless that is what you test: after the run, every open
  "QA Agent" zaak is aborted automatically.

## Project rules

The dev environment is shared with the team and the nightly e2e run.

- Only change zaken you created in this run.
- On `/admin` pages, look but never save.
- Never send e-mail to a real address; use `@example.com`.
- Only use the test person above, never another BSN.
