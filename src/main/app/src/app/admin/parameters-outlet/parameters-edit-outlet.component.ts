/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Injector, OnInit, Type, ViewChild } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ParameterEditComponent } from "../parameter-edit/parameter-edit.component";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { AdminComponent } from "../admin/admin.component";
import { UtilService } from "src/app/core/service/util.service";
import { ConfiguratieService } from "src/app/configuratie/configuratie.service";
import { ParameterEditBpmnComponent } from "../parameter-edit-bpmn/parameter-edit-bpmn.component";

@Component({
  selector: "app-parameters-edit-outlet",
  templateUrl: "./parameters-edit-outlet.component.html",
})
export class ParametersOutletComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  data: any;
  selected!: number;
  component!: Type<any>;
  componentInjector!: Injector;

  constructor(
    public readonly utilService: UtilService,
    public readonly configuratieService: ConfiguratieService,

    private route: ActivatedRoute,
    private injector: Injector,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.setupMenu("title.parameters.wijzigen");

    this.route.data.subscribe(({ parameters }) => {
      this.data = parameters;

      console.log("ProcessOutletComponent data:", this.data);
      //   this.selected = process.selected;
      this.selected = 2;
      this.loadComponent(this.selected);
    });
  }

  loadComponent(selected: number) {
    // pick component class dynamically
    switch (selected) {
      case 1:
        this.component = ParameterEditComponent;
        break;
      case 2:
        this.component = ParameterEditBpmnComponent;
        break;
      case 3:
        this.component = ParameterEditComponent;
        break;
    }

    // create injector to pass inputs
    this.componentInjector = Injector.create({
      providers: [
        { provide: "processData", useValue: this.data },
        { provide: "switchFn", useValue: (n: number) => this.switch(n) },
      ],
      parent: this.injector,
    });
  }

  switch(to: number) {
    this.selected = to;
    this.loadComponent(to);
  }
}
