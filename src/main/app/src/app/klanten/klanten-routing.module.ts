/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { ErrorCardComponent } from "../fout-afhandeling/error-card/error-card.component";
import { GeneratedType } from "../shared/utils/generated-types";
import { BedrijfResolverService } from "./bedrijf-view/bedrijf-resolver.service";
import { BedrijfViewComponent } from "./bedrijf-view/bedrijf-view.component";
import { PersoonResolverService } from "./persoon-view/persoon-resolver.service";
import { PersoonViewComponent } from "./persoon-view/persoon-view.component";
import { validateBsnGuard } from "./persoon-view/validate-bsn.guard";

const routes: Routes = [
  {
    path: "persoon",
    children: [
      {
        path: "fout",
        component: ErrorCardComponent,
        data: {
          title: "error-card.persoon.title.geen-data",
          text: "error-card.persoon.text.geen-data",
        },
      },
      {
        path: "",
        canActivate: [validateBsnGuard],
        component: PersoonViewComponent,
        resolve: { persoon: PersoonResolverService },
      },
    ],
  },
  {
    path: "bedrijf",
    children: [
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
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class KlantenRoutingModule {}

export function buildBedrijfRouteLink(
  bedrijf?: GeneratedType<"RestBedrijf"> | null,
) {
  const path = ["/bedrijf", bedrijf?.kvkNummer ?? bedrijf?.identificatie]; // use `identificatie` to support legacy
  if (bedrijf?.vestigingsnummer)
    path.push("vestiging", bedrijf?.vestigingsnummer);
  else if (bedrijf?.kvkNummer && bedrijf?.identificatie)
    path.push("vestiging", bedrijf?.identificatie);
  return path;
}
