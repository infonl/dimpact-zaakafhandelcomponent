/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { MatSortModule } from "@angular/material/sort";
import { NgxEditorModule } from "ngx-editor";
import { SharedModule } from "../shared/shared.module";
import { ZoekenModule } from "../zoeken/zoeken.module";
import { AdminRoutingModule } from "./admin-routing.module";
import { BpmnProcessDefinitionsComponent } from "./bpmn-process-definitions/bpmn-process-definitions.component";
import { ParametersEditBpmnComponent } from "./parameters-edit-bpmn/parameters-edit-bpmn.component";
import { ParametersEditCmmnComponent } from "./parameters-edit-cmmn/parameters-edit-cmmn.component";
import { SmartDocumentsFormItemComponent } from "./parameters-edit-cmmn/smart-documents-form/smart-documents-form-item/smart-documents-form-item.component";
import { SmartDocumentsFormComponent } from "./parameters-edit-cmmn/smart-documents-form/smart-documents-form.component";
import { ParametersEditShellComponent } from "./parameters-edit-shell/parameters-edit-shell.component";
import { ParameterSelectProcessModelMethodComponent } from "./parameters-select-process-model-method/parameters-select-process-model-method.component";
import { ParametersComponent } from "./parameters/parameters.component";

@NgModule({
  declarations: [
    ParametersEditShellComponent,
    ParametersComponent,
    ParameterSelectProcessModelMethodComponent,
    ParametersEditBpmnComponent,
  ],
  exports: [],
  imports: [
    SharedModule,
    ZoekenModule,
    AdminRoutingModule,
    NgxEditorModule,
    MatSortModule,
    ParametersEditCmmnComponent,
    SmartDocumentsFormComponent,
    SmartDocumentsFormItemComponent,
    BpmnProcessDefinitionsComponent,
  ],
})
export class AdminModule {}
