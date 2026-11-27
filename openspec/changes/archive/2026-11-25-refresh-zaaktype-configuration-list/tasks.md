## 1. Frontend

- [x] 1.1 Set `staleTime: StaleTimes.Instant` on the list query of `ParametersComponent` (design D1).
- [x] 1.2 Set `refetchOnWindowFocus: true` on that query (design D2).

## 2. Tests

- [x] 2.1 In `parameters.component.spec.ts`, check that the list is read again when the beheerder returns to the page, and when the beheerder comes back to the browser tab, and that the row then shows the procesmodel. Both specs fail without their setting.

## 3. Checks

- [x] 3.1 Run ESLint and Prettier on `src/app/admin/parameters/`.
- [x] 3.2 Run `npx ng test --test-path-patterns=src/app/admin/parameters/parameters.component.spec.ts`.
