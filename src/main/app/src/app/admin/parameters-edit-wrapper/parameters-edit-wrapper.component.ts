/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit, ViewChild } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { AdminComponent } from "../admin/admin.component";
import { UtilService } from "src/app/core/service/util.service";
import { ConfiguratieService } from "src/app/configuratie/configuratie.service";
import { ZaakProcessDefinition } from "../model/parameters/parameters-edit-process-definition-type";

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
      if (parameters.isBpmn) {
        this.processDefinitionType = { type: "BPMN" };
        return;
      }

      if (parameters.isSavedZaakafhandelparameters) {
        this.processDefinitionType = { type: "CMMN" };
        return;
      }

      this.processDefinitionType = { type: "SELECT-PROCESS-DEFINITION" };
    });
  }

  switchProcessDefinition(switchToDefinition: ZaakProcessDefinition) {
    this.processDefinitionType = switchToDefinition;
  }
}
