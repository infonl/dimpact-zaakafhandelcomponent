## ADDED Requirements

### Requirement: Pages that need a right are refused to users without that right

ZAC SHALL answer a request for one of the following pages with HTTP 403 and the error page "U heeft geen
toestemming om deze pagina te bekijken.", which has a home button and no log-out button, when the logged-in user
holds a read role but not the right the page needs:

- the pages under `/documenten/`, the pages under `/productaanvragen/` and `/informatie-objecten` itself: the
  werklijst right `inbox`, which `coordinator`, `recordmanager` and `beheerder` hold;
- `/zaken/create`: the right `startenZaak`, which `behandelaar`, `coordinator`, `recordmanager` and `beheerder` hold.

These are the same rights that decide whether the toolbar shows the link to the page. ZAC SHALL serve these pages to
a user who holds the right. A user without a read role SHALL keep getting the base role page on these paths.
Other paths, such as the pages of a single zaak, taak or document under `/zaken/`, `/taken/` and
`/informatie-objecten/`, and the REST API, SHALL keep their existing access rules.

ZAC SHALL apply these rules and the rule for the admin paths in the same way to a variant of a path that has
percent-encoded characters or path parameters (`;name=value`), such as `/%64ocumenten/inbox` or `/admin;view=list`.

#### Scenario: A raadpleger cannot open the documenten inbox
- **WHEN** a user who holds only `raadpleger` opens `/documenten/inbox`
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft geen toestemming om deze pagina te bekijken." page with a
  home button and no log-out button

#### Scenario: A behandelaar cannot open the ontkoppelde documenten or the productaanvragen inbox
- **WHEN** a user who holds only `behandelaar` opens `/documenten/ontkoppelde` or `/productaanvragen/inbox`
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft geen toestemming om deze pagina te bekijken." page

#### Scenario: A behandelaar cannot open the informatie-objecten root that redirects to the inbox
- **WHEN** a user who holds only `behandelaar` opens `/informatie-objecten`
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft geen toestemming om deze pagina te bekijken." page

#### Scenario: A coordinator can open the inbox pages
- **WHEN** a user who holds `coordinator` for one zaaktype opens `/documenten/inbox`, `/documenten/ontkoppelde` or
  `/productaanvragen/inbox`
- **THEN** ZAC serves the request

#### Scenario: A raadpleger cannot open the page to start a zaak
- **WHEN** a user who holds only `raadpleger` opens `/zaken/create`
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft geen toestemming om deze pagina te bekijken." page

#### Scenario: A behandelaar can open the page to start a zaak
- **WHEN** a user who holds `behandelaar` for one zaaktype opens `/zaken/create`
- **THEN** ZAC serves the request

#### Scenario: A raadpleger can still open a document and the zaak werklijsten
- **WHEN** a user who holds only `raadpleger` opens `/informatie-objecten/<uuid>`, `/zaken/werkvoorraad` or
  `/taken/mijn`
- **THEN** ZAC serves the request

#### Scenario: A user without a read role gets the base role page on a guarded page
- **WHEN** a user who holds only `brp_zoeken` opens `/documenten/inbox`
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft minstens één basisrol nodig om deze applicatie te kunnen
  gebruiken." page

#### Scenario: An encoded or parameterised variant of a guarded page is refused as well
- **WHEN** a user who holds only `behandelaar` opens `/%64ocumenten/inbox` or `/documenten/inbox;view=list`
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft geen toestemming om deze pagina te bekijken." page

#### Scenario: An encoded or parameterised variant of an admin path is refused as well
- **WHEN** a user who holds `behandelaar` but not `beheerder` opens `/%61dmin` or `/admin;view=list`
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft geen toestemming om deze pagina te bekijken." page
