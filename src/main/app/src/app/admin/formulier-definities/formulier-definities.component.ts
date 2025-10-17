/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit, ViewChild } from "@angular/core";
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
import { FormulierDefinitieService } from "../formulier-defintie.service";

@Component({
  templateUrl: "./formulier-definities.component.html",
  styleUrls: ["./formulier-definities.component.less"],
})
export class FormulierDefinitiesComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  isLoadingResults = false;
  columns = [
    "systeemnaam",
    "naam",
    "beschrijving",
    "creatiedatum",
    "wijzigingsdatum",
    "aantal",
    "id",
  ] as const;
  dataSource = new MatTableDataSource<
    GeneratedType<"RESTFormulierDefinitie">
  >();

  constructor(
    public dialog: MatDialog,
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private service: FormulierDefinitieService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.setupMenu("title.formulierdefinities");
    this.ophalenFormulierDefinities();
  }

  ophalenFormulierDefinities() {
    this.isLoadingResults = true;
    this.utilService.setLoading(true);
    this.service.list().subscribe((definities) => {
      this.dataSource.data = definities;
      this.isLoadingResults = false;
      this.utilService.setLoading(false);
    });
  }

  verwijderen(formulierDefinitie: GeneratedType<"RESTFormulierDefinitie">) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.formulierdefinitie.verwijderen.bevestigen",
            args: { naam: formulierDefinitie.systeemnaam },
          },
          this.service.delete(formulierDefinitie.id!),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (!result) return;
        this.utilService.openSnackbar(
          "msg.formulierdefinitie.verwijderen.uitgevoerd",
          { naam: formulierDefinitie.systeemnaam },
        );
        this.ophalenFormulierDefinities();
      });
  }
}
