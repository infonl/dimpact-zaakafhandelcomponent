## ADDED Requirements

### Requirement: Doelbinding-per-zaaktype flag replaces aanbieder configuration
The system SHALL expose a boolean configuration flag `BRP_DOELBINDING_PER_ZAAKTYPE` that indicates whether doelbinding values must be supplied per zaaktype in the admin UI. This flag SHALL default to `false` when the environment variable is absent.

#### Scenario: Flag is false (default)
- **WHEN** `BRP_DOELBINDING_PER_ZAAKTYPE` is not set or set to `"false"`
- **THEN** `BrpConfiguration.isDoelbindingPerZaaktype()` returns `false`

#### Scenario: Flag is true
- **WHEN** `BRP_DOELBINDING_PER_ZAAKTYPE` is set to `"true"`
- **THEN** `BrpConfiguration.isDoelbindingPerZaaktype()` returns `true`

### Requirement: Doelbinding setup endpoint reflects the explicit flag
The `GET /configuratie/brp/doelbinding-setup-enabled` endpoint SHALL return `true` if and only if `BRP_DOELBINDING_PER_ZAAKTYPE` is set to `true`, regardless of any previous aanbieder configuration.

#### Scenario: Doelbinding setup enabled
- **WHEN** `BRP_DOELBINDING_PER_ZAAKTYPE=true` is configured
- **THEN** `GET /configuratie/brp/doelbinding-setup-enabled` returns `true`

#### Scenario: Doelbinding setup disabled by default
- **WHEN** `BRP_DOELBINDING_PER_ZAAKTYPE` is absent or `false`
- **THEN** `GET /configuratie/brp/doelbinding-setup-enabled` returns `false`

## REMOVED Requirements

### Requirement: Aanbieder (protocollering provider) configuration
**Reason**: The `BRP_PROTOCOLLERING` env var existed solely to infer whether doelbinding-per-zaaktype was required. This purpose is now served by the explicit `BRP_DOELBINDING_PER_ZAAKTYPE` flag, making the aanbieder concept redundant.
**Migration**: Operators who previously set `BRP_PROTOCOLLERING=iConnect` to enable per-zaaktype doelbinding UI MUST set `BRP_DOELBINDING_PER_ZAAKTYPE=true` in their environment (or `brpApi.protocollering.doelbindingPerZaaktype: true` in Helm values). The old variable is no longer read and will be silently ignored.
