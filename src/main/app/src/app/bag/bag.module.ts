/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { SharedModule } from "../shared/shared.module";
import { BagLocatieComponent } from "./bag-locatie/bag-locatie.component";
import { BAGRoutingModule } from "./bag-routing.module";
import { BAGViewComponent } from "./bag-view/bag-view.component";
import { BagZakenTabelComponent } from "./bag-zaken-tabel/bag-zaken-tabel.component";

@NgModule({
  declarations: [BAGViewComponent],
  imports: [BAGRoutingModule, SharedModule, BagLocatieComponent, BagZakenTabelComponent],
})
export class BAGModule {}
