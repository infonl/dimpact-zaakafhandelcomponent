/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { BREADCRUMB_DETAIL } from "../core/breadcrumbs/route-title";
import { BAGResolverService } from "./bag-view/bag-resolver.service";
import { BAGViewComponent } from "./bag-view/bag-view.component";

export const BAG_ROUTES: Routes = [
  {
    path: ":type/:id",
    component: BAGViewComponent,
    resolve: { bagObject: BAGResolverService },
    data: BREADCRUMB_DETAIL,
  },
];
