/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { RouterModule } from "@angular/router";
import { SharedModule } from "../shared/shared.module";
import { DashboardComponent } from "./dashboard.component";
import { InformatieobjectenCardComponent } from "./informatieobjecten-card/informatieobjecten-card.component";
import { TaakZoekenCardComponent } from "./taak-zoeken-card/taak-zoeken-card.component";
import { TakenCardComponent } from "./taken-card/taken-card.component";
import { ZaakWaarschuwingenCardComponent } from "./zaak-waarschuwingen-card/zaak-waarschuwingen-card.component";
import { ZaakZoekenCardComponent } from "./zaak-zoeken-card/zaak-zoeken-card.component";
import { ZakenCardComponent } from "./zaken-card/zaken-card.component";

@NgModule({
  declarations: [
    DashboardComponent,
    InformatieobjectenCardComponent,
    TakenCardComponent,
    TaakZoekenCardComponent,
    ZakenCardComponent,
    ZaakWaarschuwingenCardComponent,
    ZaakZoekenCardComponent,
  ],
  exports: [DashboardComponent],
  imports: [CommonModule, SharedModule, RouterModule],
})
export class DashboardModule {}
