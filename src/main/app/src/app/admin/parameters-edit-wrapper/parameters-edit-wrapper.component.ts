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

@Component({
  templateUrl: "./parameters-edit-wrapper.component.html",
})
export class ParametersEditWrapperomponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  data: any;
  selected!: "CMMN" | "BPMN" | "PRISTINE";

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
      this.data = parameters;

      console.log("ParametersEditWrapperomponent data:", this.data);

      this.selected = "PRISTINE";
    });
  }

  switchProcessDefinition(to: "CMMN" | "BPMN" | "PRISTINE") {
    this.selected = to;
  }
}
