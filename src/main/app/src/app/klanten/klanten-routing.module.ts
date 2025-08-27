/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { BedrijfResolverService } from "./bedrijf-view/bedrijf-resolver.service";
import { BedrijfViewComponent } from "./bedrijf-view/bedrijf-view.component";
import { PersoonResolverService } from "./persoon-view/persoon-resolver.service";
import { PersoonViewComponent } from "./persoon-view/persoon-view.component";

const routes: Routes = [
  {
    path: "persoon",
    children: [
      {
        path: ":bsn",
        component: PersoonViewComponent,
        resolve: { persoon: PersoonResolverService },
      },
    ],
  },
  {
    path: "bedrijf/kvkvestigingsnummer",
    children: [
      {
        path: ":kvk/:vestigingsnummer",
        component: BedrijfViewComponent,
        resolve: { bedrijf: BedrijfResolverService },
        data: { lookupType: "kvkvestigingsnummer" },
      },
    ],
  },
  {
    path: "bedrijf/kvk",
    children: [
      {
        path: ":kvk",
        component: BedrijfViewComponent,
        resolve: { bedrijf: BedrijfResolverService },
        data: { lookupType: "kvk" },
      },
    ],
  },
  {
    path: "bedrijf/vestigingsnummer",
    children: [
      {
        path: ":vestigingsnummer",
        component: BedrijfViewComponent,
        resolve: { bedrijf: BedrijfResolverService },
        data: { lookupType: "vestigingsnummer" },
      },
    ],
  },
  {
    path: "bedrijf/rsin",
    children: [
      {
        path: ":rsin",
        component: BedrijfViewComponent,
        resolve: { bedrijf: BedrijfResolverService },
        data: { lookupType: "rsin" },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class KlantenRoutingModule {}
