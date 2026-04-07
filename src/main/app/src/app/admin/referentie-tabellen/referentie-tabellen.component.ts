/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass, NgIf } from "@angular/common";
import { Component, OnInit, ViewChild } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { ReferentieTabelService } from "../referentie-tabel.service";

@Component({
  templateUrl: "./referentie-tabellen.component.html",
  styleUrls: ["./referentie-tabellen.component.less"],
  standalone: true,
  imports: [
    NgClass,
    NgIf,
    MatSidenavModule,
    MatCardModule,
    MatDialogModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    RouterModule,
    SideNavComponent,
    TranslateModule,
  ],
})
export class ReferentieTabellenComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer")
  protected sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") protected menuSidenav!: MatSidenav;

  protected isLoadingResults = false;
  protected columns: string[] = ["code", "systeem", "naam", "waarden", "id"];
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestReferenceTable">
  >();

  constructor(
    public dialog: MatDialog,
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private service: ReferentieTabelService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit(): void {
    this.setupMenu("title.referentietabellen");
    this.laadReferentieTabellen();
  }

  protected laadReferentieTabellen(): void {
    this.isLoadingResults = true;
    this.service.listReferentieTabellen().subscribe((tabellen) => {
      this.dataSource.data = tabellen;
      this.isLoadingResults = false;
    });
  }

  protected verwijderReferentieTabel(
    referentieTabel: GeneratedType<"RestReferenceTable">,
  ): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.tabel.verwijderen.bevestigen",
            args: { tabel: referentieTabel.code },
          },
          this.service.deleteReferentieTabel(referentieTabel.id!),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar("msg.tabel.verwijderen.uitgevoerd", {
            tabel: referentieTabel.code,
          });
          this.laadReferentieTabellen();
        }
      });
  }
}
