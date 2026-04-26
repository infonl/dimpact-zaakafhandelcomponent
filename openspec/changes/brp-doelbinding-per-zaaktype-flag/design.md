## Context

BRP protocollering currently requires a `BRP_PROTOCOLLERING` env var ("aanbieder") that must be one of `"iConnect"` or `"2Secure"`. The only place this value is consumed is `ConfigurationRestService.readBrpDoelbindingSetupEnabled()`, which returns `true` iff the aanbieder equals `"iConnect"`. This boolean tells the frontend whether to show per-zaaktype doelbinding configuration in the admin UI.

The indirection (string provider name → inferred boolean) is confusing and couples configuration semantics to a specific vendor name. It also forces operators to supply a string that has no operational meaning beyond flipping one UI toggle.

Additionally, `originOin.oin` currently doubles as the on/off switch for protocollering: when it is non-empty, protocollering is enabled; when empty, it is disabled. This conflation of a header value with an enable flag makes configuration intent ambiguous — an operator cannot disable protocollering while retaining the OIN value in their config, and the Helm `{{- if .Values.brpApi.protocollering.originOin.oin }}` block is non-obvious. An explicit `enabled` flag separates these concerns cleanly.

After this change the configuration stack looks like:

| Layer | Before | After |
|---|---|---|
| Helm `values.yaml` | `brpApi.protocollering.aanbieder: "iConnect"` | `brpApi.protocollering.doelbindingPerZaaktype: false` |
| Helm `values.yaml` | `originOin.oin` non-empty = protocollering on | `brpApi.protocollering.enabled: false` |
| `config.yaml` template | `BRP_PROTOCOLLERING: {{ .Values...aanbieder }}` | `BRP_DOELBINDING_PER_ZAAKTYPE: "{{ .Values...doelbindingPerZaaktype }}"` |
| `config.yaml` template | `{{- if .Values.brpApi.protocollering.originOin.oin }}` gate | `BRP_PROTOCOLLERING_ENABLED: "{{ .Values...enabled }}"` + `{{- if .Values.brpApi.protocollering.enabled }}` gate |
| `BrpConfiguration` | `Optional<String> brpProtocolleringProvider` | `Boolean doelbindingPerZaaktype` (default `false`) |
| `BrpConfiguration` | `isBrpProtocolleringEnabled() = originOIN.isPresent` | `isBrpProtocolleringEnabled() = protocolleringEnabled` (Boolean, default `false`) |
| `ConfigurationRestService` | `PROVIDER_ICONNECT == ...readBrpProtocolleringProvider()` | `brpConfiguration.isDoelbindingPerZaaktype()` |

## Goals / Non-Goals

**Goals:**
- Replace the aanbieder string with an explicit boolean flag
- Simplify `BrpConfiguration.validateConfiguration()` by removing the provider existence/validity checks
- Update all tests (unit + itest) to use the new flag

**Non-Goals:**
- Changing any per-zaaktype doelbinding storage or resolution logic (`ZaaktypeBrpParameters`, `BrpClientService`)
- Changing the REST endpoint path (`/configuratie/brp/doelbinding-setup-enabled`)
- Any frontend changes — the API contract is identical (boolean response)

## Decisions

### 1. Plain `Boolean` with `defaultValue = "false"` instead of `Optional<Boolean>`

The env var `BRP_DOELBINDING_PER_ZAAKTYPE` is always emitted by the Helm `config.yaml` template (not conditional on `originOin` being set), so the value is always present at runtime. Using a plain `Boolean` with `@ConfigProperty(name = "BRP_DOELBINDING_PER_ZAAKTYPE", defaultValue = "false")` is simpler than `Optional<Boolean>` and consistent with how other always-present boolean flags are handled in Quarkus.

The `defaultValue = "false"` also means local/test environments that do not set the variable at all default to the safe off state.

### 2. Emit `BRP_DOELBINDING_PER_ZAAKTYPE` unconditionally in `config.yaml`

The current `BRP_PROTOCOLLERING` block is inside the `{{- if .Values.brpApi.protocollering.originOin.oin }}` conditional. The new `BRP_DOELBINDING_PER_ZAAKTYPE` should be emitted **unconditionally** (outside the `if` block). Reason: the flag has a safe default (`false`) and is meaningful even when protocollering is disabled, for example during local development.

### 3. Remove `readBrpProtocolleringProvider()` and all provider constants

`BRP_PROTOCOLLERING_PROVIDER_ICONNECT`, `BRP_PROTOCOLLERING_PROVIDER_2SECURE`, `SUPPORTED_PROTOCOLLERING_PROVIDERS`, and `readBrpProtocolleringProvider()` are all dead code after this change. Delete them entirely rather than deprecating — nothing outside the two affected files uses them at runtime.

### 4. Rename the getter to `isDoelbindingPerZaaktype()`

Boolean getter follows the `is` prefix convention already used by `isBrpProtocolleringEnabled()`. The method is used directly by `ConfigurationRestService` without going through `ConfigurationService`, consistent with the existing `readBrpConfiguration()` delegation pattern.

### 5. Explicit `BRP_PROTOCOLLERING_ENABLED` boolean replaces `originOIN.isPresent` as the enable gate

`BRP_PROTOCOLLERING_ENABLED` is emitted unconditionally by the Helm template (same pattern as `BRP_DOELBINDING_PER_ZAAKTYPE`) with `defaultValue = "false"`. The `originOIN: Optional<String>` constructor parameter is retained unchanged — it still provides the value injected into the `x-origin-oin` header — but `isBrpProtocolleringEnabled()` now returns `protocolleringEnabled` instead of `originOIN.isPresent`.

The Helm `config.yaml` gate changes from `{{- if .Values.brpApi.protocollering.originOin.oin }}` to `{{- if .Values.brpApi.protocollering.enabled }}`, so values inside that block (including `BRP_ORIGIN_OIN`) are still only emitted when protocollering is enabled.

Tests that used `createBrpConfiguration(originOin = Optional.empty())` solely to disable protocollering must be updated to use `protocolleringEnabled = false` instead; the `originOin` default in the fixture stays as `Optional.of("fakeOriginOin")` so existing header-injection tests continue to verify the correct header value.

## Risks / Trade-offs

**Existing deployments that set `BRP_PROTOCOLLERING=iConnect` will break at startup** → Mitigation: Quarkus ignores unknown env vars by default (no `@ConfigProperty` bound to them), so the old variable simply goes unread. Operators must add `BRP_DOELBINDING_PER_ZAAKTYPE=true` to their environment if they were using iConnect; the application will otherwise start with the flag as `false` (doelbinding setup UI hidden). Document in the Helm values comment and in `.env.example`.

**Existing deployments that relied on `originOin.oin` to enable protocollering will have it disabled after upgrade** → Mitigation: operators must set `brpApi.protocollering.enabled: true` in their values override. The application starts safely (no headers sent) rather than failing, so this is a silent behavioural change rather than a crash. Document prominently in the migration plan and in the BRP configuration guide.

**Integration tests hard-code `BRP_PROTOCOLLERING_ICONNECT`** → Replace the constant and env-var entry in `ZacItestProjectConfig.kt` and `ItestConfiguration.kt`; update the itest `ConfigurationRestServiceTest` to stop mocking `readBrpProtocolleringProvider()`.

## Migration Plan

1. Deploy new Helm chart values (sets `BRP_DOELBINDING_PER_ZAAKTYPE` and `BRP_PROTOCOLLERING_ENABLED`) and simultaneously remove old `BRP_PROTOCOLLERING` from the Kubernetes secret/configmap.
2. Operators who previously relied on iConnect-specific UI must set `brpApi.protocollering.doelbindingPerZaaktype: true` in their `values.yaml` override before upgrading.
3. Operators who had BRP protocollering enabled (non-empty `originOin.oin`) must also set `brpApi.protocollering.enabled: true` — otherwise protocollering will silently stop sending headers after the upgrade.
4. Rollback: revert Helm chart version — old env vars are ignored by the new code, new env vars are ignored by the old code, so there is no cross-version state conflict.
