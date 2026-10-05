# application-access Specification

## Purpose

Decides which logged-in users ZAC lets in, so that a user who cannot read any zaak gets an error page that says
they need a base role, instead of a dashboard that only shows permission errors.

## Requirements

### Requirement: Only users with a read role get access to ZAC

ZAC SHALL serve a request on an authenticated, non-admin path only when the logged-in user holds at least one read
role (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager` or `beheerder`), for at least one zaaktype or as
an overall role. ZAC SHALL answer every request on an authenticated path, admin paths included, from a user without
a read role with HTTP 403 and the error page "U heeft minstens één basisrol nodig om deze applicatie te kunnen
gebruiken.", which has a log-out button and no home button. Holding only other application roles, such as `brp_zoeken`, `zaakspecifiek_geautoriseerd` or
`systeemrol_behandelaar_alle_zaaktypen`, SHALL NOT give access.

#### Scenario: A user with only brp_zoeken gets the no-permission page
- **WHEN** a user who holds only `brp_zoeken`, for one or more zaaktypen and as an overall role, logs in and opens
  ZAC
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft minstens één basisrol nodig om deze applicatie te kunnen
  gebruiken." page with a log-out button and no home button, and does not show the dashboard

#### Scenario: A user with only non-read roles cannot call the REST API
- **WHEN** a user who holds only `brp_zoeken` and `zaakspecifiek_geautoriseerd` sends a request to a ZAC REST
  endpoint that requires authentication
- **THEN** ZAC answers with HTTP 403

#### Scenario: A user without any application role gets the base role page
- **WHEN** a user who holds no ZAC application role at all opens ZAC
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft minstens één basisrol nodig om deze applicatie te kunnen
  gebruiken." page

#### Scenario: A read role for one zaaktype gives access
- **WHEN** a user holds `brp_zoeken` for zaaktype A and `raadpleger` for zaaktype B, and opens ZAC
- **THEN** ZAC serves the request

#### Scenario: Each read role on its own gives access
- **WHEN** a user holds only one of `raadpleger`, `behandelaar`, `coordinator`, `recordmanager` or `beheerder`,
  for one zaaktype, and opens ZAC
- **THEN** ZAC serves the request

#### Scenario: A read role held as an overall role gives access
- **WHEN** a user holds `behandelaar` as an overall role and no zaaktype-specific roles, and opens ZAC
- **THEN** ZAC serves the request

#### Scenario: The systeemrol for all zaaktypen does not give access
- **WHEN** a user holds only `systeemrol_behandelaar_alle_zaaktypen` for a zaaktype, because PABC is misconfigured to
  hand it out, and opens ZAC
- **THEN** ZAC answers with HTTP 403

### Requirement: Admin and public paths keep their existing access rules

Requests on admin paths (`/admin` and `/rest/admin/`) SHALL still require the `beheerder` role, for at least one
zaaktype or as an overall role. A user who holds a read role but not `beheerder` SHALL get HTTP 403 and the error
page "U heeft geen toestemming om deze pagina te bekijken.", which has a home button and no log-out button. Requests on the paths that ZAC serves without authentication SHALL still be
allowed for the same HTTP methods as before, whatever roles the user holds.

#### Scenario: A user with only non-read roles cannot reach admin paths
- **WHEN** a user who holds only `brp_zoeken` requests a path under `/rest/admin/`
- **THEN** ZAC answers with HTTP 403 and shows the base role page

#### Scenario: A user with a read role but without beheerder cannot reach admin paths
- **WHEN** a user who holds `behandelaar` but not `beheerder` opens `/admin`
- **THEN** ZAC answers with HTTP 403 and shows the "U heeft geen toestemming om deze pagina te bekijken." page with a
  home button and no log-out button

#### Scenario: A beheerder can reach admin paths
- **WHEN** a user who holds `beheerder` for one zaaktype requests a path under `/rest/admin/`
- **THEN** ZAC serves the request

#### Scenario: Public paths stay reachable for a user with only non-read roles
- **WHEN** a user who holds only `brp_zoeken` requests `/sign-out` or a file under `/assets/` with HTTP GET
- **THEN** ZAC serves the request, so the error page can load its assets and the log-out button works
