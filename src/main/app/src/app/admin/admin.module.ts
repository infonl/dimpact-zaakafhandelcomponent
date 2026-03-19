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

import { GroepSignaleringenComponent } from "./groep-signaleringen/groep-signaleringen.component";
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
import { ReferentieTabellenComponent } from "./referentie-tabellen/referentie-tabellen.component";

@NgModule({
  declarations: [
    GroepSignaleringenComponent,
    ParametersEditWrapperComponent,
    ParametersComponent,
    ParameterEditSelectProcessDefinitionComponent,
    ParametersEditBpmnComponent,
    ReferentieTabellenComponent,
    ReferentieTabelComponent,
    InrichtingscheckComponent,
    MailtemplatesComponent,
    MailtemplateComponent,
    FormioFormulierenComponent,
    ProcessDefinitionsComponent,
  ],
  exports: [],
  imports: [
    SharedModule,
    ZoekenModule,
    AdminRoutingModule,
    NgxEditorModule,
    ParametersEditCmmnComponent,
  ],
})
export class AdminModule {}
