/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { InformatieObjectResolver } from "./informatie-object.resolver";

const loadInformatieObjectViewComponent = () =>
  import("./informatie-object-view/informatie-object-view.component").then(
    (module) => module.InformatieObjectViewComponent,
  );

export const INFORMATIE_OBJECTEN_ROUTES: Routes = [
  {
    path: ":uuid",
    loadComponent: loadInformatieObjectViewComponent,
    resolve: { informatieObject: InformatieObjectResolver },
  },
  {
    path: ":uuid/:versie",
    loadComponent: loadInformatieObjectViewComponent,
    resolve: { informatieObject: InformatieObjectResolver },
  },
];
