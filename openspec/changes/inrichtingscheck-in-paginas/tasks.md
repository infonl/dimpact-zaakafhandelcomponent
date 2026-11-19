# Tasks

## 1. Inrichtingscheck in pages of 25 (PR 2)

- [ ] 1.1 Baseline: on a local kind stack with about 200 generated zaaktypes, run a local k6 script (not checked in)
      against `GET /rest/health-check/zaaktypes` on the base commit, as design D6 describes, and time the page until
      it shows its first rows. Verify: the numbers are noted for task 1.9.
- [ ] 1.2 `HealthCheckService.controleerZaaktype` takes the `ZaakType` and the zaaktype configuration that the list
      already has, instead of the URL (D3). Verify: unit tests in `HealthCheckServiceTest` show that the zaaktype is
      not read again from `ZtcClientService` and that all checks give the same results as before.
- [ ] 1.3 `GET /rest/health-check/zaaktypes` takes the `@BeanParam` `RestInrichtingscheckParameters` (`page`, `rows`,
      `zoekterm`, `status`, `sortField`, `sortOrder`, with Bean Validation and enums) and returns a
      `RestResultaat<RestZaaktypeInrichtingscheck>`: check all eligible zaaktypes, filter, sort, page (D1, D2).
      Verify: unit tests in `HealthCheckRestServiceTest` for each scenario of the requirements "read in pages" and
      "filtered and sorted before it is paged", including the policy check.
- [ ] 1.4 `CmmnProcessBinding.isZaaktypeReady` reads the zaaktype and its configuration and passes them to
      `HealthCheckService.controleerZaaktype` (D3). Verify: `EngineProcessBindingsTest` shows that the check gets the
      zaaktype and the configuration, and `ZaaktypeRestServiceTest` (the zaaktypes for which a behandelaar can start a
      zaak) passes unchanged.
- [ ] 1.5 Update the integration test `HealthCheckRestServiceTest`: the page object of the list with its default
      filters, a filter on `zoekterm`, `rows` 101 giving `400`, and an unknown `status` or `sortField` giving `400`.
      Verify:
      `./gradlew itest --tests "nl.info.zac.itest.HealthCheckRestServiceTest"` passes.
- [ ] 1.6 Add an ADR (next free number, status `proposed`) "Read a paged list with GET and query parameters", add it
      to the index, and add the `GET` form to `docs/development/paging.md`. Verify:
      `python3 scripts/adr/check-adrs.py --base origin/main` passes.
- [ ] 1.7 Regenerate the OpenAPI spec and the frontend types (`./gradlew generateOpenApiSpec`, then the frontend
      type generation). In `HealthCheckService` (frontend) return `queryOptions()` for the list with the parameters
      in the query key. Verify: `npx ngc -p tsconfig.app.json --noEmit`
      passes.
- [ ] 1.8 Rebuild `InrichtingscheckComponent` on signals (D5): signals for the debounced `zoekterm`, the toggle
      `status`, the sort and the page index; `injectQuery` with `keepPreviousData`; a `mat-paginator` with pages of
      25 and the paginator translator; the expanded row by `uuid`; the table and its filter row always rendered with
      a progress bar while fetching; no `MatTableDataSource`, `effect`, `untracked` or `ngOnInit`. Migrate
      `inrichtingscheck.component.spec.ts` to Testing Library and cover each scenario of the requirement "shows the
      list in pages of 25" and the E-formulier scenario. Verify: `npm test -- --test-path-patterns inrichtingscheck`
      and `npm run lint` pass.
- [ ] 1.9 Measure again as in task 1.1 on the image of this layer (D6), also right after a restart, and add a section
      `Measurements` with the before and after numbers to `design.md` of this change and to the pull request
      description. Verify: the section has response time (median, p95), response size (uncompressed and gzip) and the
      time until the page shows its first rows, before and after.
- [ ] 1.10 Update the section *Zaaktype inrichtingscheck* of
      `docs/manuals/inrichting-zaakafhandelcomponent/inrichting-zaakafhandelcomponent.md`: the list shows 25
      zaaktypes per page and the filter searches in the omschrijving and the doel. Verify: the section describes the paginator and
      the filter.
- [ ] 1.11 Run `./gradlew spotlessApply detektApply`, `./gradlew detekt` and `./gradlew test`, and check the page by
      hand on the kind stack. Verify: all pass and the page shows pages of 25 with a working filter, toggle and sort.

## Workflow follow-up

- Night Shift opens the archive pull request of this change after the code layer is merged.
