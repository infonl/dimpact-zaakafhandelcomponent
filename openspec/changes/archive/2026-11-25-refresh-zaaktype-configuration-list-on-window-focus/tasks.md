## 1. Frontend

- [x] 1.1 Refetch the list query of `ParametersComponent` when its window gets the focus, and remove `refetchOnWindowFocus: true` (design D1).

## 2. Tests

- [x] 2.1 In `parameters.component.spec.ts`, check that a `focus` event on the window reads the list again and that the row then shows the procesmodel. The spec fails without the change.
- [x] 2.2 Remove the fixture that dated a cached page six minutes back, now that every cached page is stale.

## 3. Checks

- [x] 3.1 Run ESLint and Prettier on `src/app/admin/parameters/`.
- [x] 3.2 Run `npx ng test --test-path-patterns=src/app/admin/parameters/parameters.component.spec.ts`.
- [ ] 3.3 Check in a browser that the list is read again after switching tabs, after Cmd+Tab back from another window, and after clicking back into the ZAC window next to another window.
