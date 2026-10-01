## Why

A user whose only ZAC application roles are roles without read ('lezen') rights, for example only `brp_zoeken`,
can log in to ZAC and gets the dashboard, which then fails with a 4xx "insufficient permissions" message.
Such a user cannot do anything useful in ZAC. They should get the same "U heeft geen toestemming om deze pagina
te bekijken." page, with only a log-out button, that a user without any ZAC application role already gets.

## What Changes

- `RequestAuthorizationFilter` admits a request on an authenticated, non-admin path only when the logged-in user
  holds at least one read role (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager` or `beheerder`),
  either for a zaaktype or as an overall role. Before this change, any application role was enough.
- The read roles are taken from the existing OPA `leesrollen` rule, so OPA stays the single source of truth for
  which roles grant read rights.
- A user who holds only non-read roles, such as `brp_zoeken` or `zaakspecifiek_geautoriseerd`, gets a 403 on
  every authenticated path, including the Angular app, and therefore sees the existing 403 error page.
- Admin paths keep requiring `beheerder`. Public paths are unchanged.

## Capabilities

### New Capabilities
- `application-access`: which logged-in users the generic request authorization filter lets into ZAC, and which
  get the 403 error page.

### Modified Capabilities
<!-- none -->

## Impact

- Code: `UserPrincipalFilter` (now injects `PolicyService`), `LoggedInUser` (new `hasReadApplicationRole` flag),
  `RequestAuthorizationFilter`, and their unit tests.
- Runtime: one extra OPA call (`rol/leesrollen`) per session, at login, for a user who holds application roles. If
  OPA does not return the rule, the first request of the session fails instead of the user being let in.
- Users: users with only non-read roles lose access to the ZAC UI and REST API. They had no working
  functionality before. No change for users with a read role.
