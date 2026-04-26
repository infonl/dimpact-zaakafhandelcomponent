## Why

The `BRP_PROTOCOLLERING` env var ("aanbieder") exists solely to determine whether doelbinding values must be configured per zaaktype in the admin UI, yet it does so indirectly — by checking if the value equals `"iConnect"`. Removing this inference in favour of an explicit `doelbindingPerZaaktype` boolean makes the intent clear, eliminates an unnecessary concept (provider identity) from configuration, and simplifies validation logic.

## What Changes

- Remove the `BRP_PROTOCOLLERING` environment variable, its validation, and the `BRP_PROTOCOLLERING_PROVIDER_ICONNECT` / `BRP_PROTOCOLLERING_PROVIDER_2SECURE` constants from `BrpConfiguration`
- Add a new boolean env var `BRP_DOELBINDING_PER_ZAAKTYPE` (default `false`) that controls whether doelbinding values must be supplied per zaaktype
- The `ConfigurationRestService.readBrpDoelbindingSetupEnabled()` endpoint reads the new flag instead of comparing the aanbieder string to `"iConnect"`
- Remove `brpApi.protocollering.aanbieder` from `charts/zac/values.yaml`; add `brpApi.protocollering.doelbindingPerZaaktype: false`
- Replace the `BRP_PROTOCOLLERING` env var block in `charts/zac/templates/config.yaml` with a `BRP_DOELBINDING_PER_ZAAKTYPE` env var
- Update `.env.example` accordingly

## Capabilities

### New Capabilities
- `brp-doelbinding-per-zaaktype-flag`: Replace the aanbieder string with an explicit boolean flag that controls whether the admin UI must supply doelbinding values per zaaktype

### Modified Capabilities
<!-- No existing spec-level requirement changes -->

## Impact

- `src/main/kotlin/nl/info/zac/configuration/BrpConfiguration.kt` — remove `ENV_VAR_BRP_PROTOCOLLERING_PROVIDER`, `BRP_PROTOCOLLERING_PROVIDER_ICONNECT`, `BRP_PROTOCOLLERING_PROVIDER_2SECURE` constants and `readBrpProtocolleringProvider()` / related validation; add `BRP_DOELBINDING_PER_ZAAKTYPE` boolean `@ConfigProperty` and getter
- `src/main/kotlin/nl/info/zac/app/configuration/ConfigurationRestService.kt` — replace aanbieder check with call to new `isBrpDoelbindingPerZaaktype()` getter on `BrpConfiguration`
- `charts/zac/values.yaml` — remove `brpApi.protocollering.aanbieder`; add `brpApi.protocollering.doelbindingPerZaaktype: false`
- `charts/zac/templates/config.yaml` — replace `BRP_PROTOCOLLERING` env var with `BRP_DOELBINDING_PER_ZAAKTYPE`
- `.env.example` — remove `BRP_PROTOCOLLERING`; add `BRP_DOELBINDING_PER_ZAAKTYPE`
- Tests for `BrpConfiguration` and `ConfigurationRestService`
