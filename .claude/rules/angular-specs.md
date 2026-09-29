---
paths:
  - "src/main/app/src/**/*.spec.ts"
---

# Angular spec conventions

- Import the standalone component under test directly; it declares its own template dependencies in its `imports` array.
- Use `fromPartial` from `@total-typescript/shoehorn` to create partial mocks of generated types.

## Query the DOM through Testing Library, not through Angular
New and modified specs use [Testing Library](https://testing-library.com/docs/queries/about/#priority)
to reach the DOM. Prefer `getByRole` with an accessible name; fall back to `getByLabelText`
and `getByText` only when no role fits. A spec that queries by role fails when the markup
stops being accessible, which is behaviour worth testing on its own.

```ts
// Before
const row = fixture.nativeElement.querySelector("tr.zaak-row");
// After
const row = screen.getByRole("row", { name: /ZAAK-001/ });
```

Older specs that still use the Angular style are warnings project-wide, but
**errors on any spec file a pull request touches** — so a spec you edit has to be migrated
before it merges. See [linting-strategy.md](../../docs/development/linting-strategy.md) for the
command that reproduces that check locally.

Where a third-party widget renders nothing queryable — `ngx-editor` sets no role on its
ProseMirror element, OpenLayers draws to a canvas, a file input is `display: none` —
disable the rule on that line with a comment saying which widget forces it.
