/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit, ViewChild } from "@angular/core";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { ConfiguratieService } from "src/app/configuratie/configuratie.service";
import { UtilService } from "src/app/core/service/util.service";
import { AdminComponent } from "../admin/admin.component";
import { ZaakProcessDefinition } from "../model/parameters/zaak-process-definition-type";

@Component({
  templateUrl: "./parameters-edit-wrapper.component.html",
})
export class ParametersEditWrapperComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  processDefinitionType!: ZaakProcessDefinition;

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
      if (!parameters.featureFlagBpmnSupport) {
        this.processDefinitionType = { type: "CMMN", selectedIndexStart: 1 };
        return;
      }

      if (parameters.isBpmn) {
        this.processDefinitionType = { type: "BPMN", selectedIndexStart: 1 };
        return;
      }

      if (parameters.isSavedZaakafhandelParameters) {
        this.processDefinitionType = { type: "CMMN", selectedIndexStart: 1 };
        return;
      }

      this.processDefinitionType = { type: "SELECT-PROCESS-DEFINITION" };
    });
  }

  switchProcessDefinition(switchToDefinition: ZaakProcessDefinition) {
    this.processDefinitionType = switchToDefinition;
  }
}
