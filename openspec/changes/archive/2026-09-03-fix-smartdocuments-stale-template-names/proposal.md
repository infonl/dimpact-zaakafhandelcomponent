## Why

When a template or template group is renamed inside SmartDocuments, ZAC keeps using the name it cached at the moment an admin last saved the zaaktype's template mapping. That stale name is not just a cosmetic label: ZAC sends it verbatim to SmartDocuments' own document-creation wizard (`Selection.templateGroup` / `Selection.template`), which SmartDocuments resolves by name. After a rename, SmartDocuments can no longer find the template under the old name, so "Document maken" fails outright for the end user. The same stale name also shows up in the "Document maken" dropdown and in BPMN human task forms, so users cannot tell which template they are actually about to generate. The only screen that already shows the current name is the admin "koppelingen" page, because it happens to merge a live SmartDocuments fetch with the persisted mapping — the fix is to apply that same principle everywhere ZAC talks about a template's name.

## What Changes

- The document-generation flow (`DocumentCreationService.createDocumentForAttendedFlow`) resolves the current template group and template name live from SmartDocuments, matched by `smartDocumentsId`, instead of reading the persisted `naam` column. Both names are resolved from a single live fetch, not two.
- If the configured `smartDocumentsId` no longer exists in SmartDocuments' current template tree, document generation fails with a clear, actionable error (e.g. pointing at the need to update the zaaktype's SmartDocuments mapping) instead of silently sending a stale or empty name.
- If the live SmartDocuments fetch itself fails (service unavailable, auth error, etc.), document generation fails with a clear error rather than silently falling back to the persisted (possibly stale) name — sending a stale name is what causes the current failures, so a stale fallback is not an acceptable substitute for a real error.
- The template mapping read path used by the "Document maken" dropdown and by BPMN human task form setup (`smartdocuments-templates-mapping` GET) resolves display names the same way the admin "koppelingen" page already does: overlay the persisted `informatieObjectTypeUUID` per template onto a live-fetched SmartDocuments tree, matched by `smartDocumentsId`, rather than reading the persisted `naam` snapshot directly.
- A template or group whose `smartDocumentsId` no longer exists live is omitted from the mapping shown to end users, the same way an unmapped template is omitted today.
- **BREAKING (internal only)**: none of the persisted data or REST contracts change shape; this only changes where the returned `name` values come from. No API consumers outside ZAC are affected.

## Capabilities

### New Capabilities
- `smartdocuments-template-name-resolution`: defines that ZAC must resolve current SmartDocuments template/group names live (matched by `smartDocumentsId`) at the point of use — both for the actual document-creation request sent to SmartDocuments, and for any name shown to a user — rather than trusting the name snapshot persisted at mapping-save time.

### Modified Capabilities
_None — no existing `openspec/specs/` capability currently covers SmartDocuments template mapping or document creation._

## Impact

- Backend: `nl/info/zac/documentcreation/DocumentCreationService.kt`, `nl/info/zac/smartdocuments/SmartDocumentsTemplatesService.kt` (`getTemplateName`/`getTemplateGroupName`/`getTemplatesMapping`), `nl/info/zac/smartdocuments/SmartDocumentsService.kt` (`listTemplates`), `nl/info/zac/app/admin/ZaaktypeConfigurationRestService.kt` (`GET smartdocuments-templates-mapping`).
- Frontend: `informatie-object-create-attended.component.ts` and `formio-setup-service.ts` keep calling the same mapping endpoint but now receive live names, with no frontend code changes required. The admin koppelingen screen (`smart-documents-form.component.ts`) is unaffected either way: it builds its tree from a separately-fetched live tree and only overlays `informatieObjectTypeUUID` values from the mapping endpoint — it never reads that endpoint's `name` field, so it was never subject to this bug and needs no change.
- Performance: document generation and every "Document maken"/BPMN task-form load now trigger one additional live SmartDocuments call (`GET sdapi/structure`, full tree — SmartDocuments exposes no per-ID lookup). No caching exists today; this proposal does not introduce one, but flags it as a likely follow-up if the extra call proves too slow or SmartDocuments-side rate limits are hit.
- No database schema change is required. The persisted `naam` columns on `SmartDocumentsTemplate`/`SmartDocumentsTemplateGroup` remain, but stop being read as the source of truth for anything shown to or sent on behalf of a user; they remain useful only as a last-known-name reference for the admin mapping screen.
