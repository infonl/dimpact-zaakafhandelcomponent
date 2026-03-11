/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, ElementRef, OnInit, ViewChild } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { BpmnService } from "../bpmn.service";

@Component({
  templateUrl: "./formio-formulieren.component.html",
  styleUrls: ["./formio-formulieren.component.less"],
  standalone: false,
})
export class FormioFormulierenComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("fileInput", { static: false }) fileInput!: ElementRef;

  isLoadingResults = false;
  columns: string[] = ["name", "title", "bpmnProcessDefinition", "id"];
  dataSource = new MatTableDataSource<GeneratedType<"RestFormioFormulier">>();

  constructor(
    public readonly dialog: MatDialog,
    public readonly utilService: UtilService,
    public readonly configuratieService: ConfiguratieService,
    private readonly bpmService: BpmnService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.setupMenu("title.formioformulieren");
    this.loadFormioFormulieren();
  }

  protected delete(formioFormulier: GeneratedType<"RestFormioFormulier">) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.formioformulier.verwijderen.bevestigen",
            args: { naam: formioFormulier.name },
          },
          this.bpmService.deleteProcessDefinitionForm(
            formioFormulier.bpmnProcessDefinition!,
            formioFormulier.name!,
          ),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar(
            "msg.formioformulier.verwijderen.uitgevoerd",
            { naam: formioFormulier.name },
          );
          this.loadFormioFormulieren();
        }
      });
  }

  private loadFormioFormulieren() {
    this.isLoadingResults = true;
    this.utilService.setLoading(true);
    this.bpmService.listFormioFormulieren().subscribe((formioFormulier) => {
      this.dataSource.data = formioFormulier;
      this.isLoadingResults = false;
      this.utilService.setLoading(false);
    });
  }
}
