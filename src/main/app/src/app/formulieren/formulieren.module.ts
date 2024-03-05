/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { DocumentIconComponent } from "../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { SharedModule } from "../shared/shared.module";
import { FormulierComponent } from "./formulier/formulier.component";
import { DocumentenFormulierVeldComponent } from "./formulier/velden/documenten/documenten-formulier-veld.component";

@NgModule({
  declarations: [FormulierComponent, DocumentenFormulierVeldComponent],
  exports: [FormulierComponent],
  imports: [
    SharedModule,
    DocumentIconComponent,
    InformatieObjectIndicatiesComponent,
  ],
})
export class FormulierenModule {}
