# About ZAC

ZAC (zaakafhandelcomponent) is a Dutch case management app for municipalities. Employees handle
*zaken* (cases) and *taken* (tasks). The UI is Angular with Angular Material. It follows the browser
language (Dutch or English); your tests run with a Dutch browser, so look up labels in
`src/main/app/src/assets/i18n/nl.json`, and the templates under `src/main/app/src/app/`.

Backend changes in `src/main/kotlin` and `src/main/java` (Java to Kotlin conversions, package moves) are
usually `not-testable`, unless the PR changes what a user sees.

## Logging in

- Open `/`. It redirects to Keycloak, whose login page stays in English.
- Fill "Username or email" and "Password" (use `{ exact: true }` for the password label), click "Sign In".
- Logged in when the profile icon `account_circle` shows (`page.getByText("account_circle").first()`);
  this can take up to 30 seconds.

Test users (their exact rights are unknown; user 1 can open the admin pages):

| Credentials | Display name | Group |
|---|---|---|
| `QA_USERNAME`, `QA_PASSWORD` (user 1) | E2etest User1 | Test groep A |
| `QA_USERNAME_2`, `QA_PASSWORD_2` (user 2) | E2etest User2 | Test groep B |

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

Only create zaken this way, so the omschrijving starts with "QA Agent:". Put the steps in a helper and use
exactly these locators; they are known to work, and simpler ones match the wrong element.

1. `page.goto("/")`, then `page.getByLabel("Zaak toevoegen").click()`.
2. Zaaktype. The zaaktypes load slowly, so retry both clicks together:
   `await expect(async () => { await page.getByRole("combobox", { name: "Zaaktype" }).click(); await page.getByRole("option", { name: zaaktypeName }).click({ timeout: 5_000 }); }).toPass({ timeout: 40_000 });`
   with `zaaktypeName` "Zaaktype voor e2e testen" (CMMN) or "Zaaktype voor BPMN e2e testen" (BPMN).
3. Initiator (test person Héndrika Janse):
   - `page.locator("div").filter({ hasText: /^person$/ }).click()`
   - `page.getByLabel("BSN").fill("999993896")`
   - `page.getByLabel("emoji_people Persoon").getByRole("button", { name: "Zoeken" }).click()`
   - `page.getByRole("button", { name: "Select" }).click()`
4. Address:
   - `page.locator("div").filter({ hasText: /^gps_fixed$/ }).click()`
   - `page.getByPlaceholder("Zoeken op adres, postcode of woonplaats")`: fill "1112gv", then press Enter
   - `page.getByRole("row", { name: /1112GV/ }).first().getByRole("button", { name: "Koppelen" }).click()`
   - `page.locator("mat-toolbar button mat-icon", { hasText: "close" }).click()`
5. Group: `page.getByRole("combobox", { name: "Zaak toekennen aan groep" }).fill("Test groep A")`, then
   `page.getByRole("option", { name: "Test groep A", exact: true }).click()`. For BPMN also
   `page.getByRole("combobox", { name: "Zaak toekennen aan medewerker" }).fill("E2etest User1")` and
   `page.getByRole("option", { name: "E2etest User1", exact: true }).click()`.
6. `page.getByLabel("Communicatiekanaal").click()`, `page.getByRole("option", { name: " E-mail " }).click()`,
   `page.getByLabel("Omschrijving").fill("QA Agent: <what the test is about>")`.
7. `page.getByRole("button", { name: "Aanmaken" }).click()`, then
   `page.waitForURL(/\/zaken\/ZAAK-\d{4}-\d+/, { timeout: 40_000 })`; the zaaknummer is in the URL.

Creating a zaak takes up to a minute, so give `test.beforeAll` a longer timeout:
`test.beforeAll(async ({ browser }) => { test.setTimeout(180_000); ... })`.

Create as few zaken as you can: only for tests about zaak behaviour, and one zaak per spec file, created
in `test.beforeAll` and shared by its tests. Pages and lists need no zaak of your own. Don't abort or close
your zaken yourself, unless that is what you test: the cleanup does that after the run.

## Project rules

The dev environment is shared with the team and the nightly e2e run.

- Only change zaken you created in this run.
- On `/admin` pages, look but never save.
- Never send e-mail to a real address; use `@example.com`.
- Only use the test person above, never another BSN.
