/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgSwitch, NgSwitchCase, NgSwitchDefault } from "@angular/common";
import { Component, OnInit, ViewChild } from "@angular/core";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { ConfiguratieService } from "src/app/configuratie/configuratie.service";
import { UtilService } from "src/app/core/service/util.service";
import { SideNavComponent } from "src/app/shared/side-nav/side-nav.component";
import { AdminComponent } from "../admin/admin.component";
import { ProcessModelMethodSelection } from "../model/parameters/process-model-method";
import { ParametersEditBpmnComponent } from "../parameters-edit-bpmn/parameters-edit-bpmn.component";
import { ParametersEditCmmnComponent } from "../parameters-edit-cmmn/parameters-edit-cmmn.component";
import { ParameterSelectProcessModelMethodComponent } from "../parameters-select-process-model-method/parameters-select-process-model-method.component";

@Component({
  templateUrl: "./parameters-edit-shell.component.html",
  standalone: true,
  imports: [
    NgSwitch,
    NgSwitchCase,
    NgSwitchDefault,
    MatSidenavModule,
    MatProgressSpinnerModule,
    SideNavComponent,
    ParameterSelectProcessModelMethodComponent,
    ParametersEditCmmnComponent,
    ParametersEditBpmnComponent,
  ],
})
export class ParametersEditShellComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  protected isLoading: boolean = true; // wait for data since mat-stepper cannot load its steps dynamically
  protected modellingMethodSelection!: ProcessModelMethodSelection;

  constructor(
    public readonly utilService: UtilService,
    public readonly configuratieService: ConfiguratieService,
    private route: ActivatedRoute,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.setupMenu("title.parameters.wijzigen");

    this.route.data.subscribe(({ parameters }) => {
      this.isLoading = false;

      if (parameters.isBpmn) {
        this.modellingMethodSelection = { type: "BPMN", selectedIndexStart: 1 };
        return;
      }

      if (parameters.isSavedZaakafhandelParameters) {
        this.modellingMethodSelection = { type: "CMMN", selectedIndexStart: 1 };
        return;
      }

      this.modellingMethodSelection = { type: null };
    });
  }

  protected switchModellingMethod(selection: ProcessModelMethodSelection) {
    this.modellingMethodSelection = selection;
  }
}
