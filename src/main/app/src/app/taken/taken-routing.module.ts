/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { TabelGegevensResolver } from "../shared/dynamic-table/datasource/tabel-gegevens-resolver.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { TaakResolver } from "./taak.resolver";

const routes: Routes = [
  {
    path: "",
    redirectTo: "werkvoorraad",
    pathMatch: "full",
  },
  {
    path: "werkvoorraad",
    loadComponent: () =>
      import("./taken-werkvoorraad/taken-werkvoorraad.component").then(
        (m) => m.TakenWerkvoorraadComponent,
      ),
    resolve: { tabelGegevens: TabelGegevensResolver },
    data: {
      werklijst: "WERKVOORRAAD_TAKEN" satisfies GeneratedType<"Werklijst">,
    },
  },
  {
    path: "mijn",
    loadComponent: () =>
      import("./taken-mijn/taken-mijn.component").then(
        (m) => m.TakenMijnComponent,
      ),
    resolve: { tabelGegevens: TabelGegevensResolver },
    data: { werklijst: "MIJN_TAKEN" satisfies GeneratedType<"Werklijst"> },
  },
  {
    path: ":id",
    loadComponent: () =>
      import("./taak-view/taak-view.component").then(
        (m) => m.TaakViewComponent,
      ),
    resolve: { taak: TaakResolver },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class TakenRoutingModule {}
