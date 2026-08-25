/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { animate, style, transition, trigger } from "@angular/animations";
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
import { MatExpansionModule } from "@angular/material/expansion";
import { Router } from "@angular/router";
import { forkJoin, lastValueFrom } from "rxjs";
import { UtilService } from "../../../core/service/util.service";
import { FoutAfhandelingService } from "../../../fout-afhandeling/fout-afhandeling.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../../shared/confirm-dialog/confirm-dialog.component";
import { FileDragAndDropDirective } from "../../../shared/directives/file-drag-and-drop.directive";
import { injectMutation } from "../../../shared/http/inject-mutation";
import { SharedModule } from "../../../shared/shared.module";
import { GeneratedType } from "../../../shared/utils/generated-types";
import {
  promptForSaveLocation,
  writeFile,
} from "../../../shared/utils/save-file";
import { BpmnService } from "../../bpmn.service";
import { extractAttachmentFilename, readFileContent } from "../file.helper";

type FormFile = { file: File; content: string };

@Component({
  standalone: true,
  selector: "zac-bpmn-process-definition-item",
  templateUrl: "./bpmn-process-definition-item.component.html",
  styleUrls: ["./bpmn-process-definition-item.component.less"],
  imports: [SharedModule, FileDragAndDropDirective, MatExpansionModule],
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
export class BpmnProcessDefinitionItemComponent {
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
  private readonly router = inject(Router);
  private readonly bpmnService = inject(BpmnService);
  private readonly utilService = inject(UtilService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly deleteProcessDefinitionFormMutation = injectMutation(() =>
    this.bpmnService.deleteProcessDefinitionForm(),
  );

  protected readonly downloadMutation = injectMutation(
    () => ({
      mutationFn: async () => {
        const { key, version } = this.processDefinition();
        const fileHandle = await promptForSaveLocation({
          suggestedName: `${key}-v${version}.zip`,
          types: [{ accept: { "application/zip": [".zip"] } }],
        });
        const response = await lastValueFrom(
          this.bpmnService.downloadProcessDefinition(key),
        );

        return { fileHandle, response };
      },
    }),
    {
      onSuccess: async ({ fileHandle, response }) => {
        if (!response.body) return;
        if (fileHandle) {
          await writeFile(fileHandle, response.body);
          return;
        }
        this.utilService.downloadBlobResponse(
          response.body,
          extractAttachmentFilename(
            response.headers.get("Content-Disposition"),
          ) ?? `${this.processDefinition().key}.zip`,
        );
      },
      onError: (error) => {
        if (error.name === "AbortError") return;
        this.utilService.openSnackbarError(
          "msg.error.bpmn.process.definition.download.failed",
        );
      },
    },
  );

  protected uploadBpmnForm() {
    this.bpmnFormFileInput().nativeElement.click();
  }

  protected openFormulierBuilder(
    form: GeneratedType<"RestBpmnProcessDefinitionForm">,
  ) {
    void this.router.navigate(
      [
        "/admin/bpmn-procesdefinities",
        this.processDefinition().key,
        "taakformulier",
        form.formKey,
      ],
      form.uploaded ? { queryParams: { bewerken: true } } : {},
    );
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
      .then((formFiles) => {
        const unmatchedFileNames = formFiles
          .filter((formFile) => !this.matchesTaskFormKey(formFile))
          .map(({ file }) => file.name);

        if (!unmatchedFileNames.length) {
          this.uploadForms(formFiles);
          return;
        }
        this.dialog
          .open(ConfirmDialogComponent, {
            data: new ConfirmDialogData({
              key: "msg.bpmn.task-forms.upload.unlinked.confirm",
              args: { namen: unmatchedFileNames.join(", ") },
            }),
          })
          .afterClosed()
          .subscribe((confirmed) => {
            if (confirmed) this.uploadForms(formFiles);
          });
      })
      .catch((error) => {
        this.foutAfhandelingService.foutAfhandelen(error);
      });
  }

  /**
   * A form is linked to a user task by the `name` in its JSON, so a form whose name matches no task
   * is stored and reported as uploaded while the task it was meant for stays without a form.
   */
  private matchesTaskFormKey(formFile: FormFile) {
    return (this.processDefinition().details?.forms ?? []).some(
      ({ formKey }) => formKey === this.deriveFormName(formFile),
    );
  }

  /** Mirrors how the backend names an uploaded form. */
  private deriveFormName({ file, content }: FormFile) {
    try {
      const { name } = JSON.parse(content) as { name?: string };
      if (name) return name;
    } catch {
      // unparseable content is for the backend to reject
    }
    return file.name.endsWith(".json") ? file.name.slice(0, -5) : file.name;
  }

  private uploadForms(formFiles: FormFile[]) {
    forkJoin(
      formFiles.map(({ file, content }) =>
        this.bpmnService.uploadProcessDefinitionForm(
          this.processDefinition().key,
          { filename: file.name, content },
        ),
      ),
    ).subscribe(() => {
      this.utilService.openSnackbar("msg.bpmn.task-forms.upload.success", {
        namen: formFiles.map(({ file }) => file.name).join(", "),
      });
      if (formFiles.length >= this.missingForms().length) {
        this.forceHideWarning.set(true);
        setTimeout(() => this.bpmnFormListChanged.emit(), 450);
      } else {
        this.bpmnFormListChanged.emit();
      }
    });
  }

  protected deleteBpmnForm(bpmnFormName: string) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData({
          key: "msg.bpmn.task-forms.delete.confirm",
          args: { naam: bpmnFormName },
        }),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.deleteProcessDefinitionFormMutation.mutate(
            {
              processDefinitionKey: this.processDefinition().key,
              name: bpmnFormName,
            },
            {
              onSuccess: () =>
                this.utilService.openSnackbar("msg.bpmn.task-forms.deleted", {
                  namen: bpmnFormName,
                }),
            },
          );
        }
      });
  }

  protected deleteAllOrphanedForms() {
    const orphanedForms = this.processDefinition().details?.orphanedForms ?? [];
    if (!orphanedForms.length) return;

    Promise.all(
      orphanedForms.map((form) =>
        this.deleteProcessDefinitionFormMutation.mutateAsync({
          processDefinitionKey: this.processDefinition().key,
          name: form.formKey,
        }),
      ),
    )
      .then(() =>
        this.utilService.openSnackbar("msg.bpmn.task-forms.deleted", {
          namen: orphanedForms.map(({ formKey }) => formKey).join(", "),
        }),
      )
      // the query client already reported the failure to the user
      .catch(() => undefined);
  }
}
