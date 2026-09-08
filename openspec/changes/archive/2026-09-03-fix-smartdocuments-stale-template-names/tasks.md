## 1. Live ID-based name resolution

- [x] 1.1 Add a function that resolves one or more `smartDocumentsId`s (template group and/or template) against the tree returned by `SmartDocumentsService.listTemplates()`, returning the current name(s) or an explicit "not found" per id — `findGroupById`/`findTemplateById` in `RestSmartDocumentsTemplateGroup.kt`, used by `SmartDocumentsTemplatesService.readCurrentSelection`, which returns the existing `Selection` model (`templateGroup`/`template` fields) instead of a new data class
- [x] 1.2 Unit test: resolves both a template group name and a template name from a single live tree fetch, given matching ids
- [x] 1.3 Unit test: reports "not found" distinctly (not an exception swallowed into a default) when an id no longer exists in the live tree

## 2. Document creation flow

- [x] 2.1 Replace the `getTemplateGroupName`/`getTemplateName` DB lookups in `DocumentCreationService.createDocumentForAttendedFlow` with the resolver from Task 1, using exactly one live fetch for both names
- [x] 2.2 On a "not found" id, fail the request with a clear, actionable error (mapping needs updating) instead of sending a stale or empty name
- [x] 2.3 On a live-fetch failure (SmartDocuments unreachable, auth error, timeout), fail the request with a clear error and do NOT fall back to the persisted name — no fallback path exists; any exception from `readCurrentSelection` propagates unchanged
- [x] 2.4 Unit test: document creation sends the current SmartDocuments name after a rename and succeeds
- [x] 2.5 Unit test: document creation fails with the new explicit error when the mapped id no longer exists live
- [x] 2.6 Unit test: document creation fails with the new explicit error when the live fetch itself errors, and never falls back to the persisted name

## 3. Template mapping read endpoint (display)

- [x] 3.1 Change `SmartDocumentsTemplatesService.getTemplatesMapping` to resolve names from a live SmartDocuments fetch, overlaid with the persisted `informatieObjectTypeUUID` per `smartDocumentsId`, instead of reading the persisted `naam` — via `resolveCurrentNames` in `RestMappedSmartDocumentsTemplateGroup.kt`
- [x] 3.2 Omit any template or group whose `smartDocumentsId` no longer exists live from the returned mapping, consistent with how an unmapped template is omitted today
- [x] 3.3 Unit test: mapping response reflects the current/live name for a renamed template while keeping its persisted `informatieobjecttype` correctly attached — tested directly against the pure `resolveCurrentNames` function in `RestSmartDocumentsTemplateGroupTest.kt` rather than through JPA-mocked service tests, since the merge logic itself has no JPA dependency
- [x] 3.4 Unit test: mapping response omits a template/group whose id no longer exists live — same test file as 3.3

## 4. Frontend verification and cleanup

- [x] 4.1 Verify `InformatieObjectCreateAttendedComponent`'s "Document maken" picker shows the live/current names against the updated backend response — confirmed by reading the component: it binds `.name` directly from `getTemplatesMapping()`'s response with no other transform, so no frontend change is needed
- [x] 4.2 Verify `formio-setup-service.ts`'s SmartDocuments task form fields show the live/current names against the updated backend response — same finding as 4.1
- [x] 4.3 Assess whether the client-side `addTemplateMappings` merge in `smart-documents.service.ts` (used by the admin koppelingen screen) is now redundant against the updated backend response; simplify it or document why it stays — **finding: it is not redundant and needs no change.** It never read the mapping endpoint's `name` field; it only overlays `informatieObjectTypeUUID` values (extracted via `getTemplateMappings`) onto a separately-fetched live tree, matched by `(templateId, parentGroupId)`. The koppelingen screen was never affected by the staleness bug and this merge solves an unrelated problem. `design.md`/`proposal.md` updated to correct the original (incorrect) assumption that this merge would become redundant.

## 5. End-to-end verification

- [x] 5.1 In a test/acceptance environment, rename a mapped template and a mapped group in SmartDocuments; confirm the "Document maken" picker and the BPMN task form show the new names without re-saving the zaaktype's mapping, and confirm document generation succeeds — **confirmed by user: manual test passed, names shown correctly**
- [x] 5.2 Confirm the "template no longer exists" and "SmartDocuments unreachable" error paths surface a clear, correctly-translated message to the user — **manually verified.** The "not found" path was forced with a temporary hardcoded bad id (reverted afterwards) and surfaces the existing generic `msg.error.smartdocuments.not.configured` translation (shared with other pre-existing `SmartDocumentsConfigurationException` scenarios; the detailed cause is logged server-side, not shown to the user). User confirmed this generic message is acceptable — a more specific message was considered but explicitly not pursued, to keep this fix minimal.

## 6. Follow-ups (tracked separately, not blocking this change)

- [x] 6.1 Decide whether to add a short-TTL cache for the live SmartDocuments read — **decided: no, not now.** No evidence yet that the extra call is actually slow; adding a cache without a measured problem is unnecessary complexity (TTL tuning, invalidation). Revisit only if post-rollout metrics show real latency or rate-limit issues.
- [x] 6.2 Decide whether to deprecate or remove the persisted `naam` columns on `SmartDocumentsTemplate`/`SmartDocumentsTemplateGroup` — **decided: remove them**, per explicit user request. Done in `V98__remove_smartdocuments_naam_column.sql`, with the `name`/`naam` property removed from both JPA entities and every place that constructed or read it updated accordingly (`RestMappedSmartDocumentsTemplateGroup.kt`'s conversion functions now emit a placeholder name that `resolveCurrentNames` always replaces).
