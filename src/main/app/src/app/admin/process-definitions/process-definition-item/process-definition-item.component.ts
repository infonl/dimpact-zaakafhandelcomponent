/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { animate, style, transition, trigger } from "@angular/animations";
import { forkJoin } from "rxjs";
import {
  Component,
  computed,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { UtilService } from "../../../core/service/util.service";
import { FoutAfhandelingService } from "../../../fout-afhandeling/fout-afhandeling.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../../shared/confirm-dialog/confirm-dialog.component";
import { FileDragAndDropDirective } from "../../../shared/directives/file-drag-and-drop.directive";
import { SharedModule } from "../../../shared/shared.module";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BpmnService } from "../../bpmn.service";
import { readFileContent } from "../file.helper";

@Component({
  standalone: true,
  selector: "zac-process-definition-item",
  templateUrl: "./process-definition-item.component.html",
  styleUrls: ["./process-definition-item.component.less"],
  imports: [SharedModule, FileDragAndDropDirective],
  animations: [
    trigger("fadeSlide", [
      transition(":enter", [
        style({ opacity: 0 }),
        animate("750ms ease-in", style({ opacity: 1 })),
      ]),
      transition(":leave", [animate("750ms ease-out", style({ opacity: 0 }))]),
    ]),
  ],
})
export class ProcessDefinitionItemComponent {
  readonly processDefinition =
    input.required<GeneratedType<"RestBpmnProcessDefinition">>();

  readonly bpmnFormListChanged = output<void>();

  protected readonly bpmnFormFileInput =
    viewChild.required<ElementRef>("bpmnFormFileInput");

  protected readonly columns = [
    "index",
    "uploaded",
    "formKey",
    "title",
    "actions",
  ];

  protected readonly missingForms = computed(() =>
    (this.processDefinition().details?.forms ?? []).filter(
      (form) => !form.uploaded,
    ),
  );

  private readonly forceHideWarning = signal(false);
  protected readonly showMissingWarning = computed(
    () => !this.forceHideWarning() && this.missingForms().length !== 0,
  );

  private readonly dialog = inject(MatDialog);
  private readonly bpmnService = inject(BpmnService);
  private readonly utilService = inject(UtilService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  protected uploadBpmnForm() {
    this.bpmnFormFileInput().nativeElement.click();
  }

  protected bpmnFormFileSelected(event: Event) {
    const target = event.target as HTMLInputElement | null;
    const files = Array.from(target?.files ?? []);
    if (target) target.value = "";
    this.uploadFiles(files);
  }

  protected bpmnFormFilesDropped(files: FileList) {
    this.uploadFiles(
      Array.from(files).filter((file) =>
        file.name.toLowerCase().endsWith(".json"),
      ),
    );
  }

  private uploadFiles(files: File[]) {
    if (!files.length) return;

    Promise.all(
      files.map((file) =>
        readFileContent(file).then((content) => ({ file, content })),
      ),
    )
      .then((fileContents) => {
        forkJoin(
          fileContents.map(({ file, content }) =>
            this.bpmnService.uploadProcessDefinitionForm(
              this.processDefinition().key,
              { filename: file.name, content },
            ),
          ),
        ).subscribe(() => {
          files.forEach((file) => {
            this.utilService.openSnackbar(
              "msg.formioformulier.uploaden.uitgevoerd",
              { naam: file.name },
            );
          });
          if (files.length >= this.missingForms().length) {
            this.forceHideWarning.set(true);
            setTimeout(() => this.bpmnFormListChanged.emit(), 450);
          } else {
            this.bpmnFormListChanged.emit();
          }
        });
      })
      .catch((error) => {
        this.foutAfhandelingService.foutAfhandelen(error);
      });
  }

  protected deleteBpmnForm(bpmnFormName: string) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.formioformulier.verwijderen.bevestigen",
            args: { naam: bpmnFormName },
          },
          this.bpmnService.deleteProcessDefinitionForm(
            this.processDefinition().key,
            bpmnFormName,
          ),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar(
            "msg.formioformulier.verwijderen.uitgevoerd",
            { naam: bpmnFormName },
          );
          this.bpmnFormListChanged.emit();
        }
      });
  }

  protected deleteOrphanedForm(formKey: string) {
    this.bpmnService
      .deleteProcessDefinitionForm(this.processDefinition().key, formKey)
      .subscribe(() => {
        this.utilService.openSnackbar(
          "msg.formioformulier.verwijderen.uitgevoerd",
          { naam: formKey },
        );
        this.bpmnFormListChanged.emit();
      });
  }
}
