## Context

The frontend runs Angular 20.3 with Jest 29. Angular 21 enables `typeCheckHostBindings` by default and changes some
defaults that affect the UI, such as CDK overlays. `@angular-builders/jest` 21 only supports Jest 30.

## Goals / Non-Goals

**Goals:**
- Angular 21 with Material, CDK and the dependent libraries on matching versions.
- No visible change for users.

**Non-Goals:**
- Angular 22, zoneless change detection, or rewriting code to new Angular 21 features. The upgrade adds no new
  Angular packages.

## Decisions

- **Two code pull requests.** The host-binding fixes compile on Angular 20, so they go first. That keeps the upgrade
  pull request limited to versions, migrations and the fixes the new versions force.
- **Jest 30 with Angular 21, not separately.** `@angular-builders/jest` 20 needs Jest 29 and version 21 needs
  Jest 30, so the two upgrades cannot be split.
- **Keep the current UI behaviour.** Where Angular 21 changes a default that changes what users see, we restore the
  old behaviour instead of adapting the UI.

## Risks / Trade-offs

- **CDK overlays render as native popovers in Angular 21.** Dialogs, menus, selects, tooltips and datepickers could
  stack differently against our own `z-index` rules. → Check them by hand; if they break, provide
  `OVERLAY_DEFAULT_CONFIG` with `usePopover: false`.
- **BPMN task forms** depend on `@formio/angular`. → Smoke test every BPMN task form.
- **Bundle size.** → Compare the production build's initial total with `main`.
