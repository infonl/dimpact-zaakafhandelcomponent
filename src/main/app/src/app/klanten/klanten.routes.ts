/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Routes } from "@angular/router";
import { ErrorCardComponent } from "../fout-afhandeling/error-card/error-card.component";
import { BedrijfResolverService } from "./bedrijf-view/bedrijf-resolver.service";
import { BedrijfViewComponent } from "./bedrijf-view/bedrijf-view.component";
import { PersoonResolverGuard } from "./persoon-view/persoon-resolver-guard";
import { PersoonResolverService } from "./persoon-view/persoon-resolver.service";
import { PersoonViewComponent } from "./persoon-view/persoon-view.component";

const PERSOON_GEEN_DATA = {
  title: "error-card.persoon.title.geen-data",
  text: "error-card.persoon.text.geen-data",
  iconName: "person_off",
};

export const PERSOON_ROUTES: Routes = [
  {
    path: ":temporaryPersonId",
    canMatch: [PersoonResolverGuard],
    component: PersoonViewComponent,
    resolve: { persoon: PersoonResolverService },
  },
  {
    path: ":temporaryPersonId",
    component: ErrorCardComponent,
    data: PERSOON_GEEN_DATA,
  },
  {
    path: "",
    component: ErrorCardComponent,
    data: PERSOON_GEEN_DATA,
  },
];

export const BEDRIJF_ROUTES: Routes = [
  {
    path: ":id", // This can only be a valid `RSIN` or `KVK` number
    component: BedrijfViewComponent,
    resolve: { bedrijf: BedrijfResolverService },
  },
  {
    path: ":id/vestiging/:vestigingsnummer", // `id` must be a `kvkNummer`
    component: BedrijfViewComponent,
    resolve: { bedrijf: BedrijfResolverService },
  },
];
