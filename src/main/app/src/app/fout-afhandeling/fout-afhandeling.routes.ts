/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";

export const FOUT_AFHANDELING_ROUTES: Routes = [
  {
    path: "",
    loadComponent: () =>
      import("./fout-afhandeling.component").then(
        (module) => module.FoutAfhandelingComponent,
      ),
  },
];
