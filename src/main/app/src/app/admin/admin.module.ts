/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { NgxEditorModule } from "ngx-editor";
import { SharedModule } from "../shared/shared.module";
import { ZoekenModule } from "../zoeken/zoeken.module";
import { AdminRoutingModule } from "./admin-routing.module";
import { FormulierDefinitieEditComponent } from "./formulier-definitie-edit/formulier-definitie-edit.component";
import { TekstvlakEditDialogComponent } from "./formulier-definitie-edit/tekstvlak-edit-dialog/tekstvlak-edit-dialog.component";
import { FormulierDefinitiesComponent } from "./formulier-definities/formulier-definities.component";
import { InrichtingscheckComponent } from "./inrichtingscheck/inrichtingscheck.component";
import { MailtemplateComponent } from "./mailtemplate/mailtemplate.component";
import { MailtemplatesComponent } from "./mailtemplates/mailtemplates.component";
import { ParametersEditBpmnComponent } from "./parameters-edit-bpmn/parameters-edit-bpmn.component";
import { ParametersEditCmmnComponent } from "./parameters-edit-cmmn/parameters-edit-cmmn.component";
import { ParameterEditSelectProcessDefinitionComponent } from "./parameters-edit-select-process-definition/parameters-edit-select-process-definition.component";
import { ParametersEditWrapperComponent } from "./parameters-edit-wrapper/parameters-edit-wrapper.component";
import { ParametersComponent } from "./parameters/parameters.component";
import { ProcessDefinitionsComponent } from "./process-definitions/process-definitions.component";
import { ReferentieTabelComponent } from "./referentie-tabel/referentie-tabel.component";
import { SmartDocumentsFormItemComponent } from "./parameters-edit-cmmn/smart-documents-form/smart-documents-form-item/smart-documents-form-item.component";
import { SmartDocumentsFormComponent } from "./parameters-edit-cmmn/smart-documents-form/smart-documents-form.component";

@NgModule({
  declarations: [
    ParametersEditWrapperComponent,
    ParametersComponent,
    ParameterEditSelectProcessDefinitionComponent,
    ParametersEditBpmnComponent,
    FormulierDefinitiesComponent,
    FormulierDefinitieEditComponent,
    ReferentieTabelComponent,
    InrichtingscheckComponent,
    MailtemplatesComponent,
    MailtemplateComponent,
    TekstvlakEditDialogComponent,
  ],
  exports: [],
  imports: [
    SharedModule,
    ZoekenModule,
    AdminRoutingModule,
    NgxEditorModule,
    ParametersEditCmmnComponent,
    SmartDocumentsFormComponent,
    SmartDocumentsFormItemComponent,
    ProcessDefinitionsComponent,
  ],
})
export class AdminModule {}
