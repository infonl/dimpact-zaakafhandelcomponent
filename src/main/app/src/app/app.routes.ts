/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import {
  BREADCRUMB_ORIGIN,
  translatedTitle,
} from "./core/breadcrumbs/route-title";

export const APP_ROUTES: Routes = [
  {
    path: "",
    title: translatedTitle("title.dashboard"),
    data: BREADCRUMB_ORIGIN,
    loadComponent: () =>
      import("./dashboard/dashboard.component").then(
        (m) => m.DashboardComponent,
      ),
  },
  {
    path: "gebruiker",
    loadComponent: () =>
      import("./identity/identity.component").then(
        (module) => module.IdentityComponent,
      ),
  },
  {
    path: "persoon",
    title: translatedTitle("persoonsgegevens"),
    loadChildren: () =>
      import("./klanten/klanten.routes").then(
        (module) => module.PERSOON_ROUTES,
      ),
  },
  {
    path: "bedrijf",
    title: translatedTitle("bedrijfsgegevens"),
    loadChildren: () =>
      import("./klanten/klanten.routes").then(
        (module) => module.BEDRIJF_ROUTES,
      ),
  },
  {
    path: "zaken",
    title: translatedTitle("zaken"),
    loadChildren: () =>
      import("./zaken/zaken.routes").then((module) => module.ZAKEN_ROUTES),
  },
  {
    path: "taken",
    title: translatedTitle("taken"),
    loadChildren: () =>
      import("./taken/taken.routes").then((module) => module.TAKEN_ROUTES),
  },
  {
    path: "admin",
    title: translatedTitle("actie.admin"),
    loadChildren: () =>
      import("./admin/admin.routes").then((module) => module.ADMIN_ROUTES),
  },
  {
    path: "bag-objecten",
    title: translatedTitle("bagobjectgegevens"),
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
    title: translatedTitle("documenten"),
    loadChildren: () =>
      import("./documenten/documenten.routes").then(
        (module) => module.DOCUMENTEN_ROUTES,
      ),
  },
  {
    path: "informatie-objecten",
    title: translatedTitle("documenten"),
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
      import("./productaanvragen/productaanvragen.routes").then(
        (module) => module.PRODUCTAANVRAGEN_ROUTES,
      ),
  },
];
