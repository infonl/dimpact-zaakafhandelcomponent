/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
// The files that handled dates and times outside src/app/date-time before the date-time rules of eslint.config.js
// were added. This list may only shrink: move the date handling of a file into src/app/date-time and remove it here.
module.exports = [
  "src/app/core/toolbar/toolbar.component.ts",
  "src/app/dashboard/dashboard.component.ts",
  "src/app/documenten/inbox-documenten-list/inbox-documenten-list.component.spec.ts",
  "src/app/documenten/ontkoppelde-documenten-list/ontkoppelde-documenten-list.component.spec.ts",
  "src/app/productaanvragen/inbox-productaanvragen-list/inbox-productaanvragen-list.component.spec.ts",
  "src/app/shared/form/date/date.spec.ts",
  "src/app/shared/form/date/date.ts",
  "src/app/shared/pipes/dagen.pipe.ts",
  "src/app/shared/pipes/datum.pipe.ts",
  "src/app/shared/utils/date-conditionals.spec.ts",
  "src/app/shared/utils/date-conditionals.ts",
  "src/app/taken/taak-view/taak-view.component.spec.ts",
  "src/app/zaken/besluit-view/besluit-view.component.spec.ts",
  "src/app/zaken/zaak-link/zaak-link.component.spec.ts",
  "src/app/zaken/zaak-opschorten-dialog/zaak-opschorten-dialog.component.ts",
  "src/app/zaken/zaak-taken/zaak-taken.component.ts",
  "src/app/zaken/zaak-view/services/zaak-action-dialogs.service.ts",
  "src/app/zaken/zaak-view/zaak-details-card/zaak-details-algemeen-tab/zaak-details-algemeen-tab.component.spec.ts",
  "src/app/zaken/zaken-afgehandeld/zaken-afgehandeld.component.ts",
  "src/app/zaken/zaken-mijn/zaken-mijn.component.ts",
  "src/functions.spec.ts",
  "src/functions.ts",
  "src/test-helpers.ts",
];
