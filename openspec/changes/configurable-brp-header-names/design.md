## Context

BRP protocollering uses four hardcoded HTTP header names defined as constants in `BrpClientHeadersFactory`:
- `x-doelbinding`, `x-verwerking` — passed via `@HeaderParam` on `PersonenApi.personen()`
- `x-origin-oin`, `x-gebruiker` — injected by the factory

A fifth header `x-api-key` is also injected by the factory using `BRP_API_KEY` from a Kubernetes Secret. The nginx BRP proxy block currently injects `x-doelbinding: "test"` (silently overriding ZAC's value) and has `apikey_header_name`/`apikey_value` wired but empty for BRP (the same mechanism already used for BAG and KVK api key injection).

The `@HeaderParam` annotation on `PersonenApi` requires compile-time string constants, so header names cannot be made runtime-configurable while `@HeaderParam` is used to pass doelbinding/verwerking values into the factory.

## Goals / Non-Goals

**Goals:**
- All ZAC-managed BRP protocollering header names configurable via Helm values (Helm is the single source of truth for defaults, not application code)
- Empty header name in Helm = header disabled; non-empty = header enabled with mandatory corresponding value
- `x-api-key` header name configurable via `BRP_API_KEY_HEADER`; injection stays in ZAC following the same pattern as all other configurable headers
- `x-doelbinding` injection removed from nginx; ZAC owns it
- Single unified validation: per-header at startup

**Non-Goals:**
- Changing header *values* other than adding `x-toepassing`
- Supporting multiple simultaneous header name sets
- Making WireMock test mappings dynamic

## Decisions

### Decision 1: Move all header injection into `BrpClientHeadersFactory` via a request-scoped context bean

**Problem:** `PersonenApi.personen()` uses `@HeaderParam(X_DOELBINDING)` to pass doelbinding and verwerking values to the factory. Because `@HeaderParam` requires compile-time constants, the header name can never be runtime-configurable while this approach is used.

**Solution:** Introduce a `@RequestScoped` CDI bean `BrpProtocolleringContext` that holds the per-request protocollering values (doelbinding, verwerking, gebruikersnaam). `BrpClientService` populates this context before calling `PersonenApi`. `BrpClientHeadersFactory` reads from it and injects all headers using configurable names from `BrpConfiguration`.

`PersonenApi` is simplified to a single `personen()` method — no `@HeaderParam` parameters needed.

**Alternatives considered:**
- **Rename in factory:** Works without interface changes but uses the old hardcoded name as an internal transport sentinel, creating confusing indirection.
- **Only configure factory-injected headers:** Partial solution; doelbinding and verwerking names remain hardcoded.

### Decision 2: API key header name is configurable; injection stays in ZAC

**Problem:** `BrpClientHeadersFactory` injects `x-api-key` from `BRP_API_KEY` (a Kubernetes Secret env var) with a hardcoded header name. Different BRP providers may use different header names for API key authentication, so the header name must be runtime-configurable like all other protocollering headers.

**Solution:** Keep `BRP_API_KEY` as a Kubernetes Secret env var. Add `BRP_API_KEY_HEADER` as a regular env var following the same pattern as `BRP_DOELBINDING_HEADER` etc. `BrpConfiguration` exposes `getApiKey(): BrpConfigurationValue`; `BrpClientService` populates the header from this value. When `BRP_API_KEY_HEADER` is empty, the header is disabled; when non-empty, `BRP_API_KEY` is required.

The nginx API key injection via `apikey_header_name`/`apikey_value` is NOT used for BRP — ZAC owns the API key header.

**Trade-off:** API key remains in a Kubernetes Secret (good RBAC isolation). Header name is in a ConfigMap (acceptable — it is not sensitive).

### Decision 3: Helm values are the source of truth for header name defaults

Header name defaults do NOT live in application code. The `@ConfigProperty` annotations for `BRP_HEADER_*` have no `defaultValue`; the environment variable is always present because the Helm chart always emits it inside the `{{- if .Values.brpApi.protocollering.originOin }}` block.

`values.yaml` provides non-empty defaults for all header names:

```yaml
brpApi:
  protocollering:
    toepassing: "ZAC"
    headers:
      doelbinding: "x-doelbinding"
      verwerking:  "x-verwerking"
      originOin:   "x-origin-oin"
      gebruiker:   "x-gebruiker"
      toepassing:  "x-toepassing"
```

An operator disables a header by setting its value to `""`. An operator renames a header by changing the string.

**Alternative considered:** Hardcode defaults in `@ConfigProperty(defaultValue = "x-doelbinding")`. Rejected — defaults would be split across application code and Helm, making it unclear which is authoritative. Helm is already where operators configure deployments.

### Decision 4: Per-header validation replaces the single `if originOin → require everything` gate

**Current model:** if `BRP_ORIGIN_OIN` is set → all value env vars are required.

**New model:** for each `BRP_HEADER_*` that is non-empty, the corresponding value env var is required. The Helm template enforces this at render time using per-header `{{- if }}` blocks; the application enforces it at startup in `BrpConfiguration.validateConfiguration()`.

Mapping of header name → required value env vars:

| Header name env var | Non-empty → required values |
|---|---|
| `BRP_HEADER_DOELBINDING` | `BRP_DOELBINDING_ZOEKMET`, `BRP_DOELBINDING_RAADPLEEGMET` |
| `BRP_HEADER_VERWERKING` | `BRP_VERWERKINGSREGISTER` |
| `BRP_HEADER_ORIGIN_OIN` | `BRP_ORIGIN_OIN` |
| `BRP_HEADER_GEBRUIKER` | *(none — uses logged-in user)* |
| `BRP_HEADER_TOEPASSING` | `BRP_TOEPASSING` |

`BRP_PROTOCOLLERING` (aanbieder) remains required whenever protocollering is enabled (`BRP_ORIGIN_OIN` value present).

The Helm template structure inside `{{- if .Values.brpApi.protocollering.originOin }}`:
```yaml
# Header names always emitted (empty string = disabled)
BRP_HEADER_DOELBINDING: {{ .Values.brpApi.protocollering.headers.doelbinding | quote }}
BRP_HEADER_VERWERKING:  {{ .Values.brpApi.protocollering.headers.verwerking  | quote }}
BRP_HEADER_ORIGIN_OIN:  {{ .Values.brpApi.protocollering.headers.originOin   | quote }}
BRP_HEADER_GEBRUIKER:   {{ .Values.brpApi.protocollering.headers.gebruiker   | quote }}
BRP_HEADER_TOEPASSING:  {{ .Values.brpApi.protocollering.headers.toepassing  | quote }}

# Value env vars emitted only when the corresponding header name is non-empty
{{- if .Values.brpApi.protocollering.headers.doelbinding }}
BRP_DOELBINDING_ZOEKMET:     {{ required "..." .Values.brpApi.protocollering.doelbinding.zoekmet     | quote }}
BRP_DOELBINDING_RAADPLEEGMET: {{ required "..." .Values.brpApi.protocollering.doelbinding.raadpleegmet | quote }}
{{- end }}
{{- if .Values.brpApi.protocollering.headers.verwerking }}
BRP_VERWERKINGSREGISTER: {{ required "..." .Values.brpApi.protocollering.verwerkingsregister | quote }}
{{- end }}
{{- if .Values.brpApi.protocollering.headers.toepassing }}
BRP_TOEPASSING: {{ required "..." .Values.brpApi.protocollering.toepassing | quote }}
{{- end }}
```

### Decision 5: Lowercase existing header name constants

The companion object constants in `BrpClientHeadersFactory` (`X_DOELBINDING`, `X_VERWERKING`, etc.) currently use uppercase values. These are changed to lowercase (`x-doelbinding`, etc.) to align with HTTP/2 conventions and the Helm defaults.

The `trimHeadersToMaxSize()` gebruiker comparison must use the *configured* name (from `BrpConfiguration`), not the constant, since the name is now runtime-configurable.

## Risks / Trade-offs

- **Thread-safety of `BrpProtocolleringContext`:** `@RequestScoped` is safe per-request in Quarkus; no shared mutable state across threads.
- **Test complexity:** Tests for `BrpClientService` that mock `PersonenApi` must be updated to verify context population rather than method parameter values. Tests for `BrpClientHeadersFactory` gain a dependency on the context bean.
- **API key in ConfigMap:** Moving `brpApi.apiKey` from Secret to nginx ConfigMap aligns with the existing BAG/KVK pattern but reduces RBAC isolation for this credential.
- **Startup validation changes:** The per-header validation model means an operator can disable doelbinding but leave verwerkingsregister set — the app won't validate it (because its header is disabled). This is intentional; operators configure what they need.

## Migration Plan

1. Update `BrpClientHeadersFactory` constants to lowercase; update referencing tests
2. Remove `x-api-key` injection from `BrpClientHeadersFactory` and `BRP_API_KEY` from `BrpConfiguration`
3. Add new header name config properties to `BrpConfiguration` (no `defaultValue`); update validation to per-header
4. Add `BrpProtocolleringContext` request-scoped bean
5. Update `BrpClientService` to populate context
6. Simplify `PersonenApi` to single method
7. Update nginx: remove `x_doelbinding`; wire `x-api-key` from `brpApi.apiKey`
8. Update `secret.yaml`: remove `BRP_API_KEY`
9. Update `config.yaml`: replace single `if originOin` block with per-header structure
10. Update `values.yaml`: add `brpApi.protocollering.headers` and `toepassing`; remove `x_doelbinding` from nginx block
11. Update `.env.example`; update unit tests

**Rollback:** Revert Helm chart changes. Helm values with empty header names disable all headers, restoring a known-safe state.

## Open Questions

- Should `BRP_HEADER_GEBRUIKER` also control the `SYSTEM_USER` constant value (`"Systeem"`)? Currently out of scope but related.
