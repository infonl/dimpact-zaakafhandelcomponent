## 1. Update Existing Constants to Lowercase

- [x] 1.0 Update the companion object constants in `BrpClientHeadersFactory` to lowercase (`x-doelbinding`, `x-verwerking`, `x-origin-oin`, `x-gebruiker`) and update any references to these constants in tests and other files

## 2. Remove API Key from ZAC

- [x] 2.1 Remove `ENV_VAR_BRP_API_KEY`, `apiKey` config property, and `getApiKey()` from `BrpConfiguration`
- [x] 2.2 Remove the `X_API_KEY` constant and api key header injection from `BrpClientHeadersFactory`
- [x] 2.3 Remove `BRP_API_KEY` from `charts/zac/templates/secret.yaml`
- [x] 2.4 Remove `BRP_API_KEY` from `.env.example`

## 3. Configuration

- [x] 3.1 Add five `@ConfigProperty` fields to `BrpConfiguration` for header names (`BRP_HEADER_DOELBINDING`, `BRP_HEADER_VERWERKING`, `BRP_HEADER_ORIGIN_OIN`, `BRP_HEADER_GEBRUIKER`, `BRP_HEADER_TOEPASSING`) — no `defaultValue` in annotation; defaults come from Helm
- [x] 3.2 Add a `@ConfigProperty` field for `BRP_TOEPASSING` — no `defaultValue`; Helm provides `"ZAC"` as default
- [x] 3.3 Add getter methods for each new header name property; return `null` when the value is blank (trim then null-if-empty) as the "disabled" signal
- [x] 3.4 Add a getter for the toepassing header value (`BRP_TOEPASSING`)
- [x] 3.5 Replace the single `if (isBrpProtocolleringEnabled())` validation block in `validateConfiguration()` with per-header validation: for each non-blank `BRP_HEADER_*`, validate that the corresponding value env var is present; `BRP_PROTOCOLLERING` remains required when `BRP_ORIGIN_OIN` is set
- [x] 3.6 Add the new env vars to `BrpConfiguration.toString()` (none need redacting)

## 4. Request-Scoped Context Bean

- [x] 4.1 Create `BrpProtocolleringContext` as a `@RequestScoped` CDI bean holding nullable doelbinding, verwerking, and gebruikersnaam strings
- [x] 4.2 Add setter and getter methods to `BrpProtocolleringContext`

## 5. Update BrpClientService

- [x] 5.1 Inject `BrpProtocolleringContext` into `BrpClientService`
- [x] 5.2 Update `queryPersonen()` to set doelbinding, verwerking, and gebruikersnaam on the context before calling `personenApi.personen()`, instead of passing them as parameters
- [x] 5.3 Update `retrievePersoon()` likewise
- [x] 5.4 Remove the protocollering-enabled branch that called the `@HeaderParam` overload — both paths now call the single no-param `personen()` method

## 6. Update BrpClientHeadersFactory

- [x] 6.1 Inject `BrpProtocolleringContext` into `BrpClientHeadersFactory`
- [x] 6.2 Update `update()` to read header names from `BrpConfiguration` getters; skip injection for any header whose configured name is `null` (blank/disabled)
- [x] 6.3 Update `trimHeadersToMaxSize()` to compare against the configured gebruiker header name (from `BrpConfiguration`); skip gebruiker size logic when the name is `null`
- [x] 6.4 Add injection of doelbinding and verwerking from `BrpProtocolleringContext` using their configured names, skipping each if the configured name is `null`
- [x] 6.5 Add injection of the toepassing header using the configured name (`BRP_HEADER_TOEPASSING`) and value (`BRP_TOEPASSING`), skipping if the configured name is `null`

## 7. Simplify PersonenApi

- [x] 7.1 Remove the `personen()` overload with `@HeaderParam` parameters from `PersonenApi`
- [x] 7.2 Remove the imports of `X_DOELBINDING`, `X_VERWERKING`, `X_GEBRUIKER` from `PersonenApi`

## 8. Update Nginx

- [x] 8.1 Remove `x_doelbinding` from `nginx.api_proxy.brp` in `charts/zac/values.yaml`
- [x] 8.2 Remove the `proxy_set_header x-doelbinding` conditional block from the BRP location block in `charts/zac/templates/configmap-nginx.yaml`
- [x] 8.3 Update the BRP location block in `charts/zac/templates/configmap-nginx.yaml` to inject `x-api-key` from `brpApi.apiKey` (conditionally, when non-empty)

## 9. Helm Chart Configuration

- [x] 9.1 Add `brpApi.protocollering.toepassing: "ZAC"` to `charts/zac/values.yaml`
- [x] 9.2 Add `brpApi.protocollering.headers` subsection to `charts/zac/values.yaml` with non-empty defaults for all five header names (`x-doelbinding`, `x-verwerking`, `x-origin-oin`, `x-gebruiker`, `x-toepassing`)
- [x] 9.3 Update the `{{- if .Values.brpApi.protocollering.originOin }}` block in `charts/zac/templates/config.yaml`:
  - Always emit all five `BRP_HEADER_*` env vars (with their Helm values, including empty string)
  - Emit `BRP_DOELBINDING_ZOEKMET` and `BRP_DOELBINDING_RAADPLEEGMET` inside `{{- if .Values.brpApi.protocollering.headers.doelbinding }}` with `required`
  - Emit `BRP_VERWERKINGSREGISTER` inside `{{- if .Values.brpApi.protocollering.headers.verwerking }}` with `required`
  - Emit `BRP_TOEPASSING` inside `{{- if .Values.brpApi.protocollering.headers.toepassing }}` with `required`
  - Keep `BRP_ORIGIN_OIN`, `BRP_PROTOCOLLERING` as before (always required when `originOin` is set)
- [x] 9.4 Add the five new `BRP_HEADER_*` env vars and `BRP_TOEPASSING` to `.env.example` with comments

## 10. Tests

- [x] 10.1 Update `BrpConfigurationTest` to cover the new per-header validation, verify api key config is removed, and verify blank env var returns `null` from getter
- [x] 10.2 Update `BrpClientHeadersFactoryTest` to inject a `BrpProtocolleringContext`; verify api key header is NOT injected
- [x] 10.3 Add a `BrpClientHeadersFactoryTest` scenario verifying custom header names (non-default) are injected correctly
- [x] 10.4 Add a `BrpClientHeadersFactoryTest` scenario verifying the gebruiker 40-char limit applies to the configured `BRP_HEADER_GEBRUIKER` name
- [x] 10.5 Update `BrpClientServiceTest` to verify doelbinding, verwerking, and gebruikersnaam are set on the context instead of passed as method parameters
- [x] 10.6 Add `BrpClientHeadersFactoryTest` scenarios verifying that a header is not sent when its configured name is blank
- [x] 10.7 Add a `BrpClientHeadersFactoryTest` scenario verifying remaining headers are still sent when only one is disabled
- [x] 10.8 Add `BrpClientHeadersFactoryTest` scenarios verifying that the toepassing header is sent with configured value, and is omitted when `BRP_HEADER_TOEPASSING` is blank
- [x] 10.9 Add `BrpConfigurationTest` scenarios verifying per-header validation: startup fails when header is enabled but corresponding value env var is absent
