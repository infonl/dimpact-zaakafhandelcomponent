## 1. Backend: RestZaak field

- [x] 1.1 Add `isZaakspecifiekGeautoriseerd: Boolean` to `RestZaak`
      (`src/main/kotlin/nl/info/zac/app/zaak/model/RestZaak.kt`), with a
      `@get:JsonbProperty("isZaakspecifiekGeautoriseerd")` annotation matching the pattern of the
      other `isXxx` fields in that class.

## 2. Backend: detection logic

- [x] 2.1 In `RestZaakConverter.toRestZaak`
      (`src/main/kotlin/nl/info/zac/app/zaak/converter/RestZaakConverter.kt`), read the zaak's
      zaakeigenschappen via `zrcClientService.listZaakeigenschappen(zaak.uuid)` and set
      `isZaakspecifiekGeautoriseerd` to `true` only when a `ZaakEigenschap` exists with
      `naam == "ZAAK_GEAUTORISEERD"` and `waarde == "true"` (exact match), `false` otherwise.
- [x] 2.2 Add/update Kotest unit tests for `RestZaakConverter` covering: the eigenschap present with
      value `"true"`, the eigenschap absent, and the eigenschap present with a different value (e.g.
      `"false"`).

## 3. API contract regeneration

- [x] 3.1 Regenerate the OpenAPI spec (`./gradlew generateOpenApiSpec`) and the frontend generated
      types (`npm run generate:types:zac-openapi` in `src/main/app/`) so `RestZaak` in
      `src/main/app/src/generated/types/zac-openapi-types.d.ts` includes
      `isZaakspecifiekGeautoriseerd`.

## 4. Frontend: zaakdetailpagina lock icon

- [x] 4.1 In `zaak-view.component.html`, add a `mat-icon` showing `lock` directly in front of
      `{{ zaak.identificatie }}` inside the existing `mat-card-title`, shown only when
      `zaak.isZaakspecifiekGeautoriseerd` is `true`.
- [x] 4.2 Add/update `zaak-view.component.spec.ts` cases asserting the lock icon is present when
      `isZaakspecifiekGeautoriseerd` is `true` and absent when it is `false` (or undefined, matching
      current behavior).

## 5. Verification

- [x] 5.1 Run `./gradlew spotlessApply detektApply` and `./gradlew test --tests
      "nl.info.zac.app.zaak.converter.RestZaakConverterTest"` (or the applicable test class).
- [x] 5.2 Run `cd src/main/app && npm test` for the affected `zaak-view` spec.
- [x] 5.3 Manually verify in a running stack: a zaak with a `ZAAK_GEAUTORISEERD`/`true`
      zaakeigenschap shows the lock icon on its zaakdetailpagina; a zaak without it does not, and no
      other page shows the icon.
