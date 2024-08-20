/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { FormioModule } from "@formio/angular";
import { FormulierenModule } from "../formulieren/formulieren.module";
import { GebruikersvoorkeurenModule } from "../gebruikersvoorkeuren/gebruikersvoorkeuren.module";
import { InformatieObjectenModule } from "../informatie-objecten/informatie-objecten.module";
import { MimetypeToExtensionPipe } from "../shared/pipes/mimetypeToExtension.pipe";
import { SharedModule } from "../shared/shared.module";
import { ZakenModule } from "../zaken/zaken.module";
import { ZoekenModule } from "../zoeken/zoeken.module";
import { TaakViewComponent } from "./taak-view/taak-view.component";
import { TakenMijnComponent } from "./taken-mijn/taken-mijn.component";
import { TakenRoutingModule } from "./taken-routing.module";
import { TakenVerdelenDialogComponent } from "./taken-verdelen-dialog/taken-verdelen-dialog.component";
import { TakenVrijgevenDialogComponent } from "./taken-vrijgeven-dialog/taken-vrijgeven-dialog.component";
import { TakenWerkvoorraadComponent } from "./taken-werkvoorraad/taken-werkvoorraad.component";

@NgModule({
  declarations: [
    TaakViewComponent,
    TakenWerkvoorraadComponent,
    TakenMijnComponent,
    TakenVerdelenDialogComponent,
    TakenVrijgevenDialogComponent,
  ],
  imports: [
    SharedModule,
    TakenRoutingModule,
    ZakenModule,
    InformatieObjectenModule,
    ZoekenModule,
    GebruikersvoorkeurenModule,
    FormulierenModule,
    MimetypeToExtensionPipe,
    FormioModule,
  ],
})
export class TakenModule {}
