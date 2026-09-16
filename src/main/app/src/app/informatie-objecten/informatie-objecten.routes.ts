/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { InformatieObjectViewComponent } from "./informatie-object-view/informatie-object-view.component";
import { InformatieObjectResolver } from "./informatie-object.resolver";

export const INFORMATIE_OBJECTEN_ROUTES: Routes = [
  {
    path: ":uuid",
    component: InformatieObjectViewComponent,
    resolve: { informatieObject: InformatieObjectResolver },
  },
  {
    path: ":uuid/:versie",
    component: InformatieObjectViewComponent,
    resolve: { informatieObject: InformatieObjectResolver },
  },
];
