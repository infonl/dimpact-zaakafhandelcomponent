## 1. Update BrpConfiguration

- [x] 1.1 Remove `ENV_VAR_BRP_PROTOCOLLERING_PROVIDER`, `BRP_PROTOCOLLERING_PROVIDER_ICONNECT`, `BRP_PROTOCOLLERING_PROVIDER_2SECURE`, and `SUPPORTED_PROTOCOLLERING_PROVIDERS` constants from the companion object
- [x] 1.2 Remove the `brpProtocolleringProvider: Optional<String>` constructor parameter and its `@ConfigProperty(name = ENV_VAR_BRP_PROTOCOLLERING_PROVIDER)` annotation
- [x] 1.3 Add `@ConfigProperty(name = "BRP_DOELBINDING_PER_ZAAKTYPE", defaultValue = "false") private val doelbindingPerZaaktype: Boolean` constructor parameter
- [x] 1.4 Remove the two `throwIf` blocks that validate `brpProtocolleringProvider` presence and validity from `validateConfiguration()`
- [x] 1.5 Add `fun isDoelbindingPerZaaktype(): Boolean = doelbindingPerZaaktype` getter method
- [x] 1.6 Remove `readBrpProtocolleringProvider()` method entirely
- [x] 1.7 Remove `ENV_VAR_BRP_PROTOCOLLERING_PROVIDER` line from `toString()`; add `"BRP_DOELBINDING_PER_ZAAKTYPE: '$doelbindingPerZaaktype'"` line

## 2. Update ConfigurationRestService

- [x] 2.1 Remove the import of `BRP_PROTOCOLLERING_PROVIDER_ICONNECT` from `ConfigurationRestService`
- [x] 2.2 Replace the body of `readBrpDoelbindingSetupEnabled()` with `configurationService.readBrpConfiguration().isDoelbindingPerZaaktype()`
- [x] 2.3 Update the KDoc comment on `readBrpDoelbindingSetupEnabled()` to reflect the new flag semantics (remove reference to iConnect)

## 3. Update Test Fixture

- [x] 3.1 Remove `brpProtocolleringProvider: Optional<String>` parameter from `createBrpConfiguration()` in `BrpUtilFixtures.kt`
- [x] 3.2 Remove `brpProtocolleringProvider = brpProtocolleringProvider` from the `BrpConfiguration(...)` constructor call in `BrpUtilFixtures.kt`
- [x] 3.3 Add `doelbindingPerZaaktype: Boolean = false` parameter to `createBrpConfiguration()`
- [x] 3.4 Pass `doelbindingPerZaaktype = doelbindingPerZaaktype` to the `BrpConfiguration(...)` constructor call

## 4. Update BrpConfigurationTest

- [x] 4.1 Remove the `"No default audit log provider configured"` test (checks for missing `BRP_PROTOCOLLERING`)
- [x] 4.2 Remove the `"Invalid BRP audit log provider specified"` test (checks for invalid provider string)
- [x] 4.3 Remove the `"BRP protocollering proxy / reading BRP audit log provider when disabled"` test that calls `readBrpProtocolleringProvider()`
- [x] 4.4 Add a test: when `doelbindingPerZaaktype = false`, `isDoelbindingPerZaaktype()` returns `false`
- [x] 4.5 Add a test: when `doelbindingPerZaaktype = true`, `isDoelbindingPerZaaktype()` returns `true`

## 5. Update ConfigurationRestServiceTest

- [x] 5.1 Replace `every { brpConfiguration.readBrpProtocolleringProvider() } returns "iConnect"` with `every { brpConfiguration.isDoelbindingPerZaaktype() } returns true` in the iConnect test case
- [x] 5.2 Replace `every { brpConfiguration.readBrpProtocolleringProvider() } returns "2Secure"` with `every { brpConfiguration.isDoelbindingPerZaaktype() } returns false` in the 2Secure test case
- [x] 5.3 Remove the `"BRP protocollering is disabled"` test case that returns `""` from `readBrpProtocolleringProvider()` (no longer relevant)
- [x] 5.4 Remove the `"BRP protocollering has an unknown provider"` test case (no longer relevant)
- [x] 5.5 Rename the two remaining Given blocks to describe the flag directly: `"doelbindingPerZaaktype is true"` and `"doelbindingPerZaaktype is false"`

## 6. Fix Remaining Test Callsites

- [x] 6.1 Remove `brpProtocolleringProvider = Optional.of("2Secure")` argument from `createBrpConfiguration()` call in `BrpClientServiceTest.kt` (line ~264) — the parameter no longer exists

## 7. Helm Chart Updates

- [x] 7.1 Remove `aanbieder: "iConnect"` from `brpApi.protocollering` in `charts/zac/values.yaml`; add `doelbindingPerZaaktype: false` with a comment explaining it enables per-zaaktype doelbinding UI
- [x] 7.2 Replace the `BRP_PROTOCOLLERING: {{ required ... .Values.brpApi.protocollering.aanbieder }}` line in `charts/zac/templates/config.yaml` with `BRP_DOELBINDING_PER_ZAAKTYPE: "{{ .Values.brpApi.protocollering.doelbindingPerZaaktype }}"` emitted **unconditionally** (outside the `originOin` `if` block)

## 8. Update .env.example

- [x] 8.1 Replace the `BRP_PROTOCOLLERING=iConnect` line with `BRP_DOELBINDING_PER_ZAAKTYPE=false` and add a comment: `# Set to true to require doelbinding values to be configured per zaaktype in the admin UI`

## 9. Update Integration Tests

- [x] 9.1 Remove `BRP_PROTOCOLLERING_ICONNECT = "iConnect"` constant from `ItestConfiguration.kt`
- [x] 9.2 Replace `"BRP_PROTOCOLLERING" to BRP_PROTOCOLLERING_ICONNECT` with `"BRP_DOELBINDING_PER_ZAAKTYPE" to "true"` in `ZacItestProjectConfig.kt`; remove the unused import of `BRP_PROTOCOLLERING_ICONNECT`
- [x] 9.3 Update the Then description in `ConfigurationRestServiceTest.kt` (itest) from `"'true' is returned because the BRP protocollering provider is set to 'iConnect' in the itest configuration"` to `"'true' is returned because BRP_DOELBINDING_PER_ZAAKTYPE is set to true in the itest configuration"`

## 10. Add Explicit `enabled` Flag — Replace `originOIN.isPresent` as the Enable Gate

- [x] 10.1 Add `@ConfigProperty(name = "BRP_PROTOCOLLERING_ENABLED", defaultValue = "false") private val protocolleringEnabled: Boolean` constructor parameter to `BrpConfiguration`, positioned before `originOIN`
- [x] 10.2 Change `isBrpProtocolleringEnabled()` body from `originOIN.isPresent` to `protocolleringEnabled`
- [x] 10.3 Add `"BRP_PROTOCOLLERING_ENABLED: '$protocolleringEnabled'"` line to `BrpConfiguration.toString()`, positioned before `BRP_ORIGIN_OIN`
- [x] 10.4 Add `protocolleringEnabled: Boolean = true` parameter to `createBrpConfiguration()` in `BrpUtilFixtures.kt`; pass it as `protocolleringEnabled = protocolleringEnabled` to the `BrpConfiguration(...)` constructor
- [x] 10.5 In `BrpClientHeadersFactoryTest.kt`: replace `createBrpConfiguration(originOin = Optional.empty())` with `createBrpConfiguration(protocolleringEnabled = false)` in the three test cases that use an empty OIN solely to disable protocollering (lines ~34, ~205, ~229); rename the first Given block from `"originOin is empty"` to `"protocollering is disabled"`
- [x] 10.6 Add `brpApi.protocollering.enabled: false` to `charts/zac/values.yaml` as the first entry under `protocollering`, with comment `# -- Set to true to enable BRP protocollering header injection`
- [x] 10.7 Add `BRP_PROTOCOLLERING_ENABLED: "{{ .Values.brpApi.protocollering.enabled }}"` unconditionally to `charts/zac/templates/config.yaml` (next to `BRP_DOELBINDING_PER_ZAAKTYPE`); change the `{{- if .Values.brpApi.protocollering.originOin.oin }}` gate to `{{- if .Values.brpApi.protocollering.enabled }}`
- [x] 10.8 Replace `BRP_PROTOCOLLERING_ENABLED=false` in `.env.example` with documentation comment: `# Set to true to enable BRP protocollering — required when BRP_ORIGIN_OIN is used`
- [x] 10.9 Add `BRP_PROTOCOLLERING_ENABLED` row to the `charts/zac/README.md` Helm values table
- [x] 10.10 Update `docs/manuals/brp-configuration/brp-configuration.md`: replace the description that enabling protocollering is triggered by a non-empty `originOin.oin` with `protocollering.enabled: true`; update both the iConnect and 2Secure Helm examples to include `enabled: true`
