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
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
