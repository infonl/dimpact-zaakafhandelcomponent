/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { FormioWrapperComponent } from "../formulieren/formio-wrapper/formio-wrapper.component";
import { InformatieObjectenModule } from "../informatie-objecten/informatie-objecten.module";
import { MimetypeToExtensionPipe } from "../shared/pipes/mimetypeToExtension.pipe";
import { SharedModule } from "../shared/shared.module";
import { ZakenModule } from "../zaken/zaken.module";
import { TaakEditComponent } from "./taak-edit/taak-edit.component";
import { TaakViewComponent } from "./taak-view/taak-view.component";
import { TakenRoutingModule } from "./taken-routing.module";

@NgModule({
  declarations: [TaakViewComponent],
  imports: [
    SharedModule,
    TakenRoutingModule,
    ZakenModule,
    InformatieObjectenModule,
    FormioWrapperComponent,
    MimetypeToExtensionPipe,
    TaakEditComponent,
  ],
})
export class TakenModule {}
