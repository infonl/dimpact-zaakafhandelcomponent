/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { TabelGegevensResolver } from "../shared/dynamic-table/datasource/tabel-gegevens-resolver.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZaakIdentificatieResolver } from "./zaak-identificatie-resolver.service";
import { ZaakViewComponent } from "./zaak-view/zaak-view.component";

const routes: Routes = [
  {
    path: "zaken",
    children: [
      {
        path: "",
        redirectTo: "werkvoorraad",
        pathMatch: "full",
      },
      {
        path: "mijn",
        loadComponent: () =>
          import("./zaken-mijn/zaken-mijn.component").then(
            (m) => m.ZakenMijnComponent,
          ),
        resolve: { tabelGegevens: TabelGegevensResolver },
        data: { werklijst: "MIJN_ZAKEN" satisfies GeneratedType<"Werklijst"> },
      },
      {
        path: "werkvoorraad",
        loadComponent: () =>
          import("./zaken-werkvoorraad/zaken-werkvoorraad.component").then(
            (m) => m.ZakenWerkvoorraadComponent,
          ),
        resolve: { tabelGegevens: TabelGegevensResolver },
        data: {
          werklijst: "WERKVOORRAAD_ZAKEN" satisfies GeneratedType<"Werklijst">,
        },
      },
      {
        path: "create",
        loadComponent: () =>
          import("./zaak-create/zaak-create.component").then(
            (m) => m.ZaakCreateComponent,
          ),
      },
      {
        path: "afgehandeld",
        loadComponent: () =>
          import("./zaken-afgehandeld/zaken-afgehandeld.component").then(
            (m) => m.ZakenAfgehandeldComponent,
          ),
        resolve: { tabelGegevens: TabelGegevensResolver },
        data: {
          werklijst: "AFGEHANDELDE_ZAKEN" satisfies GeneratedType<"Werklijst">,
        },
      },
      {
        path: ":zaakIdentificatie",
        component: ZaakViewComponent,
        resolve: { zaak: ZaakIdentificatieResolver },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ZakenRoutingModule {}
