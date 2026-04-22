/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { DashboardComponent } from "./dashboard/dashboard.component";
import { IdentityComponent } from "./identity/identity.component";

const routes: Routes = [
  { path: "", component: DashboardComponent },
  { path: "gebruiker", component: IdentityComponent },
  {
    path: "taken",
    loadChildren: () =>
      import("./taken/taken.module").then((module) => module.TakenModule),
  },
  {
    path: "admin",
    loadChildren: () =>
      import("./admin/admin.routes").then((module) => module.ADMIN_ROUTES),
  },
  {
    path: "bag-objecten",
    loadChildren: () =>
      import("./bag/bag.routes").then((module) => module.BAG_ROUTES),
  },
  {
    path: "signaleringen",
    loadChildren: () =>
      import("./signaleringen/signaleringen.routes").then(
        (module) => module.SIGNALERINGEN_ROUTES,
      ),
  },
  {
    path: "documenten",
    loadChildren: () =>
      import("./documenten/documenten-routing.module").then(
        (module) => module.DocumentenRoutingModule,
      ),
  },
  {
    path: "productaanvragen",
    loadChildren: () =>
      import("./productaanvragen/productaanvragen-routing.module").then(
        (module) => module.ProductaanvragenRoutingModule,
      ),
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
