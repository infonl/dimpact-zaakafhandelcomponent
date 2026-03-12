/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  ViewChild,
} from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatTableDataSource } from "@angular/material/table";
import { UtilService } from "../../../core/service/util.service";
import { FoutAfhandelingService } from "../../../fout-afhandeling/fout-afhandeling.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../../shared/confirm-dialog/confirm-dialog.component";
import { SharedModule } from "../../../shared/shared.module";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BpmnService } from "../../bpmn.service";
import { readFileContent } from "../file.helper";

@Component({
  selector: "zac-process-definition-item",
  templateUrl: "./process-definition-item.component.html",
  styleUrls: ["./process-definition-item.component.less"],
  imports: [SharedModule],
})
export class ProcessDefinitionItemComponent implements OnChanges {
  @Input({ required: true })
  processDefinition!: GeneratedType<"RestBpmnProcessDefinition">;
  @Input()
  forms: GeneratedType<"RestFormioFormulier">[] = [];

  @Output() bpmnFormListChanged = new EventEmitter<void>();

  @ViewChild("bpmnFormFileInput", { static: false })
  bpmnFormFileInput!: ElementRef;

  protected formsDataSource = new MatTableDataSource<
    GeneratedType<"RestFormioFormulier">
  >();
  protected columns = ["index", "name", "title", "id"];

  private readonly dialog = inject(MatDialog);
  private readonly bpmnService = inject(BpmnService);
  private readonly utilService = inject(UtilService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  ngOnChanges(): void {
    this.formsDataSource.data = this.forms.filter(
      (form) => form.bpmnProcessDefinition === this.processDefinition.key,
    );
  }

  protected uploadBpmnForm() {
    this.bpmnFormFileInput.nativeElement.click();
  }

  protected bpmnFormFileSelected(event: Event) {
    const target = event.target as HTMLInputElement | null;
    const file = target?.files?.[0];
    if (!file) return;

    readFileContent(file)
      .then((content) => {
        this.bpmnService
          .uploadProcessDefinitionForm(this.processDefinition.key, {
            filename: file.name,
            content,
          })
          .subscribe(() => {
            this.utilService.openSnackbar(
              "msg.formioformulier.uploaden.uitgevoerd",
              { naam: file.name },
            );
            this.bpmnFormListChanged.emit();
          });
      })
      .catch((error) => {
        this.foutAfhandelingService.foutAfhandelen(error);
      });
  }

  protected deleteBpmnForm(bpmnForm: GeneratedType<"RestFormioFormulier">) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.formioformulier.verwijderen.bevestigen",
            args: { naam: bpmnForm.name },
          },
          this.bpmnService.deleteProcessDefinitionForm(
            bpmnForm.bpmnProcessDefinition!,
            bpmnForm.name!,
          ),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar(
            "msg.formioformulier.verwijderen.uitgevoerd",
            { naam: bpmnForm.name },
          );
          this.bpmnFormListChanged.emit();
        }
      });
  }
}
