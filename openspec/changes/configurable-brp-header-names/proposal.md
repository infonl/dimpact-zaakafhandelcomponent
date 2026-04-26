## Why

BRP protocollering header names (`x-doelbinding`, `x-verwerking`, `x-origin-oin`, `x-gebruiker`) are hardcoded in `BrpClientHeadersFactory`. Different BRP API providers (iConnect, 2Secure) or municipality-specific deployments may require different header name conventions, making it impossible to adapt without code changes.

Additionally, the nginx BRP proxy block currently injects a hardcoded `x-doelbinding: "test"` header that silently overrides the value ZAC sets, and the `x-api-key` authentication header is injected by ZAC application code rather than the nginx proxy layer where authentication headers belong.

## What Changes

- Add a new `x-toepassing` header with a Helm-defaulted value of `ZAC`, injected on every protocollering-enabled BRP request
- Add a `brpApi.protocollering.headers` subsection to Helm `values.yaml` with a non-empty default name for each BRP protocollering header — these defaults flow into the application as environment variables; the application does not hardcode them
- Setting a header name to an empty string in `values.yaml` disables that header entirely and makes its corresponding value env var optional
- When a header name is non-empty, the corresponding value environment variable (`BRP_DOELBINDING_ZOEKMET`, `BRP_VERWERKINGSREGISTER`, etc.) becomes mandatory and is validated at application startup
- The Helm template emits value env vars only when their corresponding header name is non-empty; this replaces the single `if originOin → require everything` validation with per-header validation
- Remove `x-api-key` header injection from ZAC (`BrpClientHeadersFactory`) — authentication headers are the nginx proxy's responsibility
- Remove `BRP_API_KEY` environment variable and related code from ZAC; remove from Kubernetes Secret
- Update nginx BRP proxy block to inject `x-api-key` from `brpApi.apiKey` (using the existing `apikey_header_name`/`apikey_value` mechanism already used by BAG and KVK)
- Remove `x_doelbinding` injection from the nginx BRP proxy block — ZAC now owns this header
- Consolidate header injection: move doelbinding/verwerking/gebruiker into factory via a request-scoped context bean (removing `@HeaderParam` from `PersonenApi`)

## Capabilities

### New Capabilities
- `configurable-brp-header-names`: Add `x-toepassing` header; make all ZAC-managed BRP protocollering header names and their enable/disable state configurable via Helm values (empty = disabled, non-empty = enabled with mandatory value); move api key injection to nginx

### Modified Capabilities
<!-- No existing spec-level requirement changes -->

## Impact

- `src/main/kotlin/nl/info/client/brp/util/BrpClientHeadersFactory.kt` — remove `X_API_KEY` constant and api key injection; use configurable names for remaining headers; read header names from `BrpConfiguration` (no code-level defaults)
- `src/main/kotlin/nl/info/zac/configuration/BrpConfiguration.kt` — remove `ENV_VAR_BRP_API_KEY` and `getApiKey()`; add new config properties for header names (no `defaultValue` in annotation — defaults come from Helm); change per-header validation
- `src/main/kotlin/nl/info/client/brp/PersonenApi.kt` — remove `@HeaderParam` overload; consolidate to single `personen()` method
- `src/main/kotlin/nl/info/client/brp/BrpClientService.kt` — populate request-scoped context instead of passing header params
- `charts/zac/templates/secret.yaml` — remove `BRP_API_KEY` entry
- `charts/zac/templates/config.yaml` — replace single `if originOin` block with per-header conditional blocks for value env vars; always emit `BRP_HEADER_*` env vars when protocollering is enabled; add `BRP_TOEPASSING`
- `charts/zac/templates/configmap-nginx.yaml` — remove `x_doelbinding` injection; wire `x-api-key` from `brpApi.apiKey`
- `charts/zac/values.yaml` — add `brpApi.protocollering.headers` subsection with non-empty defaults; add `brpApi.protocollering.toepassing`; remove `nginx.api_proxy.brp.x_doelbinding`
- `.env.example` — document new environment variables; remove `BRP_API_KEY`
- Tests for `BrpClientHeadersFactory`, `BrpConfiguration`, and `BrpClientService`
