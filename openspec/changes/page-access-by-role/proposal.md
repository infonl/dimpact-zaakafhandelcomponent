## Why

A user without the right for a page, such as a `raadpleger` or `behandelaar` who opens `/documenten/inbox`, gets the
Angular page anyway. The REST calls of that page then fail with 403, so the user sees an empty list and error
messages. For `/admin`, ZAC already refuses the page itself with the "U heeft geen toestemming om deze pagina te
bekijken." page. The other pages that the toolbar only shows to users with a certain right have no such check.

## What Changes

- `RequestAuthorizationFilter` refuses a request for one of these Angular pages with HTTP 403 and the existing
  "U heeft geen toestemming om deze pagina te bekijken." page, when the user lacks the right that the toolbar uses
  to show the link to that page:
  - `/documenten/` (inbox and ontkoppelde documenten), `/productaanvragen/` and `/informatie-objecten` (which
    redirects to the documenten inbox): the werklijst right `inbox` (`coordinator`, `recordmanager`, `beheerder`).
  - `/zaken/create`: the overige right `startenZaak` (`behandelaar`, `coordinator`, `recordmanager`, `beheerder`).
- The filter reads these rights from OPA, the same way the toolbar and the REST endpoints of these pages do.
- The admin check, the read role check and the public paths stay as they are.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `application-access`: adds a requirement that the pages of the inbox werklijsten and the page to start a zaak
  need the same right as the toolbar link to them.

## Impact

- Code: `RequestAuthorizationFilter` (now injects `PolicyService`) and `RequestAuthorizationFilterTest`.
- Runtime: one OPA call per full page load of a guarded page. Navigation inside the Angular app, JS chunks, assets
  and REST calls do not pass this check, so they make no extra OPA call.
- Users: a `raadpleger` or `behandelaar` who opens a guarded page by URL, bookmark or reload gets the 403 page
  instead of a page with errors. A `raadpleger` gets it for `/zaken/create` too. No change for users who hold the
  right.
- Frontend: no change.
