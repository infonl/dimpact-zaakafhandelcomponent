/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  ElementRef,
  inject,
  OnInit,
  ViewChild,
} from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import {
  injectMutation,
  injectQuery,
} from "@tanstack/angular-query-experimental";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { FileDragAndDropDirective } from "../../shared/directives/file-drag-and-drop.directive";
import { SharedModule } from "../../shared/shared.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { BpmnService } from "../bpmn.service";
import { BpmnProcessDefinitionItemComponent } from "./bpmn-process-definition-item/bpmn-process-definition-item.component";
import { readFileContent } from "./file.helper";

interface GroupNode {
  name: string;
  key: string;
  definition: GeneratedType<"RestBpmnProcessDefinition">;
}

type Node = GroupNode | GeneratedType<"RestBpmnProcessDefinition">;

@Component({
  standalone: true,
  templateUrl: "./bpmn-process-definitions.component.html",
  styleUrls: ["./bpmn-process-definitions.component.less"],
  imports: [
    SharedModule,
    BpmnProcessDefinitionItemComponent,
    FileDragAndDropDirective,
  ],
})
export class BpmnProcessDefinitionsComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("bpmnProcessDefinitionFileInput", { static: false })
  bpmnProcessDefinitionFileInput!: ElementRef;

  protected readonly processDefinitionsQuery = injectQuery(() =>
    this.bpmnService.listProcessDefinitionsQuery(true),
  );

  protected readonly data = computed(() =>
    this.buildTreeData(this.processDefinitionsQuery.data() ?? []),
  );

  protected expandedKey: string | null = null;

  protected toggleNode(node: GroupNode) {
    this.expandedKey = this.expandedKey === node.key ? null : node.key;
  }

  childrenAccessor = (node: Node) =>
    "definition" in node ? [(node as GroupNode).definition] : [];

  hasChild = (_: number, node: Node): node is GroupNode => "definition" in node;

  private readonly dialog = inject(MatDialog);
  private readonly bpmnService = inject(BpmnService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  private readonly uploadMutation = injectMutation(() => ({
    ...this.bpmnService.uploadProcessDefinitionQuery(),
    onSuccess: () => void this.processDefinitionsQuery.refetch(),
  }));

  private readonly deleteMutation = injectMutation(() => ({
    mutationFn: (key: string) => this.bpmnService.deleteProcessDefinition(key),
  }));

  constructor() {
    super(inject(UtilService), inject(ConfiguratieService));
  }

  ngOnInit() {
    this.setupMenu("title.procesdefinities");
  }

  protected selectBpmnProcessDefinitionFile() {
    this.bpmnProcessDefinitionFileInput.nativeElement.click();
  }

  protected bpmnProcessDefinitionFileSelected(event: Event) {
    if (event.target instanceof HTMLInputElement) {
      const file = event.target.files?.[0];
      event.target.value = "";
      if (file) this.uploadBpmnFile(file);
    }
  }

  protected bpmnProcessDefinitionFileDropped(files: FileList) {
    const file = Array.from(files).find((file) =>
      file.name.toLowerCase().endsWith(".bpmn"),
    );
    if (file) this.uploadBpmnFile(file);
  }

  private uploadBpmnFile(file: File) {
    readFileContent(file)
      .then((content) => {
        this.uploadMutation.mutate({ content, filename: file.name });
      })
      .catch((error) => {
        this.foutAfhandelingService.foutAfhandelen(error);
      });
  }

  protected deleteProcessDefinition(processDefinition: {
    key: string;
    name: string;
  }) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData({
          key: "msg.procesdefinitie.verwijderen.bevestigen",
          args: { naam: processDefinition.name },
        }),
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.deleteMutation.mutate(processDefinition.key, {
            onSuccess: () => {
              this.utilService.openSnackbar(
                "msg.procesdefinitie.verwijderen.uitgevoerd",
                { naam: processDefinition.name },
              );
              void this.processDefinitionsQuery.refetch();
            },
          });
        }
      });
  }

  protected refreshDefinitions() {
    void this.processDefinitionsQuery.refetch();
  }

  protected asProcessDefinition(
    node: Node,
  ): GeneratedType<"RestBpmnProcessDefinition"> {
    return node as GeneratedType<"RestBpmnProcessDefinition">;
  }

  protected hasAllFormsUploaded(node: GroupNode): boolean {
    const forms = node.definition.details?.forms ?? [];
    return forms.length > 0 && forms.every((form) => form.uploaded);
  }

  private buildTreeData(
    definitions: GeneratedType<"RestBpmnProcessDefinition">[],
  ): GroupNode[] {
    return definitions.map((def) => ({
      name: def.name,
      key: def.key,
      definition: def,
    }));
  }
}
