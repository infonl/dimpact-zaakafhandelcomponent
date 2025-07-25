/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2025 INFO.nl
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
import { ProcessDefinitionsService } from "../process-definitions.service";

@Component({
  templateUrl: "./process-definitions.component.html",
  styleUrls: ["./process-definitions.component.less"],
})
export class ProcessDefinitionsComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("fileInput", { static: false }) fileInput!: ElementRef;

  isLoadingResults = false;
  columns: string[] = ["name", "version", "key", "id"];
  dataSource = new MatTableDataSource<GeneratedType<"RestProcessDefinition">>();

  constructor(
    public dialog: MatDialog,
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private processDefinitionsService: ProcessDefinitionsService,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.setupMenu("title.procesdefinities");
    this.loadProcessDefinitions();
  }

  selectFile() {
    this.fileInput.nativeElement.click();
  }

  fileSelected(event: Event) {
    if (event.target instanceof HTMLInputElement) {
      const file = event.target.files?.[0];
      if (!file) return;

      this.readFileContent(file)
        .then((content) => {
          this.processDefinitionsService
            .uploadProcessDefinition({
              content,
              filename: file.name,
            })
            .subscribe(() => {
              this.loadProcessDefinitions();
            });
        })
        .catch((error) => {
          this.foutAfhandelingService.foutAfhandelen(error);
        });
    }
  }

  delete(processDefinition: GeneratedType<"RestProcessDefinition">) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.procesdefinitie.verwijderen.bevestigen",
            args: { naam: processDefinition.name },
          },
          this.processDefinitionsService.deleteProcessDefinition(
            processDefinition.key,
          ),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar(
            "msg.procesdefinitie.verwijderen.uitgevoerd",
            { naam: processDefinition.name },
          );
          this.loadProcessDefinitions();
        }
      });
  }

  private loadProcessDefinitions() {
    this.isLoadingResults = true;
    this.utilService.setLoading(true);
    this.processDefinitionsService
      .listProcessDefinitions()
      .subscribe((processDefinitions) => {
        this.dataSource.data = processDefinitions;
        this.isLoadingResults = false;
        this.utilService.setLoading(false);
      });
  }

  private readFileContent(file: File) {
    return new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = (error) => reject(error);
      reader.readAsText(file);
    });
  }
}
