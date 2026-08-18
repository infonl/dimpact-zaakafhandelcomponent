/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideAngularQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { fireEvent, render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { SharedModule } from "../../shared/shared.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BpmnService } from "../bpmn.service";
import { BpmnProcessDefinitionsComponent } from "./bpmn-process-definitions.component";
import { extractBpmnProcessKey, readFileContent } from "./file.helper";

jest.mock("./file.helper");

function makeFileList(...files: File[]): FileList {
  return fromPartial<FileList>({
    ...files,
    length: files.length,
    item: (index: number) => files[index] ?? null,
  });
}

const processDefinition = fromPartial<
  GeneratedType<"RestBpmnProcessDefinition">
>({
  id: "pd-1",
  key: "key-a",
  name: "Process A",
  version: 1,
  details: {
    inUse: false,
    forms: [{ formKey: "f1", title: "Form 1", uploaded: true }],
    orphanedForms: [],
  },
});

const detailsTitle = "bpmn.process-definition.card.details.title";

describe(BpmnProcessDefinitionsComponent.name, () => {
  let fixture: ComponentFixture<BpmnProcessDefinitionsComponent>;
  let bpmnService: Pick<
    BpmnService,
    | "listProcessDefinitionsQuery"
    | "uploadProcessDefinitionQuery"
    | "deleteProcessDefinition"
    | "uploadProcessDefinitionForm"
    | "deleteProcessDefinitionForm"
  >;
  let utilService: Pick<
    UtilService,
    "setLoading" | "setTitle" | "openSnackbar"
  >;
  let foutAfhandelingService: Pick<FoutAfhandelingService, "foutAfhandelen">;
  let dialogOpen: jest.SpyInstance;
  let container: HTMLElement;

  const user = userEvent.setup();

  // jsdom has no scrollIntoView; stub it per test and restore to avoid leaking.
  const originalScrollIntoView = Element.prototype.scrollIntoView;

  async function setup(
    definitions: GeneratedType<"RestBpmnProcessDefinition">[] = [
      processDefinition,
    ],
  ) {
    bpmnService.listProcessDefinitionsQuery = jest.fn().mockReturnValue({
      queryKey: ["/rest/bpmn-process-definitions"],
      queryFn: () => Promise.resolve(definitions),
    });

    const rendered = await render(BpmnProcessDefinitionsComponent, {
      imports: [SharedModule, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        provideAngularQuery(
          new QueryClient({ defaultOptions: { queries: { retry: false } } }),
        ),
        { provide: BpmnService, useValue: bpmnService },
        { provide: UtilService, useValue: utilService },
        { provide: ConfiguratieService, useValue: {} },
        { provide: FoutAfhandelingService, useValue: foutAfhandelingService },
      ],
    });
    fixture = rendered.fixture;
    container = rendered.container;

    await flushRendering();
  }

  async function flushRendering() {
    await fixture.whenStable();
    await sleep();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function fileInput() {
    return container.querySelector<HTMLInputElement>('input[accept=".bpmn"]')!;
  }

  function groupRowOf(name: string) {
    return screen.getByRole("button", { name }).parentElement!;
  }

  function dropFiles(...files: File[]) {
    fireEvent.drop(container.querySelector<HTMLElement>("[dropzone]")!, {
      dataTransfer: { files: makeFileList(...files) },
    });
  }

  function chooseFile(file: File) {
    fireEvent.change(fileInput(), { target: { files: [file] } });
  }

  beforeEach(() => {
    Element.prototype.scrollIntoView = jest.fn();
    notifyManager.setScheduler((fn) => fn());

    // the component imports SharedModule, so it injects MatDialog from its own
    // standalone injector rather than the one the TestBed hands out
    dialogOpen = jest
      .spyOn(MatDialog.prototype, "open")
      .mockReturnValue(
        fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(false) }),
      );

    bpmnService = {
      listProcessDefinitionsQuery: jest.fn(),
      uploadProcessDefinitionQuery: jest
        .fn()
        .mockReturnValue(createMutationOptions({})),
      deleteProcessDefinition: jest
        .fn()
        .mockReturnValue(createMutationOptions({})),
      uploadProcessDefinitionForm: jest.fn().mockReturnValue(of({})),
      deleteProcessDefinitionForm: jest.fn().mockReturnValue(of({})),
    };
    utilService = {
      setLoading: jest.fn(),
      setTitle: jest.fn(),
      openSnackbar: jest.fn(),
    };
    foutAfhandelingService = { foutAfhandelen: jest.fn() };
  });

  afterEach(() => {
    Element.prototype.scrollIntoView = originalScrollIntoView;
    notifyManager.setScheduler((fn) => setTimeout(fn, 0));
  });

  it("sets the title and shows a row per process definition", async () => {
    await setup();

    expect(utilService.setTitle).toHaveBeenCalledWith(
      "title.bpmn-procesdefinities",
      undefined,
    );
    expect(screen.getByRole("button", { name: "Process A" })).toBeVisible();
  });

  it("shows an empty message when there are no process definitions", async () => {
    await setup([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Process A" }),
    ).not.toBeInTheDocument();
  });

  it("marks a process definition whose forms have all been uploaded", async () => {
    await setup();

    expect(
      within(groupRowOf("Process A")).getByText("check_circle"),
    ).toBeVisible();
  });

  it("marks a process definition that is still missing a form", async () => {
    await setup([
      fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
        ...processDefinition,
        details: {
          ...processDefinition.details,
          forms: [{ formKey: "f1", title: "Form 1", uploaded: false }],
        },
      }),
    ]);

    expect(within(groupRowOf("Process A")).getByText("error")).toBeVisible();
  });

  it("marks a process definition without forms as incomplete", async () => {
    await setup([
      fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
        ...processDefinition,
        details: { ...processDefinition.details, forms: [] },
      }),
    ]);

    expect(within(groupRowOf("Process A")).getByText("error")).toBeVisible();
  });

  it("shows the details of a process definition when its row is expanded", async () => {
    await setup();

    expect(screen.getByText(detailsTitle)).not.toBeVisible();

    await user.click(screen.getByRole("button", { name: "Process A" }));

    expect(screen.getByText(detailsTitle)).toBeVisible();
  });

  it("collapses an expanded process definition when its row is clicked again", async () => {
    await setup();

    await user.click(screen.getByRole("button", { name: "Process A" }));
    await user.click(screen.getByRole("button", { name: "Process A" }));

    expect(screen.getByText(detailsTitle)).not.toBeVisible();
  });

  it("does not expand a process definition when its status icon is clicked", async () => {
    await setup();

    await user.click(within(groupRowOf("Process A")).getByText("check_circle"));

    expect(screen.getByText(detailsTitle)).not.toBeVisible();
  });

  it("opens the file picker from the upload button", async () => {
    await setup();
    const click = jest.spyOn(fileInput(), "click");

    await user.click(
      screen.getByRole("button", {
        name: "bpmn.process-definition.button.upload.definition",
      }),
    );

    expect(click).toHaveBeenCalled();
  });

  it("uploads the chosen process definition file", async () => {
    const content = "<bpmn/>";
    (readFileContent as jest.Mock).mockResolvedValue(content);
    await setup();

    chooseFile(new File([content], "process.bpmn"));
    await sleep();

    expect(
      bpmnService.uploadProcessDefinitionQuery().mutationFn,
    ).toHaveBeenCalledWith(
      { filename: "process.bpmn", content },
      expect.anything(),
    );
  });

  it("lets the same process definition file be chosen again after uploading it", async () => {
    (readFileContent as jest.Mock).mockResolvedValue("<bpmn/>");
    await setup();

    chooseFile(new File(["<bpmn/>"], "process.bpmn"));

    expect(fileInput().value).toBe("");
  });

  it("reports a chosen process definition file that cannot be read", async () => {
    const error = new Error("read error");
    (readFileContent as jest.Mock).mockRejectedValue(error);
    await setup();

    chooseFile(new File(["<bad>"], "bad.bpmn"));
    await sleep();

    expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
  });

  it("uploads a dropped process definition file", async () => {
    const content = "<bpmn/>";
    (readFileContent as jest.Mock).mockResolvedValue(content);
    await setup();

    dropFiles(new File([content], "dropped.bpmn"));
    await sleep();

    expect(
      bpmnService.uploadProcessDefinitionQuery().mutationFn,
    ).toHaveBeenCalledWith(
      { filename: "dropped.bpmn", content },
      expect.anything(),
    );
  });

  it("accepts a dropped process definition file whose extension is upper case", async () => {
    const content = "<bpmn/>";
    (readFileContent as jest.Mock).mockResolvedValue(content);
    await setup();

    dropFiles(new File([content], "process.BPMN"));
    await sleep();

    expect(
      bpmnService.uploadProcessDefinitionQuery().mutationFn,
    ).toHaveBeenCalledWith(
      { filename: "process.BPMN", content },
      expect.anything(),
    );
  });

  it("ignores dropped files that are not process definitions", async () => {
    await setup();

    dropFiles(new File(["{}"], "form.json"));
    await sleep();

    expect(readFileContent).not.toHaveBeenCalled();
  });

  it("ignores an empty drop", async () => {
    await setup();

    dropFiles();
    await sleep();

    expect(readFileContent).not.toHaveBeenCalled();
  });

  it("reports a dropped process definition file that cannot be read", async () => {
    const error = new Error("read error");
    (readFileContent as jest.Mock).mockRejectedValue(error);
    await setup();

    dropFiles(new File(["<bad>"], "bad.bpmn"));
    await sleep();

    expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
  });

  it("announces the upload and expands the definition named in the uploaded file", async () => {
    const content = '<definitions><process id="key-a"></process></definitions>';
    (readFileContent as jest.Mock).mockResolvedValue(content);
    (extractBpmnProcessKey as jest.Mock).mockReturnValue("key-a");
    await setup();

    dropFiles(new File([content], "different-filename.bpmn"));
    await sleep();
    await flushRendering();

    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.bpmn.process-definition.upload.success",
      { naam: "different-filename.bpmn" },
    );
    expect(screen.getByText(detailsTitle)).toBeVisible();
  });

  it("asks for confirmation naming the process definition to delete", async () => {
    await setup();

    await user.click(
      screen.getByRole("button", { name: "actie.verwijderen Process A" }),
    );

    const dialogData = dialogOpen.mock.calls[0][1].data;
    expect(dialogData._melding.key).toBe(
      "msg.bpmn.process-definition.delete.confirm",
    );
    expect(dialogData._melding.args).toEqual({ naam: "Process A" });
  });

  it("deletes the process definition once confirmed", async () => {
    await setup();
    dialogOpen.mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(true) }),
    );

    await user.click(
      screen.getByRole("button", { name: "actie.verwijderen Process A" }),
    );
    await sleep();

    expect(bpmnService.deleteProcessDefinition).toHaveBeenCalledWith(
      expect.objectContaining({ key: "key-a", name: "Process A" }),
    );
  });

  it("keeps the process definition when the confirmation is cancelled", async () => {
    await setup();

    await user.click(
      screen.getByRole("button", { name: "actie.verwijderen Process A" }),
    );
    await sleep();

    expect(bpmnService.deleteProcessDefinition).not.toHaveBeenCalled();
  });
});
