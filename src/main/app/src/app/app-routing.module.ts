/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { IdentityComponent } from "./identity/identity.component";

const routes: Routes = [
  {
    path: "",
    loadComponent: () =>
      import("./dashboard/dashboard.component").then(
        (m) => m.DashboardComponent,
      ),
  },
  { path: "gebruiker", component: IdentityComponent },
  {
    path: "persoon",
    loadChildren: () =>
      import("./klanten/klanten.routes").then(
        (module) => module.PERSOON_ROUTES,
      ),
  },
  {
    path: "bedrijf",
    loadChildren: () =>
      import("./klanten/klanten.routes").then(
        (module) => module.BEDRIJF_ROUTES,
      ),
  },
  {
    path: "zaken",
    loadChildren: () =>
      import("./zaken/zaken.routes").then((module) => module.ZAKEN_ROUTES),
  },
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
    path: "informatie-objecten",
    loadChildren: () =>
      import("./informatie-objecten/informatie-objecten.routes").then(
        (module) => module.INFORMATIE_OBJECTEN_ROUTES,
      ),
  },
  {
    path: "fout",
    loadChildren: () =>
      import("./fout-afhandeling/fout-afhandeling.routes").then(
        (module) => module.FOUT_AFHANDELING_ROUTES,
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
