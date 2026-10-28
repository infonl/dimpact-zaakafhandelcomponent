# About ZAC

ZAC (zaakafhandelcomponent) is a Dutch case management app for municipalities. Employees handle
*zaken* (cases) and *taken* (tasks). The UI is Angular with Angular Material. It follows the browser
language (Dutch or English); your tests run with a Dutch browser, so look up labels in
`src/main/app/src/assets/i18n/nl.json`, and the templates under `src/main/app/src/app/`.

Backend changes in `src/main/kotlin` and `src/main/java` (Java to Kotlin conversions, package moves) are
usually `not-testable`, unless the PR changes what a user sees.

## Test users

`globalSetup.ts` logs both users in before your run, so tests start logged in as user 1. Logged in means
the profile icon `account_circle` shows (`page.getByText("account_circle").first()`). Their exact rights
are unknown; user 1 can open the admin pages.

| User | Display name | Group | Session |
|---|---|---|---|
| user 1 | E2etest User1 | Test groep A | `auth/user1.json` (default) |
| user 2 | E2etest User2 | Test groep B | `auth/user2.json` |

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

## Creating a zaak

When a ticket is about a zaak, its detail page, its notities, betrokkenen or taken, create one with the
`createZaak` helper instead of marking the ticket `needs-human`. It returns the zaaknummer; the zaak opens
on `/zaken/<zaaknummer>`. A BPMN zaak is assigned to user 1 and starts with a taak for that user.

```ts
import { test, expect } from "../fixtures";
import { createZaak } from "../project/createZaak";

let zaaknummer: string;

// One zaak per spec file, shared by its tests. A page from browser.newPage() doesn't get the config's
// baseURL and session, so pass them.
test.beforeAll(async ({ browser }, testInfo) => {
  test.setTimeout(180_000);
  const page = await browser.newPage({
    baseURL: testInfo.project.use.baseURL,
    storageState: "auth/user1.json",
    locale: "nl-NL",
  });
  zaaknummer = await createZaak(page, { type: "CMMN", omschrijving: "<what the test is about>" });
  await page.close();
});
```

Only create zaken this way, so the omschrijving starts with "QA Agent:". Create as few as you can: one per
spec file, and none for tests of pages and lists. Don't abort or close your zaken yourself, unless that is
what you test: `globalTeardown.ts` aborts them after the run.

## Project rules

The dev environment is shared with the team and the nightly e2e run.

- Only change zaken you created in this run.
- On `/admin` pages, look but never save.
- Never send e-mail to a real address; use `@example.com`.
- Only use the test person above, never another BSN.
