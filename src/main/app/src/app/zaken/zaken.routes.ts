/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import {
  BREADCRUMB_DETAIL,
  BREADCRUMB_ORIGIN,
  translatedTitle,
} from "../core/breadcrumbs/route-title";
import { TabelGegevensResolver } from "../shared/dynamic-table/datasource/tabel-gegevens-resolver.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZaakIdentificatieResolver } from "./zaak-identificatie-resolver.service";

export const ZAKEN_ROUTES: Routes = [
  {
    path: "",
    redirectTo: "werkvoorraad",
    pathMatch: "full",
  },
  {
    path: "mijn",
    title: translatedTitle("title.zaken.mijn"),
    loadComponent: () =>
      import("./zaken-mijn/zaken-mijn.component").then(
        (m) => m.ZakenMijnComponent,
      ),
    resolve: { tabelGegevens: TabelGegevensResolver },
    data: {
      werklijst: "MIJN_ZAKEN" satisfies GeneratedType<"Werklijst">,
      ...BREADCRUMB_ORIGIN,
    },
  },
  {
    path: "werkvoorraad",
    title: translatedTitle("title.zaken.werkvoorraad"),
    loadComponent: () =>
      import("./zaken-werkvoorraad/zaken-werkvoorraad.component").then(
        (m) => m.ZakenWerkvoorraadComponent,
      ),
    resolve: { tabelGegevens: TabelGegevensResolver },
    data: {
      werklijst: "WERKVOORRAAD_ZAKEN" satisfies GeneratedType<"Werklijst">,
      ...BREADCRUMB_ORIGIN,
    },
  },
  {
    path: "create",
    title: translatedTitle("title.zaak.aanmaken"),
    loadComponent: () =>
      import("./zaak-create/zaak-create.component").then(
        (m) => m.ZaakCreateComponent,
      ),
  },
  {
    path: "afgehandeld",
    title: translatedTitle("title.zaken.afgehandeld"),
    loadComponent: () =>
      import("./zaken-afgehandeld/zaken-afgehandeld.component").then(
        (m) => m.ZakenAfgehandeldComponent,
      ),
    resolve: { tabelGegevens: TabelGegevensResolver },
    data: {
      werklijst: "AFGEHANDELDE_ZAKEN" satisfies GeneratedType<"Werklijst">,
      ...BREADCRUMB_ORIGIN,
    },
  },
  {
    path: ":zaakIdentificatie",
    title: (route) => route.paramMap.get("zaakIdentificatie") ?? "",
    loadComponent: () =>
      import("./zaak-view/zaak-view.component").then(
        (m) => m.ZaakViewComponent,
      ),
    resolve: { zaak: ZaakIdentificatieResolver },
    data: BREADCRUMB_DETAIL,
  },
];
