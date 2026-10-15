/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ResolveFn, Routes } from "@angular/router";
import { BREADCRUMB_DETAIL } from "../core/breadcrumbs/route-title";
import { GeneratedType } from "../shared/utils/generated-types";
import { InformatieObjectViewComponent } from "./informatie-object-view/informatie-object-view.component";
import { InformatieObjectResolver } from "./informatie-object.resolver";

const title: ResolveFn<string> = (route) =>
  (
    route.data[
      "informatieObject"
    ] as GeneratedType<"RestEnkelvoudigInformatieobject">
  ).identificatie ?? "";

export const INFORMATIE_OBJECTEN_ROUTES: Routes = [
  { path: "", redirectTo: "/documenten/inbox", pathMatch: "full" },
  {
    path: ":uuid",
    resolve: { informatieObject: InformatieObjectResolver },
    children: [
      {
        path: "",
        title,
        data: BREADCRUMB_DETAIL,
        component: InformatieObjectViewComponent,
      },
    ],
  },
  {
    path: ":uuid/:versie",
    resolve: { informatieObject: InformatieObjectResolver },
    children: [
      {
        path: "",
        title,
        data: BREADCRUMB_DETAIL,
        component: InformatieObjectViewComponent,
      },
    ],
  },
];
