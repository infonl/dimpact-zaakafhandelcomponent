/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

# Bundle Size Migration Plan

## Goal

Reduce the initial JS bundle from ~6.5 MB to ~2–3 MB by lazy-loading heavy libraries
and ultimately switching to `bootstrapApplication()` (AOT-only, no JIT compiler in prod).

## Baseline

Run before each phase to track progress:

```bash
cd src/main/app
npm run build -- --stats-json
# or just check build output for initial bundle size
```

Current state (2026-03-19): **~6.5 MB main bundle**, no lazy loading, JIT bootstrap.

---

## Phase 1 — Lazy-load Form.io (quick win, ~1.5–2 MB)

**Why**: `@formio/angular` is ~46 MB on disk; estimated ~1.5–2 MB in the bundle.
It's only needed on `/taken/*` routes (task forms). Currently pulled in eagerly via
`TakenModule` → `FormulierenModule`.

### Steps

- [ ] Convert `FormulierenModule` to lazy-loadable (ensure it has its own routing or is
      loadable via `loadChildren`)
- [ ] In `TakenModule` routing (or `app-routing`), replace eager `TakenModule` import
      with `loadChildren(() => import('./taken/taken.module').then(m => m.TakenModule))`
- [ ] Verify `FormulierenModule` is no longer in `AppModule` imports (directly or transitively)
- [ ] Build and confirm Form.io appears in a separate chunk, not `main.js`
- [ ] Smoke test: open a task with a dynamic form — form still renders correctly

**Expected result**: initial bundle drops ~1.5–2 MB.

---

## Phase 2 — Lazy-load OpenLayers (medium win, ~300–500 KB)

**Why**: `ol` (OpenLayers) + `proj4` are only used on address/location features
(`BAGModule`). Currently loaded for every user on every page.

### Steps

- [ ] Identify all routes that use `BAGModule` components
- [ ] Extract `BAGModule` into a lazy chunk (either its own route or dynamic import
      inside the component(s) that use it)
- [ ] Verify `ol` and `proj4` no longer appear in `main.js` (check build stats)
- [ ] Smoke test: address lookup / map features still work

**Expected result**: initial bundle drops ~300–500 KB.

---

## Phase 3 — Lazy-load AdminModule (small win, ~60–80 KB)

**Why**: Only admin users access `/admin`. `ngx-editor` (rich text) is pulled in here.
Low effort, makes architectural sense regardless of size.

### Steps

- [ ] In `app-routing`, replace eager `AdminModule` with:
      `loadChildren(() => import('./admin/admin.module').then(m => m.AdminModule))`
- [ ] Remove `AdminModule` from `AppModule.imports`
- [ ] Verify admin routes still work (navigate to `/admin/check`)
- [ ] Verify `ngx-editor` moves out of `main.js`

**Expected result**: initial bundle drops ~60–80 KB.

---

## Phase 4 — Switch to `bootstrapApplication()` (ultimate goal, ~500 KB–1 MB)

**Why**: `platformBrowserDynamic` + `@angular/compiler` in prod deps ships the full
Angular JIT compiler to every user. Switching to AOT-only bootstrap removes it.

**Prerequisite**: all components must be standalone (the ongoing standalone migration).

### Steps

- [ ] Standalone migration complete (see `migrate-ng19-standalone-components.md`)
- [ ] Replace `src/main.ts` bootstrap:
  ```typescript
  // Before
  platformBrowserDynamic().bootstrapModule(AppModule)

  // After
  bootstrapApplication(AppComponent, appConfig)
  ```
- [ ] Create `app.config.ts` with `provideRouter`, `provideHttpClient`, `provideAnimations`,
      TanStack Query provider, and other root providers currently in `AppModule`
- [ ] Delete `AppModule` (or keep temporarily during transition)
- [ ] Move `@angular/compiler` from `dependencies` to `devDependencies` (or remove entirely)
- [ ] Remove `@angular/platform-browser-dynamic` from prod deps
- [ ] Build and verify compiler is not in the bundle
- [ ] Full regression test of the app

**Expected result**: initial bundle drops ~500 KB–1 MB. Lazy-loaded routes also become
simpler (`loadComponent` instead of `loadChildren`).

---

## Summary

| Phase | Change | Est. Saving | Status |
|---|---|---|---|
| 1 | Lazy-load Form.io via TakenModule | ~1.5–2 MB | [ ] |
| 2 | Lazy-load OpenLayers via BAGModule | ~300–500 KB | [ ] |
| 3 | Lazy-load AdminModule | ~60–80 KB | [ ] |
| 4 | `bootstrapApplication` + drop compiler | ~500 KB–1 MB | [ ] |
| **Total** | | **~2.4–3.6 MB** | |

Target initial bundle after all phases: **~3–4 MB → gzipped ~800 KB–1.2 MB over the wire**.
