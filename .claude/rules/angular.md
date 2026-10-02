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

## Translate a camelCase identifier through `toI18nKey`
`translations.spec.ts` requires the last segment of every i18n key to be kebab-case, so a key named
after a column, form control or validator error (`zaakIdentificatie`) is `zaak-identificatie`. When the
key is built from such an identifier at runtime, convert it with `toI18nKey` or the `i18nKey` pipe
(`{{ column | i18nKey | translate }}`). Form fields already do this for their `key`.
