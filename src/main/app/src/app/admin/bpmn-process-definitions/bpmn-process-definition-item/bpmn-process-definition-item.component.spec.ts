/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter, Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { fireEvent, render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { from, of, throwError } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { FoutAfhandelingService } from "../../../fout-afhandeling/fout-afhandeling.service";
import { SharedModule } from "../../../shared/shared.module";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BpmnService } from "../../bpmn.service";
import { readFileContent } from "../file.helper";
import { BpmnProcessDefinitionItemComponent } from "./bpmn-process-definition-item.component";

jest.mock("../file.helper", () => ({
  ...jest.requireActual("../file.helper"),
  readFileContent: jest.fn(),
}));

function makeFileList(...files: File[]): FileList {
  return fromPartial<FileList>({
    ...files,
    length: files.length,
    item: (index: number) => files[index] ?? null,
  });
}

const zipBlob = new Blob(["fakeZipContent"]);

const zipResponse = new HttpResponse({
  body: zipBlob,
  headers: new HttpHeaders({
    "Content-Disposition": 'attachment; filename="test-key-v2.zip"',
  }),
});

const uploadedForm: GeneratedType<"RestBpmnProcessDefinitionForm"> = {
  formKey: "form-uploaded",
  title: "Uploaded Form",
  uploaded: true,
};

const missingForm: GeneratedType<"RestBpmnProcessDefinitionForm"> = {
  formKey: "form-missing",
  title: "Missing Form",
  uploaded: false,
};

const orphanedForm: GeneratedType<"RestBpmnProcessDefinitionForm"> = {
  formKey: "form-orphaned",
  title: "Orphaned Form",
  uploaded: true,
};

const baseProcessDefinition = fromPartial<
  GeneratedType<"RestBpmnProcessDefinition">
>({
  id: "pd-1",
  key: "test-key",
  name: "Test Process",
  version: 2,
  details: {
    inUse: false,
    uploadDate: "2026-01-15T10:00:00Z",
    modificationDate: "2026-02-20T14:30:00Z",
    documentation: "Test documentation",
    forms: [uploadedForm, missingForm],
    orphanedForms: [],
  },
});

type ProcessDefinitionDetails = NonNullable<
  GeneratedType<"RestBpmnProcessDefinition">["details"]
>;

function withDetails(details: Partial<ProcessDefinitionDetails>) {
  return fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
    ...baseProcessDefinition,
    details: { ...baseProcessDefinition.details, ...details },
  });
}

describe(BpmnProcessDefinitionItemComponent.name, () => {
  let bpmnService: Pick<
    BpmnService,
    | "uploadProcessDefinitionForm"
    | "deleteProcessDefinitionForm"
    | "downloadProcessDefinition"
  >;
  let utilService: Pick<
    UtilService,
    "openSnackbar" | "openSnackbarError" | "downloadBlobResponse"
  >;
  let foutAfhandelingService: Pick<FoutAfhandelingService, "foutAfhandelen">;
  let bpmnFormListChanged: jest.Mock;
  let deleteProcessDefinitionFormMutation: ReturnType<
    typeof createMutationOptions<
      object,
      { processDefinitionKey: string; name: string }
    >
  >;
  let dialogOpen: jest.SpyInstance;
  let container: HTMLElement;
  let detectChanges: () => void;

  const user = userEvent.setup({ delay: null });

  async function setup(processDefinition = baseProcessDefinition) {
    const rendered = await render(BpmnProcessDefinitionItemComponent, {
      inputs: { processDefinition },
      on: { bpmnFormListChanged },
      imports: [SharedModule, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideQueryClient(testQueryClient),
        provideRouter([]),
        { provide: BpmnService, useValue: bpmnService },
        { provide: UtilService, useValue: utilService },
        { provide: FoutAfhandelingService, useValue: foutAfhandelingService },
      ],
    });
    container = rendered.container;
    detectChanges = () => rendered.detectChanges();
    // the mat-table creates its row views in one pass and binds the cells in the next
    rendered.detectChanges();
  }

  function fileInput() {
    // eslint-disable-next-line no-restricted-syntax, testing-library/no-node-access -- the file input is `display: none` and label-less by design; the upload button opens it programmatically, so it is not in the accessibility tree
    return container.querySelector<HTMLInputElement>('input[type="file"]')!;
  }

  function dropFiles(...files: File[]) {
    fireEvent.drop(
      screen.getByRole("heading", {
        name: "bpmn.process-definition.card.task-forms.title",
      }),
      { dataTransfer: { files: makeFileList(...files) } },
    );
  }

  function rowOf(formKey: string) {
    return screen.getByRole("row", { name: new RegExp(formKey) });
  }

  function confirmNextDialog() {
    dialogOpen.mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(true) }),
    );
  }

  beforeEach(() => {
    // the component imports SharedModule, so it injects MatDialog from its own
    // standalone injector rather than the one the TestBed hands out
    dialogOpen = jest
      .spyOn(MatDialog.prototype, "open")
      .mockReturnValue(
        fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(false) }),
      );
    bpmnFormListChanged = jest.fn();
    deleteProcessDefinitionFormMutation = createMutationOptions<
      object,
      { processDefinitionKey: string; name: string }
    >({});
    bpmnService = {
      uploadProcessDefinitionForm: jest.fn().mockReturnValue(of(null)),
      deleteProcessDefinitionForm: jest
        .fn()
        .mockReturnValue(deleteProcessDefinitionFormMutation),
      downloadProcessDefinition: jest.fn().mockReturnValue(of(zipResponse)),
    };
    utilService = {
      openSnackbar: jest.fn(),
      openSnackbarError: jest.fn(),
      downloadBlobResponse: jest.fn(),
    };
    foutAfhandelingService = { foutAfhandelen: jest.fn() };
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("shows the version, key and documentation of the process definition", async () => {
    await setup();

    expect(screen.getByText("test-key")).toBeVisible();
    expect(screen.getByText("Test documentation")).toBeVisible();
    expect(
      // eslint-disable-next-line no-restricted-syntax, testing-library/no-node-access -- zac-static-text renders a <label> that is not associated with a form control, so its value cannot be reached by role or label query
      container.querySelector('zac-static-text[label="versie"]'),
    ).toHaveTextContent("2");
  });

  describe("downloading the process definition", () => {
    const downloadButton = () =>
      screen.getByRole("button", {
        name: "bpmn.process-definition.card.details.button.download",
      });

    describe("in a browser that can ask where to save a file", () => {
      let writableStream: { write: jest.Mock; close: jest.Mock };
      let fileHandle: FileSystemFileHandle;

      beforeEach(() => {
        writableStream = { write: jest.fn(), close: jest.fn() };
        fileHandle = fromPartial<FileSystemFileHandle>({
          createWritable: jest.fn().mockResolvedValue(writableStream),
        });
        window.showSaveFilePicker = jest.fn().mockResolvedValue(fileHandle);
      });

      afterEach(() => {
        delete window.showSaveFilePicker;
      });

      it("writes the zip to the file the user chose, under the suggested name of the version on screen", async () => {
        await setup();

        await user.click(downloadButton());
        await sleep();

        expect(window.showSaveFilePicker).toHaveBeenCalledWith({
          suggestedName: "test-key-v2.zip",
          types: [{ accept: { "application/zip": [".zip"] } }],
        });
        expect(writableStream.write).toHaveBeenCalledWith(zipBlob);
        expect(writableStream.close).toHaveBeenCalled();
        expect(utilService.downloadBlobResponse).not.toHaveBeenCalled();
      });

      it("asks where to save before requesting the zip, which outlives the user activation of the click", async () => {
        let chooseFile!: (fileHandle: FileSystemFileHandle) => void;
        window.showSaveFilePicker = jest.fn(
          () =>
            new Promise<FileSystemFileHandle>((resolve) => {
              chooseFile = resolve;
            }),
        );
        await setup();

        await user.click(downloadButton());
        await sleep();

        expect(bpmnService.downloadProcessDefinition).not.toHaveBeenCalled();

        chooseFile(fileHandle);
        await sleep();

        expect(bpmnService.downloadProcessDefinition).toHaveBeenCalledWith(
          "test-key",
        );
        expect(writableStream.write).toHaveBeenCalledWith(zipBlob);
      });

      it("requests nothing and reports nothing when the user closes the dialog", async () => {
        window.showSaveFilePicker = jest
          .fn()
          .mockRejectedValue(
            new DOMException("fakeAbortMessage", "AbortError"),
          );
        await setup();

        await user.click(downloadButton());
        await sleep();

        expect(bpmnService.downloadProcessDefinition).not.toHaveBeenCalled();
        expect(utilService.openSnackbarError).not.toHaveBeenCalled();
        expect(utilService.downloadBlobResponse).not.toHaveBeenCalled();
      });

      it("reports a failure of the request as any other", async () => {
        (bpmnService.downloadProcessDefinition as jest.Mock).mockReturnValue(
          throwError(() => new Error("fakeDownloadFailure")),
        );
        await setup();

        await user.click(downloadButton());
        await sleep();

        expect(utilService.openSnackbarError).toHaveBeenCalledWith(
          "msg.error.bpmn.process.definition.download.failed",
        );
        expect(writableStream.write).not.toHaveBeenCalled();
      });
    });

    it("names the zip after the Content-Disposition of the response", async () => {
      await setup();

      await user.click(downloadButton());
      await sleep();

      expect(bpmnService.downloadProcessDefinition).toHaveBeenCalledWith(
        "test-key",
      );
      expect(utilService.downloadBlobResponse).toHaveBeenCalledWith(
        zipBlob,
        "test-key-v2.zip",
      );
    });

    it("falls back to the process definition key when the response has no filename", async () => {
      (bpmnService.downloadProcessDefinition as jest.Mock).mockReturnValue(
        of(new HttpResponse({ body: zipBlob })),
      );
      await setup();

      await user.click(downloadButton());
      await sleep();

      expect(utilService.downloadBlobResponse).toHaveBeenCalledWith(
        zipBlob,
        "test-key.zip",
      );
    });

    it("shows an error message and downloads nothing when the request fails", async () => {
      (bpmnService.downloadProcessDefinition as jest.Mock).mockReturnValue(
        throwError(() => new Error("fakeDownloadFailure")),
      );
      await setup();

      await user.click(downloadButton());
      await sleep();

      expect(utilService.openSnackbarError).toHaveBeenCalledWith(
        "msg.error.bpmn.process.definition.download.failed",
      );
      expect(utilService.downloadBlobResponse).not.toHaveBeenCalled();
    });

    it("disables the button while the download is running", async () => {
      // let the mutation state reach the template without waiting for a batch
      notifyManager.setScheduler((fn) => fn());
      try {
        let resolveDownload!: (response: HttpResponse<Blob>) => void;
        (bpmnService.downloadProcessDefinition as jest.Mock).mockReturnValue(
          from(
            new Promise<HttpResponse<Blob>>((resolve) => {
              resolveDownload = resolve;
            }),
          ),
        );
        await setup();

        await user.click(downloadButton());
        await sleep();
        detectChanges();

        expect(downloadButton()).toBeDisabled();

        resolveDownload(new HttpResponse({ body: zipBlob }));
        await sleep();
        detectChanges();

        expect(downloadButton()).toBeEnabled();
      } finally {
        notifyManager.setScheduler((fn) => setTimeout(fn, 0));
      }
    });
  });

  it("tells the user when the process definition is in use", async () => {
    await setup(withDetails({ inUse: true }));

    expect(
      screen.getByText("bpmn.process-definition.card.details.in-use"),
    ).toBeVisible();
  });

  it("says nothing about being in use when it is not", async () => {
    await setup();

    expect(
      screen.queryByText("bpmn.process-definition.card.details.in-use"),
    ).not.toBeInTheDocument();
  });

  it("warns when not every task form has been uploaded", async () => {
    await setup();

    expect(
      screen.getByText("bpmn.process-definition.card.task-forms.title.tooltip"),
    ).toBeVisible();
  });

  it("does not warn when every task form has been uploaded", async () => {
    await setup(withDetails({ forms: [uploadedForm] }));

    expect(
      screen.queryByText(
        "bpmn.process-definition.card.task-forms.title.tooltip",
      ),
    ).not.toBeInTheDocument();
  });

  it("shows a row per task form and marks the uploaded ones", async () => {
    await setup();

    expect(
      within(rowOf("form-uploaded")).getByText("check_circle"),
    ).toBeVisible();
    expect(within(rowOf("form-missing")).getByText("error")).toBeVisible();
  });

  it("shows an empty message when there are no task forms", async () => {
    await setup(withDetails({ forms: [] }));

    expect(screen.queryAllByRole("row", { name: /form-/ })).toHaveLength(0);
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("only offers to delete task forms that have been uploaded", async () => {
    await setup();

    expect(
      within(rowOf("form-uploaded")).getByRole("button", {
        name: "actie.verwijderen",
      }),
    ).toBeEnabled();
    expect(
      within(rowOf("form-missing")).queryByRole("button", {
        name: "actie.verwijderen",
      }),
    ).not.toBeInTheDocument();
  });

  it("does not allow deleting task forms of a process definition that is in use", async () => {
    await setup(withDetails({ inUse: true }));

    expect(
      within(rowOf("form-uploaded")).getByRole("button", {
        name: "actie.verwijderen",
      }),
    ).toBeDisabled();
  });

  it("opens the file picker from the upload button", async () => {
    await setup();
    const click = jest.spyOn(fileInput(), "click");

    await user.click(
      screen.getByRole("button", {
        name: "bpmn.process-definition.task-forms.button.upload.title",
      }),
    );

    expect(click).toHaveBeenCalled();
  });

  it("uploads the chosen task form and announces it", async () => {
    jest.useFakeTimers();
    const fileContent = '{"name": "form-missing"}';
    (readFileContent as jest.Mock).mockResolvedValue(fileContent);
    await setup();

    await user.upload(
      fileInput(),
      new File([fileContent], "form-missing.json"),
    );
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();

    expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalledWith(
      "test-key",
      { filename: "form-missing.json", content: fileContent },
    );
    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.bpmn.task-forms.upload.success",
      { namen: "form-missing.json" },
    );

    jest.runAllTimers();
    expect(bpmnFormListChanged).toHaveBeenCalled();
  });

  it("lets the same task form be chosen again after uploading it", async () => {
    (readFileContent as jest.Mock).mockResolvedValue("{}");
    await setup();

    await user.upload(fileInput(), new File(["{}"], "test-form.json"));

    expect(fileInput().value).toBe("");
  });

  it("reports a task form that cannot be read", async () => {
    const error = new Error("read error");
    (readFileContent as jest.Mock).mockRejectedValue(error);
    await setup();

    await user.upload(fileInput(), new File(["bad"], "bad.json"));
    await sleep();

    expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
  });

  it("uploads dropped task forms and announces them", async () => {
    jest.useFakeTimers();
    const fileContent = '{"name": "form-missing"}';
    (readFileContent as jest.Mock).mockResolvedValue(fileContent);
    await setup();

    dropFiles(new File([fileContent], "form-missing.json"));
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();

    expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalledWith(
      "test-key",
      { filename: "form-missing.json", content: fileContent },
    );
    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.bpmn.task-forms.upload.success",
      { namen: "form-missing.json" },
    );

    jest.runAllTimers();
    expect(bpmnFormListChanged).toHaveBeenCalled();
  });

  it("accepts a dropped task form whose extension is upper case", async () => {
    const fileContent = '{"name": "form-missing"}';
    (readFileContent as jest.Mock).mockResolvedValue(fileContent);
    await setup();

    dropFiles(new File([fileContent], "form-missing.JSON"));
    await sleep();

    expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalledWith(
      "test-key",
      { filename: "form-missing.JSON", content: fileContent },
    );
  });

  it("ignores dropped files that are not task forms", async () => {
    await setup();

    dropFiles(new File(["<bpmn/>"], "process.bpmn"));
    await sleep();

    expect(bpmnService.uploadProcessDefinitionForm).not.toHaveBeenCalled();
  });

  it("reports a dropped task form that cannot be read", async () => {
    const error = new Error("read error");
    (readFileContent as jest.Mock).mockRejectedValue(error);
    await setup();

    dropFiles(new File(["bad"], "bad.json"));
    await sleep();

    expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
  });

  describe("a task form whose name matches no task of the process definition", () => {
    const fileContent = '{"name": "typed-wrong"}';

    it("asks for confirmation naming the task form instead of uploading it", async () => {
      (readFileContent as jest.Mock).mockResolvedValue(fileContent);
      await setup();

      dropFiles(new File([fileContent], "typed-wrong.json"));
      await sleep();

      const dialogData = dialogOpen.mock.calls[0][1].data;
      expect(dialogData._melding.key).toBe(
        "msg.bpmn.task-forms.upload.unlinked.confirm",
      );
      expect(dialogData._melding.args).toEqual({ namen: "typed-wrong.json" });
      expect(bpmnService.uploadProcessDefinitionForm).not.toHaveBeenCalled();
    });

    it("uploads it once the confirmation is accepted", async () => {
      (readFileContent as jest.Mock).mockResolvedValue(fileContent);
      await setup();
      confirmNextDialog();

      dropFiles(new File([fileContent], "typed-wrong.json"));
      await sleep();

      expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalledWith(
        "test-key",
        { filename: "typed-wrong.json", content: fileContent },
      );
    });

    it("falls back to the file name when the task form holds no name", async () => {
      const namelessContent = '{"components": []}';
      (readFileContent as jest.Mock).mockResolvedValue(namelessContent);
      await setup();

      dropFiles(new File([namelessContent], "form-missing.json"));
      await sleep();

      expect(dialogOpen).not.toHaveBeenCalled();
      expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalled();
    });

    it("leaves unparseable content to the backend rather than warning about it", async () => {
      const unparseableContent = "not json at all";
      (readFileContent as jest.Mock).mockResolvedValue(unparseableContent);
      await setup();

      dropFiles(new File([unparseableContent], "form-missing.json"));
      await sleep();

      expect(dialogOpen).not.toHaveBeenCalled();
      expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalled();
    });
  });

  describe("opening the form builder", () => {
    const createButtonName =
      "bpmn.process-definition.task-forms.row.button.create";
    const editButtonName = "bpmn.process-definition.task-forms.row.button.edit";

    it("offers a builder button for every task form, uploaded or not", async () => {
      await setup();

      expect(
        within(rowOf("form-missing")).getByRole("button", {
          name: createButtonName,
        }),
      ).toBeVisible();
      expect(
        within(rowOf("form-uploaded")).getByRole("button", {
          name: editButtonName,
        }),
      ).toBeVisible();
    });

    it("opens an empty builder for a task form that has not been uploaded yet", async () => {
      await setup();
      const navigate = jest
        .spyOn(TestBed.inject(Router), "navigate")
        .mockResolvedValue(true);

      await user.click(
        within(rowOf("form-missing")).getByRole("button", {
          name: createButtonName,
        }),
      );

      expect(navigate).toHaveBeenCalledWith(
        [
          "/admin/bpmn-procesdefinities",
          "test-key",
          "taakformulier",
          "form-missing",
        ],
        {},
      );
    });

    it("opens the builder on the stored task form when it was already uploaded", async () => {
      await setup();
      const navigate = jest
        .spyOn(TestBed.inject(Router), "navigate")
        .mockResolvedValue(true);

      await user.click(
        within(rowOf("form-uploaded")).getByRole("button", {
          name: editButtonName,
        }),
      );

      expect(navigate).toHaveBeenCalledWith(
        [
          "/admin/bpmn-procesdefinities",
          "test-key",
          "taakformulier",
          "form-uploaded",
        ],
        { queryParams: { bewerken: true } },
      );
    });
  });

  it("asks for confirmation naming the task form to delete", async () => {
    await setup();

    await user.click(
      within(rowOf("form-uploaded")).getByRole("button", {
        name: "actie.verwijderen",
      }),
    );

    const dialogData = dialogOpen.mock.calls[0][1].data;
    expect(dialogData._melding.key).toBe("msg.bpmn.task-forms.delete.confirm");
    expect(dialogData._melding.args).toEqual({ naam: "form-uploaded" });
  });

  it("deletes the task form and announces it once confirmed", async () => {
    await setup();
    confirmNextDialog();

    await user.click(
      within(rowOf("form-uploaded")).getByRole("button", {
        name: "actie.verwijderen",
      }),
    );
    await sleep();

    expect(deleteProcessDefinitionFormMutation.mutationFn).toHaveBeenCalledWith(
      { processDefinitionKey: "test-key", name: "form-uploaded" },
      expect.anything(),
    );
    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.bpmn.task-forms.deleted",
      { namen: "form-uploaded" },
    );
  });

  it("keeps the task form when the confirmation is cancelled", async () => {
    await setup();

    await user.click(
      within(rowOf("form-uploaded")).getByRole("button", {
        name: "actie.verwijderen",
      }),
    );
    await sleep();

    expect(
      deleteProcessDefinitionFormMutation.mutationFn,
    ).not.toHaveBeenCalled();
    expect(utilService.openSnackbar).not.toHaveBeenCalled();
  });

  it("does not show the orphaned forms section when there are none", async () => {
    await setup();

    expect(
      screen.queryByText(
        "bpmn.process-definition.card.task-forms.orphaned.title",
      ),
    ).not.toBeInTheDocument();
  });

  it("deletes every orphaned form and announces them in one message", async () => {
    await setup(withDetails({ orphanedForms: [orphanedForm] }));

    await user.click(
      screen.getByRole("button", {
        name: "bpmn.process-definition.card.task-forms.orphaned.title",
      }),
    );

    expect(screen.getByText("form-orphaned")).toBeVisible();

    await user.click(
      screen.getByRole("button", {
        name: "bpmn.process-definition.task-forms.orphaned.button.delete.title",
      }),
    );
    await sleep();

    expect(deleteProcessDefinitionFormMutation.mutationFn).toHaveBeenCalledWith(
      { processDefinitionKey: "test-key", name: "form-orphaned" },
      expect.anything(),
    );
    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.bpmn.task-forms.deleted",
      { namen: "form-orphaned" },
    );
  });
});
