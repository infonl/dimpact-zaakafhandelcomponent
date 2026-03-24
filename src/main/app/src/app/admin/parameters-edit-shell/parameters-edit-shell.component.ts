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
import { ProcessModelMethodSelection } from "../model/parameters/process-model-method";

@Component({
  templateUrl: "./parameters-edit-shell.component.html",
  standalone: false,
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

  switchModellingMethod(selection: ProcessModelMethodSelection) {
    this.modellingMethodSelection = selection;
  }
}
