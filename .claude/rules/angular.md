---
paths:
  - "src/main/app/src/**/*.ts"
  - "src/main/app/src/**/*.html"
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

## Look up an i18n key built at runtime through `toI18nKey`
When you build a key from a prefix and an enum value, field name or external value, pass it through
`toI18nKey` or the `i18nKey` pipe before you translate it: `{{ "taak.status." + taak.status | i18nKey | translate }}`.
