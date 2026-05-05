## 1. Update Existing Constants to Lowercase

- [x] 1.0 Update the companion object constants in `BrpClientHeadersFactory` to lowercase (`x-doelbinding`, `x-verwerking`, `x-origin-oin`, `x-gebruiker`) and update any references to these constants in tests and other files

## 2. Remove API Key from ZAC

- [x] 2.1 Remove `ENV_VAR_BRP_API_KEY`, `apiKey` config property, and `getApiKey()` from `BrpConfiguration`
- [x] 2.2 Remove the `X_API_KEY` constant and api key header injection from `BrpClientHeadersFactory`
- [x] 2.3 Remove `BRP_API_KEY` from `charts/zac/templates/secret.yaml`
- [x] 2.4 Remove `BRP_API_KEY` from `.env.example`

## 3. Configuration Abstraction

- [x] 3.1 Create `BrpConfigurationValue` interface in `BrpConfigurationProvider.kt` with `isAvailable()`, `getHeaderName()`, and `getValue()` methods — encapsulates a single header name+value pair and its enabled state
- [x] 3.2 Create `BrpConfigurationProvider` interface with `isBrpProtocolleringEnabled()`, `isDoelbindingPerZaaktype()`, `getHeaderUser()`, `getOriginOIN()`, `getDoelbindingZoekMetDefault()`, `getDoelbindingRaadpleegMetDefault()`, `getVerwerkingRegisterDefault()`, `getToepassing()`, `buildDoelbinding()`, `buildVerwerkingRegister()`, and `buildUser()` methods
- [x] 3.3 Create `BrpConfigurationValueImpl` in `BrpConfiguration.kt` implementing `BrpConfigurationValue` — takes env variable name, max size, optional header name, value supplier, and optional default value supplier; `isAvailable()` returns true when header name is present and non-blank; `getValue()` returns `null` when not available or supplier returns null, truncated to max size
- [x] 3.4 Make `BrpConfiguration` implement `BrpConfigurationProvider`
- [x] 3.5 Update `BrpClientService` to inject `BrpConfigurationProvider` instead of `BrpConfiguration`
- [x] 3.6 Update `ConfigurationService` to inject `BrpConfigurationProvider` instead of `BrpConfiguration`

## 4. Configuration

- [x] 4.1 Add `@ConfigProperty` fields to `BrpConfiguration` for header names (`BRP_DOELBINDING_HEADER`, `BRP_VERWERKING_HEADER`, `BRP_ORIGIN_OIN_HEADER`, `BRP_GEBRUIKER_HEADER`, `BRP_TOEPASSING_HEADER`) — no `defaultValue` in annotation; defaults come from Helm
- [x] 4.2 Add `@ConfigProperty` fields for header values (`BRP_DOELBINDING_ZOEKMET`, `BRP_DOELBINDING_RAADPLEEGMET`, `BRP_VERWERKINGSREGISTER`, `BRP_TOEPASSING`, `BRP_ORIGIN_OIN`) — no `defaultValue`; Helm provides defaults
- [x] 4.3 Add `@ConfigProperty` field for `BRP_SYSTEM_USER` — optional fallback user value when no logged-in user is available
- [x] 4.4 Implement `BrpConfigurationProvider` methods in `BrpConfiguration` using `BrpConfigurationValueImpl` builders; `buildUser()` uses `BRP_SYSTEM_USER` as default value supplier
- [x] 4.5 Replace the single `if (isBrpProtocolleringEnabled())` validation block in `validateConfiguration()` with per-header validation: for each non-blank header name env var, validate that the corresponding value env var is present
- [x] 4.6 Add the new env vars to `BrpConfiguration.toString()` (none need redacting)

## 5. Request-Scoped Context Bean

- [x] 5.1 Create `BrpProtocolleringContext` as a `@RequestScoped` CDI bean holding a `headers: MutableMap<String, String>` — callers write header name→value entries directly into the map; `BrpClientHeadersFactory` reads from it
- [x] 5.2 Add getter/setter convenience methods to `BrpProtocolleringContext` if needed for readability

## 6. Update BrpClientService

- [x] 6.1 Update `queryPersonen()` to populate `brpProtocolleringContext.headers` before calling `personenApi.personen()`: resolve doelbinding, verwerking, user, origin OIN, and toepassing via their respective `BrpConfigurationProvider` builders; skip each if the configured header name is not available; log a warning when a header is enabled but its value cannot be resolved
- [x] 6.2 Update `retrievePersoon()` likewise using the context map
- [x] 6.3 Make `userName` parameter non-nullable (`String` instead of `String? = null`) in both `queryPersonen()` and `retrievePersoon()` — callers must supply the logged-in user's ID; `BRP_SYSTEM_USER` serves as fallback within `buildUser()`
- [x] 6.4 Update `KlantRestService` to pass `loggedInUserInstance.get().id` to `queryPersonen()` and `retrievePersoon()`
- [x] 6.5 Update `DocumentCreationDataConverter` to pass the logged-in user's ID down through `createAanvragerData()`, `convertToAanvragerData()`, and `createAanvragerDataNatuurlijkPersoon()` to `brpClientService.retrievePersoon()`

## 7. Update BrpClientHeadersFactory

- [x] 7.1 Inject `BrpProtocolleringContext` into `BrpClientHeadersFactory`
- [x] 7.2 Update `update()` to iterate `brpProtocolleringContext.headers` and set each header on the outgoing request; no longer reads named fields
- [x] 7.3 Update `trimHeadersToMaxSize()` to compare against the configured gebruiker header name from `BrpConfigurationProvider`; skip gebruiker size logic when the header name is not available

## 8. Simplify PersonenApi

- [x] 8.1 Remove the `personen()` overload with `@HeaderParam` parameters from `PersonenApi`
- [x] 8.2 Remove the imports of `X_DOELBINDING`, `X_VERWERKING`, `X_GEBRUIKER` from `PersonenApi`

## 9. Update Nginx

- [x] 9.1 Remove `x_doelbinding` from `nginx.api_proxy.brp` in `charts/zac/values.yaml`
- [x] 9.2 Remove the `proxy_set_header x-doelbinding` conditional block from the BRP location block in `charts/zac/templates/configmap-nginx.yaml`
- [x] 9.3 Update the BRP location block in `charts/zac/templates/configmap-nginx.yaml` to inject `x-api-key` from `brpApi.apiKey` (conditionally, when non-empty)

## 10. Helm Chart Configuration

- [x] 10.1 Add `brpApi.protocollering.toepassing: "ZAC"` to `charts/zac/values.yaml`
- [x] 10.2 Add `brpApi.protocollering.headers` subsection to `charts/zac/values.yaml` with non-empty defaults for all five header names (`x-doelbinding`, `x-verwerking`, `x-origin-oin`, `x-gebruiker`, `x-toepassing`)
- [x] 10.3 Update the `{{- if .Values.brpApi.protocollering.originOin }}` block in `charts/zac/templates/config.yaml`:
  - Always emit all five `BRP_*_HEADER` env vars (with their Helm values, including empty string)
  - Emit `BRP_DOELBINDING_ZOEKMET` and `BRP_DOELBINDING_RAADPLEEGMET` inside `{{- if .Values.brpApi.protocollering.headers.doelbinding }}` with `required`
  - Emit `BRP_VERWERKINGSREGISTER` inside `{{- if .Values.brpApi.protocollering.headers.verwerking }}` with `required`
  - Emit `BRP_TOEPASSING` inside `{{- if .Values.brpApi.protocollering.headers.toepassing }}` with `required`
  - Keep `BRP_ORIGIN_OIN`, `BRP_PROTOCOLLERING_ENABLED` as before (always required when `originOin` is set)
- [x] 10.4 Add `BRP_SYSTEM_USER` to `charts/zac/templates/config.yaml` (optional, emitted only when `brpApi.protocollering.systemUser` is set)
- [x] 10.5 Add `brpApi.protocollering.systemUser` (empty string default) to `charts/zac/values.yaml`
- [x] 10.6 Add the new `BRP_*_HEADER` env vars and `BRP_TOEPASSING` to `.env.example` with comments
- [x] 10.7 Add `BRP_SYSTEM_USER` to `.env.example` with a comment

## 11. Tests

- [x] 11.1 Update `BrpConfigurationTest` to cover the new per-header validation, verify api key config is removed, and verify blank env var returns `null` from `BrpConfigurationValue.getValue()`
- [x] 11.2 Update `BrpClientHeadersFactoryTest` to inject a `BrpProtocolleringContext` via its `headers` map; verify api key header is NOT injected
- [x] 11.3 Add a `BrpClientHeadersFactoryTest` scenario verifying custom header names (non-default) are injected correctly
- [x] 11.4 Add a `BrpClientHeadersFactoryTest` scenario verifying the factory passes through values as-is (truncation is `BrpConfigurationValueImpl`'s responsibility, not the factory's)
- [x] 11.5 Update `BrpClientServiceTest` to verify headers are set on `brpProtocolleringContext.headers` instead of passed as method parameters
- [x] 11.6 Add `BrpClientHeadersFactoryTest` scenarios verifying that a header is not sent when its configured name is blank
- [x] 11.7 Add a `BrpClientHeadersFactoryTest` scenario verifying remaining headers are still sent when only one is disabled
- [x] 11.8 Add `BrpClientHeadersFactoryTest` scenarios verifying that the toepassing header is sent with configured value, and is omitted when `BRP_TOEPASSING_HEADER` is blank
- [x] 11.9 Add `BrpConfigurationTest` scenarios verifying per-header validation: startup fails when header is enabled but corresponding value env var is absent
- [x] 11.10 Add `BrpClientServiceTest` scenario verifying `BRP_SYSTEM_USER` is used as fallback when no logged-in user value is supplied

## 12. Restore BRP API Key with Configurable Header Name

- [x] 12.1 Add `BRP_API_KEY` (optional `String`) and `BRP_API_KEY_HEADER` (optional `String`) `@ConfigProperty` fields back to `BrpConfiguration`
- [x] 12.2 Add `getApiKey(): BrpConfigurationValue` to `BrpConfigurationProvider` interface and implement it in `BrpConfiguration` using `BrpConfigurationValueImpl`
- [x] 12.3 Add per-header validation in `validateConfiguration()`: when `BRP_API_KEY_HEADER` is non-empty, require `BRP_API_KEY`
- [x] 12.4 Update `BrpClientService.populateUserOriginAndToepassing()` (or equivalent) to set the API key header from `brpConfigurationProvider.getApiKey()` into `brpProtocolleringContext.headers`
- [x] 12.5 Re-add the optional `BRP_API_KEY` entry to `charts/zac/templates/secret.yaml` (emitted only when `brpApi.apiKey` is set)
- [x] 12.6 Add `BRP_API_KEY_HEADER` env var to `charts/zac/templates/config.yaml` inside the protocollering block (always emitted, empty string = disabled)
- [x] 12.7 Add `brpApi.apiKey` and `brpApi.protocollering.apiKey.header` to `charts/zac/values.yaml`
- [x] 12.8 Add `BRP_API_KEY` and `BRP_API_KEY_HEADER` to `.env.example` with comments
- [x] 12.9 Add `BRP_API_KEY` and `BRP_API_KEY_HEADER` to `docker-compose.yaml`
- [x] 12.10 Correct task 9.3: remove nginx API key injection (task 9.3 was never implemented; nginx does not inject the BRP API key — ZAC does)
- [x] 12.11 Include `BRP_API_KEY_HEADER` in `BrpConfiguration.toString()`; redact `BRP_API_KEY` value
- [x] 12.12 Update `BrpConfigurationTest` to cover API key header validation (startup fails when header enabled but `BRP_API_KEY` absent)
- [x] 12.13 Update `BrpClientServiceTest` to verify the API key header is set on `brpProtocolleringContext.headers`

## 13. Fix Integration Test WireMock Expected Counts

- [x] 13.1 In `KlantRestServiceTest.kt` line 286 change `{ "count": 1 }` to `{ "count": 2 }` — `getTemporaryPersonId()` (spec initialisation) issues one default-header BRP request before any `When` blocks run, so the default-header count after the no-zaaktype GET `When` block is 2, not 1
- [x] 13.2 In `KlantRestServiceTest.kt` line 401 change `{ "count": 2 }` to `{ "count": 3 }` — by that point three default-header BRP requests have been made: spec init + no-zaaktype GET + no-zaaktype PUT

## 14. Remove Dead `BRP_PROTOCOLLERING` Env Var

- [x] 14.1 Remove `- BRP_PROTOCOLLERING=${BRP_PROTOCOLLERING:-iConnect}` from `docker-compose.yaml` — `BrpConfiguration` no longer reads this env var; the provider/aanbieder concept was replaced by `BRP_PROTOCOLLERING_ENABLED` and per-header configuration
