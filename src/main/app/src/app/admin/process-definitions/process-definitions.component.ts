/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, ElementRef, OnInit, ViewChild } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { SharedModule } from "../../shared/shared.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { BpmnService } from "../bpmn.service";
import { ProcessDefinitionItemComponent } from "./process-definition-item/process-definition-item.component";

interface ProcessDefinitionGroupNode {
  name: string;
  key: string;
  versions?: GeneratedType<"RestBpmnProcessDefinition">[];
}

type ProcessDefinitionNode =
  | ProcessDefinitionGroupNode
  | GeneratedType<"RestBpmnProcessDefinition">;

@Component({
  templateUrl: "./process-definitions.component.html",
  styleUrls: ["./process-definitions.component.less"],
  imports: [SharedModule, ProcessDefinitionItemComponent],
})
export class ProcessDefinitionsComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("bpmnProcessDefinitionFileInput", { static: false })
  bpmnProcessDefinitionFileInput!: ElementRef;
  @ViewChild("formioFileInput", { static: false })
  formioFileInput!: ElementRef;

  treeData: ProcessDefinitionGroupNode[] = [];

  childrenAccessor = (node: ProcessDefinitionNode) =>
    (node as ProcessDefinitionGroupNode).versions ?? [];

  hasChild = (
    _: number,
    node: ProcessDefinitionNode,
  ): node is ProcessDefinitionGroupNode =>
    "versions" in node &&
    Boolean((node as ProcessDefinitionGroupNode).versions?.length);

  private currentProcessDefinitionKey = "";

  constructor(
    public dialog: MatDialog,
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private bpmnService: BpmnService,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.setupMenu("title.procesdefinities");
    this.loadProcessDefinitions();
  }

  selectBpmnProcessDefinitionFile() {
    this.bpmnProcessDefinitionFileInput.nativeElement.click();
  }

  bpmnProcessDefinitionFileSelected(event: Event) {
    if (event.target instanceof HTMLInputElement) {
      const file = event.target.files?.[0];
      if (!file) return;

      this.readFileContent(file)
        .then((content) => {
          this.bpmnService
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

  deleteProcessDefinition(processDefinition: { key: string; name: string }) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.procesdefinitie.verwijderen.bevestigen",
            args: { naam: processDefinition.name },
          },
          this.bpmnService.deleteProcessDefinition(processDefinition.key),
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

  selectFormioFile(bpmnProcessDefinitionKey: string) {
    this.currentProcessDefinitionKey = bpmnProcessDefinitionKey;
    this.formioFileInput.nativeElement.click();
  }

  formioFileSelected(event: Event) {
    const target = event.target as HTMLInputElement | null;
    const file = target?.files?.[0];
    if (!file) return;

    this.readFileContent(file)
      .then((content) => {
        this.bpmnService
          .uploadProcessDefinitionForm(this.currentProcessDefinitionKey, {
            filename: file.name,
            content,
          })
          .subscribe(() => {
            this.loadProcessDefinitions();
          });
      })
      .catch((error) => {
        this.foutAfhandelingService.foutAfhandelen(error);
      });
  }

  asProcessDefinition(
    node: ProcessDefinitionNode,
  ): GeneratedType<"RestBpmnProcessDefinition"> {
    return node as GeneratedType<"RestBpmnProcessDefinition">;
  }

  private loadProcessDefinitions() {
    this.utilService.setLoading(true);
    this.bpmnService
      .listProcessDefinitions()
      .subscribe((processDefinitions) => {
        this.treeData = this.buildTreeData(processDefinitions);
        this.utilService.setLoading(false);
      });
  }

  private buildTreeData(
    definitions: GeneratedType<"RestBpmnProcessDefinition">[],
  ): ProcessDefinitionGroupNode[] {
    const groupMap = new Map<string, ProcessDefinitionGroupNode>();

    for (const def of definitions) {
      if (!groupMap.has(def.key)) {
        groupMap.set(def.key, { name: def.name, key: def.key, versions: [] });
      }
      groupMap.get(def.key)!.versions!.push(def);
    }

    return Array.from(groupMap.values());
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
