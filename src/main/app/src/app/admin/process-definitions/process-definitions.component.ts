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
import { AdminComponent } from "../admin/admin.component";
import { ProcessDefinition } from "../model/process-definition";
import { ProcessDefinitionContent } from "../model/process-definition-content";
import { ProcessDefinitionService } from "../process-definition.service";

@Component({
  templateUrl: "./process-definitions.component.html",
  styleUrls: ["./process-definitions.component.less"],
})
export class ProcessDefinitionsComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav: MatSidenav;
  @ViewChild("fileInput", { static: false }) fileInput: ElementRef;

  isLoadingResults = false;
  columns: string[] = ["name", "version", "key", "id"];
  dataSource: MatTableDataSource<ProcessDefinition> =
    new MatTableDataSource<ProcessDefinition>();

  constructor(
    public dialog: MatDialog,
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private processDefinitionService: ProcessDefinitionService,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit(): void {
    this.setupMenu("title.procesdefinities");
    this.loadProcessDefinitions();
  }

  selectFile() {
    this.fileInput.nativeElement.click();
  }

  fileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.readFileContent(file)
        .then((content) => {
          this.processDefinitionService
            .uploadProcessDefinition(
              new ProcessDefinitionContent(file.name, content),
            )
            .subscribe(() => {
              this.loadProcessDefinitions();
            });
        })
        .catch((error) => {
          this.foutAfhandelingService.foutAfhandelen(error);
        });
    }
  }

  delete(processDefinition: ProcessDefinition): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.procesdefinitie.verwijderen.bevestigen",
            args: { naam: processDefinition.name },
          },
          this.processDefinitionService.deleteProcessDefinition(
            processDefinition,
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

  private loadProcessDefinitions(): void {
    this.isLoadingResults = true;
    this.utilService.setLoading(true);
    this.processDefinitionService
      .listProcessDefinitions()
      .subscribe((processDefinitions) => {
        this.dataSource.data = processDefinitions;
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
