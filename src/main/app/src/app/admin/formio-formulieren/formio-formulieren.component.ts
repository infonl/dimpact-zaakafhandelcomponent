/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, ElementRef, OnInit, ViewChild } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { FormioFormulierenService } from "../formio-formulieren.service";

@Component({
  templateUrl: "./formio-formulieren.component.html",
  styleUrls: ["./formio-formulieren.component.less"],
})
export class FormioFormulierenComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("fileInput", { static: false }) fileInput!: ElementRef;

  isLoadingResults = false;
  columns: string[] = ["name", "title", "id"];
  dataSource = new MatTableDataSource<GeneratedType<"RestFormioFormulier">>();

  constructor(
    public readonly dialog: MatDialog,
    public readonly utilService: UtilService,
    public readonly configuratieService: ConfiguratieService,
    private readonly formioFormulierenService: FormioFormulierenService,
    private readonly foutAfhandelingService: FoutAfhandelingService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.setupMenu("title.formioformulieren");
    this.loadFormioFormulieren();
  }

  selectFile() {
    this.fileInput.nativeElement.click();
  }

  fileSelected(event: Event) {
    const target = event.target as HTMLInputElement | null;
    const file = target?.files?.[0];
    if (!file) return;
    this.readFileContent(file)
      .then((content) => {
        this.formioFormulierenService
          .uploadFormioFormulier({
            filename: file.name,
            content,
          })
          .subscribe(() => {
            this.loadFormioFormulieren();
          });
      })
      .catch((error) => {
        this.foutAfhandelingService.foutAfhandelen(error);
      });
  }

  delete(formioFormulier: GeneratedType<"RestFormioFormulier">) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.formioformulier.verwijderen.bevestigen",
            args: { naam: formioFormulier.name },
          },
          this.formioFormulierenService.deleteFormioFormulier(
            formioFormulier.id!,
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
    this.formioFormulierenService
      .listFormioFormulieren()
      .subscribe((formioFormulier) => {
        this.dataSource.data = formioFormulier;
        this.isLoadingResults = false;
        this.utilService.setLoading(false);
      });
  }

  private readFileContent(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = (error) => reject(error);
      reader.readAsText(file);
    });
  }
}
