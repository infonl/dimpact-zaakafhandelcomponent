/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { RouterLink } from "@angular/router";
import { ContactmomentenModule } from "../contactmomenten/contactmomenten.module";
import { SharedModule } from "../shared/shared.module";
import { BedrijfViewComponent } from "./bedrijf-view/bedrijf-view.component";
import { BedrijfsgegevensComponent } from "./bedrijfsgegevens/bedrijfsgegevens.component";
import { KlantZakenTabelComponent } from "./klant-zaken-tabel/klant-zaken-tabel.component";
import { KlantenRoutingModule } from "./klanten-routing.module";
import { PersoonViewComponent } from "./persoon-view/persoon-view.component";
import { PersoonsgegevensComponent } from "./persoonsgegevens/persoonsgegevens.component";
import { BedrijfZoekComponent } from "./zoek/bedrijven/bedrijf-zoek.component";
import { KlantZoekComponent } from "./zoek/klanten/klant-zoek.component";
import { PersoonZoekComponent } from "./zoek/personen/persoon-zoek.component";

@NgModule({
  declarations: [
    BedrijfZoekComponent,
    BedrijfsgegevensComponent,
    PersoonZoekComponent,
    PersoonsgegevensComponent,
    KlantZakenTabelComponent,
    KlantZoekComponent,
    PersoonViewComponent,
    BedrijfViewComponent,
  ],
  exports: [
    BedrijfZoekComponent,
    BedrijfsgegevensComponent,
    PersoonZoekComponent,
    PersoonsgegevensComponent,
    KlantZoekComponent,
  ],
  imports: [
    SharedModule,
    RouterLink,
    KlantenRoutingModule,
    ContactmomentenModule,
  ],
})
export class KlantenModule {}
