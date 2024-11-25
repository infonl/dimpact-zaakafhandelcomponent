/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { NgxEditorModule } from "ngx-editor";
import { SharedModule } from "../shared/shared.module";
import { ZoekenModule } from "../zoeken/zoeken.module";
import { AdminRoutingModule } from "./admin-routing.module";
import { FormioFormulierenComponent } from "./formio-formulieren/formio-formulieren.component";
import { FormulierDefinitieEditComponent } from "./formulier-definitie-edit/formulier-definitie-edit.component";
import { TekstvlakEditDialogComponent } from "./formulier-definitie-edit/tekstvlak-edit-dialog/tekstvlak-edit-dialog.component";
import { FormulierDefinitiesComponent } from "./formulier-definities/formulier-definities.component";
import { GroepSignaleringenComponent } from "./groep-signaleringen/groep-signaleringen.component";
import { InrichtingscheckComponent } from "./inrichtingscheck/inrichtingscheck.component";
import { MailtemplateComponent } from "./mailtemplate/mailtemplate.component";
import { MailtemplatesComponent } from "./mailtemplates/mailtemplates.component";
import { ParameterEditComponent } from "./parameter-edit/parameter-edit.component";
import { SmartDocumentsFormItemComponent } from "./parameter-edit/smart-documents-form/smart-documents-form-item/smart-documents-form-item.component";
import { SmartDocumentsFormComponent } from "./parameter-edit/smart-documents-form/smart-documents-form.component";
import { ParametersComponent } from "./parameters/parameters.component";
import { ProcessDefinitionsComponent } from "./process-definitions/process-definitions.component";
import { ReferentieTabelComponent } from "./referentie-tabel/referentie-tabel.component";
import { ReferentieTabellenComponent } from "./referentie-tabellen/referentie-tabellen.component";

@NgModule({
  declarations: [
    GroepSignaleringenComponent,
    ParametersComponent,
    ParameterEditComponent,
    FormulierDefinitiesComponent,
    FormulierDefinitieEditComponent,
    ReferentieTabellenComponent,
    ReferentieTabelComponent,
    InrichtingscheckComponent,
    MailtemplatesComponent,
    MailtemplateComponent,
    TekstvlakEditDialogComponent,
    SmartDocumentsFormComponent,
    SmartDocumentsFormItemComponent,
    ProcessDefinitionsComponent,
    FormioFormulierenComponent,
  ],
  exports: [],
  imports: [SharedModule, ZoekenModule, AdminRoutingModule, NgxEditorModule],
})
export class AdminModule {}
