/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { TabelGegevensResolver } from "../shared/dynamic-table/datasource/tabel-gegevens-resolver.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZaakCreateComponent } from "./zaak-create/zaak-create.component";
import { ZaakIdentificatieResolver } from "./zaak-identificatie-resolver.service";
import { ZaakViewComponent } from "./zaak-view/zaak-view.component";
import { ZakenAfgehandeldComponent } from "./zaken-afgehandeld/zaken-afgehandeld.component";
import { ZakenMijnComponent } from "./zaken-mijn/zaken-mijn.component";
import { ZakenWerkvoorraadComponent } from "./zaken-werkvoorraad/zaken-werkvoorraad.component";

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
        component: ZakenMijnComponent,
        resolve: { tabelGegevens: TabelGegevensResolver },
        data: { werklijst: "MIJN_ZAKEN" satisfies GeneratedType<"Werklijst"> },
      },
      {
        path: "werkvoorraad",
        component: ZakenWerkvoorraadComponent,
        resolve: { tabelGegevens: TabelGegevensResolver },
        data: {
          werklijst: "WERKVOORRAAD_ZAKEN" satisfies GeneratedType<"Werklijst">,
        },
      },
      { path: "create", component: ZaakCreateComponent },
      {
        path: "afgehandeld",
        component: ZakenAfgehandeldComponent,
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
