---
paths:
  - "src/main/app/src/**/*.ts"
  - "src/main/app/src/**/*.html"
  - "src/main/app/src/assets/i18n/*.json"
---

# Angular conventions

## Reuse existing TanStack Query definitions
Before writing a new `injectQuery`/`ensureQueryData` call, check whether the relevant service already exposes
a `queryOptions()`-based method for that endpoint (e.g. `SmartDocumentsService.getTemplatesMappingQuery`,
`InformatieObjectenService.listEnkelvoudigInformatieobjectenQuery`). Reuse it instead of inlining a new
`{ queryKey, queryFn }` object — duplicating the query key and fetch logic across components risks the keys
drifting out of sync (breaking the shared cache) and multiplies the places a bug must be fixed.
```ts
// Before
private readonly someQuery = injectQuery(() => ({
  queryKey: ["smartDocumentsTemplatesMapping", this.zaaktypeUuid],
  queryFn: () => lastValueFrom(this.smartDocumentsService.getTemplatesMapping(this.zaaktypeUuid)),
}));
// After
private readonly someQuery = injectQuery(() =>
  this.smartDocumentsService.getTemplatesMappingQuery(this.zaaktypeUuid),
);
```
If no shared method exists yet for the endpoint you need, add one to the relevant service using
`queryOptions()` from `@tanstack/angular-query-experimental`, so future callers can reuse it too.

## Use kebab-case for the last segment of frontend i18n message keys
In `src/main/app/src/assets/i18n/nl.json` and `en.json`, when the last segment of a message key is
multi-word, write it in kebab-case, not camelCase — e.g. `msg.document.verwijderen.inbox.niet-verwijderd`,
not `msg.document.verwijderen.inbox.nietVerwijderd`. This applies even when the segment is named after
a camelCase variable (such as an `isXxx` boolean) elsewhere in the code — the i18n key still uses kebab-case.
