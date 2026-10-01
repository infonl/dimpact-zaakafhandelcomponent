## Context

`RequestAuthorizationFilter` runs after `UserPrincipalFilter` on every request. For an authenticated, non-admin
path it now only checks that the `LoggedInUser` has any role in `applicationRolesPerZaaktype` or `overallRoles`.
A 403 from the filter is rendered by the `error-page` mapping in `web.xml` as `/static/error-403.html`, which is
the "U heeft geen toestemming om deze pagina te bekijken." page with a log-out button. So the frontend needs no
change: once the filter refuses the Angular app's page request, the user sees that page.

The read roles are already defined once, as the `leesrollen` set in `rollen.rego`, and read by ZAC through
`PolicyService.readLeesrollen()` (used by `SearchService` since the `zoekresultaten-leesrecht` change).
`PolicyService` is `@ApplicationScoped`, so the filter can inject it.

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

### Read the read roles from OPA, not from a Kotlin constant

The filter calls `policyService.readLeesrollen()` and admits the user when any role in
`applicationRolesPerZaaktype` values or `overallRoles` is in that set.

Alternative: hard-code the five read roles in `ZacApplicationRole`. Rejected, because then the read roles are
defined in two places and `rollen.rego` says ZAC reads them from OPA.

### Check the cheap conditions first

The filter returns `false` without calling OPA when there is no session, no logged-in user, or the user holds no
application role at all. Only otherwise it calls OPA. So users without roles cost nothing extra,
and the existing no-role behaviour does not depend on OPA.

### Do not add `systeemrol_behandelaar_alle_zaaktypen` to `leesrollen` or to the filter

Adding it to `leesrollen` would grant it `lezen` in `taak-rechten.rego` and `document-rechten.rego`, where it now has
no rights, and change search results. Handling it in the filter is dead code for the system users, and would let in
a real user to whom a misconfigured PABC hands it out per zaaktype, which `UserPrincipalFilter` does not strip.

### Keep the admin check as it is

`beheerder` is a read role, so every user who passes the admin check also passes the new check. The admin branch
stays separate and does not call OPA.

### Per-request OPA call instead of caching

`leesrollen` is a constant in the policy bundle, and the call is a cheap data lookup. ZAC already makes several OPA
calls per REST request. Caching would add invalidation questions when the policy bundle changes, for a small gain.

Alternative: compute "has read access" once in `UserPrincipalFilter` and store it on `LoggedInUser`. Rejected for
now, because roles in the session are already rebuilt per session and the extra field is not needed for a
one-call lookup. It can be added later if profiling shows the call matters.

## Risks / Trade-offs

- [OPA unavailable or `rol/leesrollen` missing] → `readLeesrollen()` throws, and the request fails with a 500
  instead of being let through. This fails closed, which is the right default for an authorization check. Users
  without any role still get the 403 page, because that check runs before the OPA call.
- [Extra latency per request] → One small OPA call for users with roles. Acceptable, see Decisions.
- [Users with only non-read roles lose access] → Intended. They could not use ZAC before. Mention it in the PR, so
  that functional administrators know these users need a read role.
