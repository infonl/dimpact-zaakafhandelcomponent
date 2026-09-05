## Context

ZAC persists a snapshot of each SmartDocuments template/group `name` at the moment an admin saves a zaaktype's template mapping (`SmartDocumentsTemplate`/`SmartDocumentsTemplateGroup` entities, `naam` column). That snapshot is later read back in two places:

- `DocumentCreationService.createDocumentForAttendedFlow` uses `SmartDocumentsTemplatesService.getTemplateGroupName`/`getTemplateName` (pure DB lookups by `smartDocumentsId`) to build the `Selection` sent to SmartDocuments' `wsxmldeposit/deposit/wizard` endpoint. That endpoint has no ID-based alternative — it only accepts current name strings.
- `SmartDocumentsTemplatesService.getTemplatesMapping` (backing the `GET .../smartdocuments-templates-mapping` endpoint) reads the same persisted names for the "Document maken" template picker (`InformatieObjectCreateAttendedComponent`) and for BPMN human task form setup (`formio-setup-service.ts`).

The only SmartDocuments read capability that returns current data is `SmartDocumentsService.listTemplates()` (`GET sdapi/structure`), which always returns the full template/group tree — SmartDocuments exposes no "get by id" or "get subtree" operation. The admin "koppelingen" screen already avoids the staleness problem today, but only because its frontend component fetches this live tree itself and overlays the persisted `informatieObjectTypeUUID` on top of it (`addTemplateMappings` in `smart-documents.service.ts`), rather than trusting the persisted `naam`.

This change generalizes that same principle — resolve names live, match everything by `smartDocumentsId` — to the two places that still don't do it: document creation and the mapping-read endpoint used by the create-document picker and BPMN task forms.

**Confirmation that SmartDocuments cannot be sent an ID instead of a name.** This was checked against two independent sources in the repo, not just the current ZAC client wiring:
1. The `Selection` request model (`nl/info/client/smartdocuments/model/document/SmartDocumentsDocumentRequestDeclarations.kt`) only has `templateGroup`/`template` string fields (`"TemplateGroup"`/`"Template"`) — no ID field, and `SmartDocumentsClient` exposes no ID-based deposit operation.
2. The WireMock fixture used to emulate the real SmartDocuments API in integration tests (`scripts/docker-compose/imports/smartdocuments-wiremock/mappings/wsxmldeposit-wizard-behandelaar1.json`) asserts the exact `POST /wsxmldeposit/deposit/wizard` request body, and it requires plain name strings: `"Selection": {"TemplateGroup": "root", "Template": "root template 1", ...}` — no ID. This fixture models the real external contract for testing purposes, independent of ZAC's own client code, and corroborates the same conclusion.

`sdapi/structure` (`scripts/docker-compose/imports/smartdocuments-wiremock/__files/sdapi-structure-response-body.json`) does return a stable `id` alongside `name` per node, confirming the ID is the right thing to key matching on — it just can't be sent back to start the wizard. Both sources agree: there is no ID-based alternative for document creation, so the fix must resolve the current name live before every use rather than trying to route around name-based selection. (Neither source is SmartDocuments' own official API documentation, which was not available to check directly — but two independent, differently-motivated artifacts in this codebase agreeing is strong internal evidence.)

## Goals / Non-Goals

**Goals:**
- Guarantee that any template/group name ZAC sends to SmartDocuments, or shows to a user, reflects SmartDocuments' current state rather than a persisted snapshot.
- Fix document-generation failures caused by a template or group rename in SmartDocuments.
- Fix stale template names shown in the "Document maken" picker and in BPMN human task forms.
- Keep all resolution keyed exclusively on `smartDocumentsId`, at every level of the group hierarchy, so renames never break matching.

**Non-Goals:**
- Changing how the mapping's `informatieobjecttype` assignment is stored or saved. `storeTemplatesMapping` stays a full delete-then-reinsert, unchanged by this proposal.
- Introducing a caching layer for the live SmartDocuments read. Deferred — see Open Questions.
- Changing the SmartDocuments document-creation API contract. It is strictly name-based; there is no ID-based alternative to switch to.
- Fixing the pre-existing lost-update risk in `storeTemplatesMapping` (delete-all/reinsert-all with no optimistic locking, discovered during investigation). That is a separate concern from name staleness and is not in scope here.
- Removing the two currently unused, name-path-based endpoints (`smartdocuments-group-template-names`, `smartdocuments-template-group`). Dead-code cleanup, tracked separately.

## Decisions

**1. Resolve names by walking a single live `listTemplates()` tree, matched by `smartDocumentsId` — not by extending the name-path mechanism.**
`RestSmartDocumentsPath` and the two endpoints built on it (`getTemplateGroup`, `listGroupTemplateNames`) already do a "live" lookup, but by a name breadcrumb, which is exactly the kind of matching this fix needs to get away from. Instead, add a small ID-based lookup over the tree `listTemplates()` already returns: given one or more `smartDocumentsId`s, find the corresponding node(s) (group and/or template) anywhere in the tree. This is a new, small piece of logic, not a reuse of the path-based mechanism.

**2. Resolve both the template group name and the template name for one document-creation request from a single live fetch.**
`createDocumentForAttendedFlow` needs both names. Calling `listTemplates()` twice (once per name) would double an already-external, potentially slow call for no benefit. Fetch once, resolve both from the same result.

**3. Centralize the live+persisted merge for display in the backend (`getTemplatesMapping`), rather than duplicating a second frontend merge in more places.**
The admin koppelingen screen (`smart-documents-form.component.ts`) already avoids stale names, but not the way initially assumed: it builds its tree from `getAllSmartDocumentsTemplateGroups()` (always the live tree) as the base, and overlays only the `informatieObjectTypeUUID` values extracted from the persisted mapping (via `getTemplateMappings`/`addTemplateMappings`, matched on `(templateId, parentGroupId)`) — it never reads the `.name` field from the persisted-mapping endpoint at all. So that screen was never affected by this bug, and `addTemplateMappings` is solving a different problem (turning a flat persisted UUID list into overlay data for an editable live tree) that is orthogonal to name staleness. It does not become redundant and is not touched by this change.
What *does* need the same principle applied is `InformatieObjectCreateAttendedComponent` and `formio-setup-service.ts`, which both read `.name` directly from `getTemplatesMapping()`'s response with no live tree of their own to fall back on. Doing the live+persisted merge once, backend-side, inside `getTemplatesMapping()` fixes both call sites via the one endpoint they already call, with no frontend changes required — the REST contract shape (`RestMappedSmartDocumentsTemplateGroup`/`RestMappedSmartDocumentsTemplate`) is unchanged, only the `name` values returned differ.

**4. Fail loudly, not silently, when a live lookup fails or a mapped ID no longer exists.**
Considered falling back to the persisted (stale) name if the live fetch fails, to preserve availability. Rejected: a stale fallback is exactly today's bug — it produces a confusing failure further down the pipe (SmartDocuments itself rejecting the request) instead of a clear one. Failing immediately, with a message that points at the actual cause (mapping needs updating, or SmartDocuments is unreachable), is strictly more useful and is no worse than the current failure mode, which already fails for the same underlying reason.

**5. No schema change; the persisted `naam` columns stay, but stop being treated as a source of truth.**
Dropping them now would be unrelated schema churn. They remain useful as a last-known-name reference for admins inspecting the mapping, and can be revisited once it's confirmed nothing else depends on them.

## Risks / Trade-offs

- **[Risk]** Every document generation, and every "Document maken"/BPMN task form load, now triggers one additional live SmartDocuments call (`GET sdapi/structure`, full org-wide tree — no per-ID lookup exists). → **Mitigation**: these flows already depend on SmartDocuments being reachable; monitor latency/error rates after rollout and add a short-TTL cache only if it proves necessary.
- **[Risk]** For organizations with very large SmartDocuments catalogs, `sdapi/structure` may be a large/slow response. → **Mitigation**: this is an existing constraint of SmartDocuments' API, not one this change introduces; same fallback as above.
- **[Risk]** Centralizing the merge in `getTemplatesMapping()` changes what every current caller of that endpoint receives, including the admin koppelingen screen. → **Mitigation**: verified this is harmless — the koppelingen screen only extracts `informatieObjectTypeUUID` values from this endpoint's response (via `getTemplateMappings`) and overlays them onto its own separately-fetched live tree; it never reads this endpoint's `name` field, so returning live-resolved names here changes nothing observable on that screen.
- **[Risk]** Turning a silent staleness bug into a hard failure could surface previously-hidden broken mappings more visibly right after rollout. → **Mitigation**: those mappings were already functionally broken (SmartDocuments was already rejecting the stale name); the change only replaces an opaque SmartDocuments-side error with a clear ZAC-side one.
- **[Risk]** Time-of-check/time-of-use gap between the live fetch and the actual SmartDocuments wizard POST — a rename or deletion could still occur in that narrow window. → **Mitigation**: accepted as a low-probability, low-impact race; no stronger consistency is achievable without transactional support from SmartDocuments itself.

## Migration Plan

No data migration is required — no schema changes, and existing persisted mappings (`smartDocumentsId` + `informatieObjectTypeUUID`) remain valid inputs to the new live-resolution logic unchanged. Rollout is a standard code deploy. Rollback is a standard revert; there is no persisted-state migration to undo.

Before rollout, validate against at least one zaaktype with a real SmartDocuments mapping in a test/acceptance environment: rename a mapped template and a mapped group in SmartDocuments, then confirm (a) the "Document maken" picker shows the new names without re-saving the mapping, (b) document generation using that template succeeds, and (c) the BPMN task form field shows the new names.

## Open Questions

- Should the extra live SmartDocuments read get a short-TTL cache from day one, or only if post-rollout metrics show it's needed?
- Should the persisted `naam` columns be deprecated/removed in a later follow-up now that they are no longer read as source of truth?
- Exact user-facing error copy (and i18n keys) for "template no longer exists in SmartDocuments" and "SmartDocuments unreachable" need sign-off before implementation.
