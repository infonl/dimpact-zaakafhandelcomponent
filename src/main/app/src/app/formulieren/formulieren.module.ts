/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { FormioModule } from "@formio/angular";
import { DocumentIconComponent } from "../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { SharedModule } from "../shared/shared.module";
import { FormioWrapperComponent } from "./formio-wrapper/formio-wrapper.component";
import { FormulierComponent } from "./formulier/formulier.component";
import { DocumentenFormulierVeldComponent } from "./formulier/velden/documenten/documenten-formulier-veld.component";

@NgModule({
  declarations: [
    FormulierComponent,
    DocumentenFormulierVeldComponent,
    FormioWrapperComponent,
  ],
  exports: [FormulierComponent, FormioWrapperComponent],
  imports: [
    SharedModule,
    DocumentIconComponent,
    InformatieObjectIndicatiesComponent,
    FormioModule,
  ],
})
export class FormulierenModule {}
