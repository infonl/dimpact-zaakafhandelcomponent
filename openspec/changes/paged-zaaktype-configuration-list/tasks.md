## 1. Backend (PZ-12915)

- [x] 1.1 Add `RestZaaktypeConfigurationListParameters`, `RestZaaktypeConfigurationSortField`,
      `RestZaaktypeFilterOptie` and `RestZaaktypeConfigurationOverzichtResultaat` (design D2, D4).
- [x] 1.2 Add the functions that filter, sort and page a list of `RestZaaktypeConfigurationOverzicht` (design D3,
      D5). Verify with `RestZaaktypeConfigurationOverzichtPageTest`.
- [x] 1.3 Make `GET /rest/zaakafhandelparameters` of `ZaaktypeConfigurationRestService` return a page for its query
      parameters, with the beheren check, and add a `LocalDateParamConverter` (design D2). Let
      `ZaakafhandelParametersService` read one page with all configurations, so the page keeps working. Verify with
      `ZaaktypeConfigurationRestServiceTest`, `LocalDateParamConverterTest` and the itest
      `ZaaktypeConfigurationRestServicePagingTest`.
- [x] 1.4 Run `./gradlew spotlessApply detektApply detekt` and check the generated OpenAPI spec.

## 2. Frontend (PZ-12914)

- [ ] 2.1 Make `ZaakafhandelParametersService` list a page through `GET /rest/zaakafhandelparameters` with query
      parameters, instead of one page with all configurations.
- [ ] 2.2 Make `ParametersComponent` request a page of 25 rows with its filters and sort order, show a paginator,
      and take the filter options from the response. Go back to the first page when a filter or the sort order
      changes. Verify with `parameters.component.spec.ts`.
- [ ] 2.3 Check that no frontend code reads the list of all configurations any more.
- [ ] 2.4 Run `npm run lint` and `npm test` in `src/main/app`, and `./gradlew spotlessApply detektApply detekt`.
