## Context

`RequestAuthorizationFilter` is mapped to `/*` and runs after `UserPrincipalFilter`. For a logged-in user with a
read role it only checks the admin prefixes (`/admin`, `/rest/admin/`) against the `beheerder` role. A plain
`sendError(403)` is rendered by the `web.xml` error page `/static/error-403.html`, the "U heeft geen toestemming om
deze pagina te bekijken." page with a home button.

The Angular app is served with `<base href="/">`, so its JS chunks load from the root and its assets from
`/assets/`. A request whose path is an Angular page, such as `/documenten/inbox`, reaches the filter only on a full
page load: a typed URL, a bookmark, a link from outside ZAC or a reload. Navigation inside the app makes no request
for the page.

The toolbar shows its links from `GET /rest/policy/werklijstRechten` and `GET /rest/policy/overigeRechten`, and the
REST endpoints of these pages assert the same rights, for example `InboxDocumentRestService`,
`DetachedDocumentRestService` and `InboxProductaanvraagRestService` assert `werklijstRechten.inbox`. These rights come
from OPA through `PolicyService.readWerklijstRechten()` and `PolicyService.readOverigeRechten()`.

Pages and the right that guards them:

| Path                                           | Right                     | Roles that hold it                                       |
|------------------------------------------------|---------------------------|----------------------------------------------------------|
| `/documenten`, `/documenten/...`               | werklijst `inbox`         | coordinator, recordmanager, beheerder                    |
| `/productaanvragen`, `/productaanvragen/...`   | werklijst `inbox`         | coordinator, recordmanager, beheerder                    |
| `/informatie-objecten` (redirects to inbox)    | werklijst `inbox`         | coordinator, recordmanager, beheerder                    |
| `/zaken/create`                                | overige `startenZaak`     | behandelaar, coordinator, recordmanager, beheerder       |

The zaak and taak werklijsten (`/zaken/werkvoorraad`, `/zaken/mijn`, `/zaken/afgehandeld`, `/taken/werkvoorraad`,
`/taken/mijn`) need werklijst `zakenTaken`, which every read role holds, so the existing read role check covers them.

## Goals / Non-Goals

**Goals:**
- Refuse a full page load of a guarded page with the same 403 page as `/admin`, based on the same OPA right as the
  toolbar link.

**Non-Goals:**
- No Angular route guards. The toolbar already hides the links, and the `/admin` check is server-side only too.
- No check on pages of a single zaak, taak or document: their access depends on the object, which the REST call
  already checks.
- No change to the read role check, OPA policies or the REST endpoints. The admin check only changes in which form of
  the path it matches.
- No new error page or text.

## Decisions

### Call OPA from the filter, only for guarded page paths

`RequestAuthorizationFilter` injects `PolicyService`. After the read role check and the admin check, it looks up the
right that the request path needs. Only when the path is a guarded page does it call
`policyService.readWerklijstRechten()` or `policyService.readOverigeRechten()`, and returns `FORBIDDEN` when the right
is `false`. Every other request makes no OPA call.

The guarded pages are a small list of path rules in the filter's companion object, each with the right it needs. A
prefix rule matches the path itself and any path below it (`/documenten` and `/documenten/inbox`, not
`/documentenx`). `/informatie-objecten` matches only the exact path, with or without a trailing slash, because
`/informatie-objecten/<uuid>` is the page of one document.

Alternative: compute the flags once per session in `UserPrincipalFilter` and store them on `LoggedInUser`, as the
read role flag is. Rejected, because it adds two OPA calls to every login for flags that are only needed on a full
page load of a few pages, and it grows `LoggedInUser` for this one filter.

Alternative: check the roles in Kotlin, as `hasBeheerderApplicationRole` does. Rejected, because then the roles per
right are defined both in the `.rego` files and in Kotlin, and the toolbar and the page could disagree.

### Match the admin and page rules on the servlet path, not on the request URI

The request URI is raw, so `/%64ocumenten/inbox` or `/admin;view=list` would not match a rule, while the
container and the app still treat it as the guarded page. The admin and page rules therefore match on
`servletPath + pathInfo`, which the container has decoded, normalized and stripped of path parameters. The public
path rules keep matching on the raw request URI: a variant that does not match them falls through to the
authenticated checks, which is stricter.

### Refuse with the existing 403 page

The filter returns `Authorization.FORBIDDEN`, which sends a plain 403 and shows `/static/error-403.html`, as for
`/admin`. The read role check comes first, so a user without a read role still gets the base role page.

## Risks / Trade-offs

- [OPA unavailable during a full page load of a guarded page] → `requireResult` throws and the user gets the 500
  page instead of a page whose REST calls fail. The rest of ZAC is unaffected.
- [A new Angular page that needs a right is not added to the list] → It behaves as today: the page loads and its
  REST calls fail. The path rules sit next to the admin prefixes, so a reviewer sees them.
- [In-app navigation to a guarded page is not refused] → Only reachable through links that the toolbar hides by the
  same right, or through the browser history after the rights changed in the same session. The REST calls still
  refuse the data.
- [Angular route paths renamed] → The filter rules must follow. A unit test per guarded path names each path.
