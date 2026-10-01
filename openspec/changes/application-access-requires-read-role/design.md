## Context

`RequestAuthorizationFilter` runs after `UserPrincipalFilter` on every request. For an authenticated, non-admin
path it now only checks that the `LoggedInUser` has any role in `applicationRolesPerZaaktype` or `overallRoles`.
A 403 from the filter is rendered by the `error-page` mapping in `web.xml` as `/static/error-403.html`, which is
the "U heeft geen toestemming om deze pagina te bekijken." page with a log-out button. So the frontend needs no
change: once the filter refuses the Angular app's page request, the user sees that page.

The read roles are already defined once, as the `leesrollen` set in `rollen.rego`, and read by ZAC through
`PolicyService.readLeesrollen()` (used by `SearchService` since the `zoekresultaten-leesrecht` change).

The filter is mapped to `/*` in `web.xml`, and Angular serves its chunks from the root. So `index.html`, every JS
chunk and every REST call pass through it, which comes to dozens of requests on a cold load. `UserPrincipalFilter`
builds the `LoggedInUser` from PABC once per session and stores it in the HTTP session.

`systeemrol_behandelaar_alle_zaaktypen` is only hardcoded to the internal system users in `LoggedInUserProvider`,
which never send HTTP requests, and `UserPrincipalFilter` strips it from a real user's overall roles. So the filter
never needs to admit it.

## Goals / Non-Goals

**Goals:**
- Refuse users without read rights in the filter, with the same 403 as users without any role.

**Non-Goals:**
- No change to the admin-path check or to the public paths.
- No change to `leesrollen` in OPA or to the OPA `lezen` rules.
- No frontend change.

## Decisions

### Determine "has a read role" once per session, in `UserPrincipalFilter`

When `UserPrincipalFilter` builds the `LoggedInUser`, it calls `policyService.readLeesrollen()` and sets
`LoggedInUser.hasReadApplicationRole` to whether any role in `applicationRolesPerZaaktype` values or `overallRoles`
is in that set. `RequestAuthorizationFilter` only reads that flag, so it makes no OPA call.

The flag has the same lifetime as the roles it is derived from, which are also fixed for the session. The flag
defaults to `false`, so a `LoggedInUser` built elsewhere, such as the internal system users, never passes the filter.

Alternative: call OPA from `RequestAuthorizationFilter` on every request. Rejected, because the filter runs for
every static file and REST call, so a cold load would make dozens of OPA calls.

Alternative: cache `leesrollen` in the filter. Rejected, because it adds invalidation questions when the policy
bundle changes, while the per-session flag needs no cache at all.

### Read the read roles from OPA, not from a Kotlin constant

Alternative: hard-code the five read roles in `ZacApplicationRole`. Rejected, because then the read roles are
defined in two places and `rollen.rego` says ZAC reads them from OPA.

### Skip OPA for users without application roles

`UserPrincipalFilter` sets the flag to `false` without calling OPA when the user holds no application role at all.
So the existing no-role behaviour does not depend on OPA.

### Do not add `systeemrol_behandelaar_alle_zaaktypen` to `leesrollen` or to the filter

Adding it to `leesrollen` would grant it `lezen` in `taak-rechten.rego` and `document-rechten.rego`, where it now has
no rights, and change search results. Handling it in the filter is dead code for the system users, and would let in
a real user to whom a misconfigured PABC hands it out per zaaktype, which `UserPrincipalFilter` does not strip.

### Keep the admin check as it is

`beheerder` is a read role, so every user who passes the admin check also passes the new check. The admin branch
stays separate.

## Risks / Trade-offs

- [OPA unavailable or `rol/leesrollen` missing at login] → `readLeesrollen()` throws, and the first request of the
  session fails with a 500 instead of the user being let in. This fails closed, which is the right default for an
  authorization check. Users without any role still get the 403 page, because they skip the OPA call.
- [`leesrollen` changes during a session] → The flag keeps its old value until the next session, the same as the
  roles from PABC already do.
- [Users with only non-read roles lose access] → Intended. They could not use ZAC before. Mention it in the PR, so
  that functional administrators know these users need a read role.
