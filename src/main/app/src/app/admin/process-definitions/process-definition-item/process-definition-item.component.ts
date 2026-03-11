/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
} from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatTableDataSource } from "@angular/material/table";
import { UtilService } from "../../../core/service/util.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../../shared/confirm-dialog/confirm-dialog.component";
import { SharedModule } from "../../../shared/shared.module";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BpmnService } from "../../bpmn.service";

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

  @Output() uploadBpmnFormFile = new EventEmitter<string>();
  @Output() bpmnFormListChanged = new EventEmitter<void>();

  protected formsDataSource = new MatTableDataSource<
    GeneratedType<"RestFormioFormulier">
  >();
  protected columns = ["index", "name", "title", "id"];

  private readonly dialog = inject(MatDialog);
  private readonly bpmnService = inject(BpmnService);
  private readonly utilService = inject(UtilService);

  ngOnChanges(): void {
    this.formsDataSource.data = this.forms.filter(
      (form) => form.bpmnProcessDefinition === this.processDefinition.key,
    );
  }

  protected uploadBpmnForm() {
    this.uploadBpmnFormFile.emit(this.processDefinition.key);
    this.bpmnFormListChanged.emit();
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
