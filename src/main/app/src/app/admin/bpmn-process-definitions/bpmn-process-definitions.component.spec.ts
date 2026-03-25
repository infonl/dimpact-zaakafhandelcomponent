/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideAngularQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { SharedModule } from "../../shared/shared.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BpmnService } from "../bpmn.service";
import { BpmnProcessDefinitionsComponent } from "./bpmn-process-definitions.component";
import { readFileContent } from "./file.helper";

jest.mock("./file.helper");

function makeFileList(...files: File[]): FileList {
  return {
    ...files,
    length: files.length,
    item: (index: number) => files[index] ?? null,
  } as FileList;
}

const flushPromises = (): Promise<void> =>
  new Promise<void>((resolve) => setTimeout(resolve));

const baseProcessDefinition = fromPartial<
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

describe(BpmnProcessDefinitionsComponent.name, () => {
  let fixture: ComponentFixture<BpmnProcessDefinitionsComponent>;
  let component: BpmnProcessDefinitionsComponent;
  let bpmnService: jest.Mocked<BpmnService>;
  let utilService: jest.Mocked<UtilService>;
  let foutAfhandelingService: jest.Mocked<FoutAfhandelingService>;
  let dialogOpenSpy: jest.SpyInstance;
  let queryClient: QueryClient;
  let deleteMutationFn: jest.Mock;

  beforeEach(async () => {
    notifyManager.setScheduler((fn) => fn());

    deleteMutationFn = jest.fn().mockResolvedValue({});

    await TestBed.configureTestingModule({
      imports: [
        BpmnProcessDefinitionsComponent,
        SharedModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        provideAngularQuery(
          new QueryClient({ defaultOptions: { queries: { retry: false } } }),
        ),
        {
          provide: BpmnService,
          useValue: {
            listProcessDefinitionsQuery: jest.fn().mockReturnValue({
              queryKey: ["/rest/bpmn-process-definitions"],
              queryFn: () => Promise.resolve([baseProcessDefinition]),
            }),
            uploadProcessDefinitionQuery: jest.fn().mockReturnValue({
              mutationFn: jest.fn().mockResolvedValue({}),
            }),
            deleteProcessDefinition: deleteMutationFn,
            uploadProcessDefinitionForm: jest.fn().mockReturnValue(of({})),
            deleteProcessDefinitionForm: jest.fn().mockReturnValue(of({})),
          },
        },
        {
          provide: UtilService,
          useValue: {
            setLoading: jest.fn(),
            setTitle: jest.fn(),
            openSnackbar: jest.fn(),
          },
        },
        {
          provide: ConfiguratieService,
          useValue: {},
        },
        {
          provide: FoutAfhandelingService,
          useValue: { foutAfhandelen: jest.fn() },
        },
      ],
    }).compileComponents();

    queryClient = TestBed.inject(QueryClient);
    bpmnService = TestBed.inject(BpmnService) as jest.Mocked<BpmnService>;
    utilService = TestBed.inject(UtilService) as jest.Mocked<UtilService>;
    foutAfhandelingService = TestBed.inject(
      FoutAfhandelingService,
    ) as jest.Mocked<FoutAfhandelingService>;

    fixture = TestBed.createComponent(BpmnProcessDefinitionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    dialogOpenSpy = jest
      .spyOn((component as unknown as { dialog: MatDialog }).dialog, "open")
      .mockReturnValue({ afterClosed: () => of(false) } as never);
  });

  afterEach(() => {
    notifyManager.setScheduler((fn) => setTimeout(fn, 0));
    jest.clearAllMocks();
  });

  describe("on init", () => {
    it("should call listProcessDefinitionsQuery", () => {
      expect(bpmnService.listProcessDefinitionsQuery).toHaveBeenCalled();
    });

    it("should render a group row per process definition", () => {
      const rows: NodeList =
        fixture.nativeElement.querySelectorAll(".tree-group-row");
      expect(rows.length).toBe(1);
    });

    it("should show the process definition name", () => {
      expect(fixture.nativeElement.textContent).toContain("Process A");
    });

    it("should show empty-state message when there are no definitions", () => {
      queryClient.setQueryData(["/rest/bpmn-process-definitions"], []);
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain(
        "msg.geen.gegevens.gevonden",
      );
    });
  });

  describe("toggleNode", () => {
    it("should expand a node on first click", () => {
      fixture.nativeElement.querySelector(".tree-group-row").click();
      expect(component["expandedKey"]).toBe("key-a");
    });

    it("should collapse an already-expanded node on second click", () => {
      const row: HTMLElement =
        fixture.nativeElement.querySelector(".tree-group-row");
      row.click();
      row.click();
      expect(component["expandedKey"]).toBeNull();
    });
  });

  describe("hasAllFormsUploaded", () => {
    it("should return true when all forms are uploaded", () => {
      expect(
        component["hasAllFormsUploaded"]({
          name: "",
          key: "",
          definition: baseProcessDefinition,
        }),
      ).toBe(true);
    });

    it("should return false when a form is not uploaded", () => {
      expect(
        component["hasAllFormsUploaded"]({
          name: "",
          key: "",
          definition: fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
            ...baseProcessDefinition,
            details: {
              ...baseProcessDefinition.details,
              forms: [{ formKey: "f1", title: "Form 1", uploaded: false }],
            },
          }),
        }),
      ).toBe(false);
    });

    it("should return false when there are no forms", () => {
      expect(
        component["hasAllFormsUploaded"]({
          name: "",
          key: "",
          definition: fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
            ...baseProcessDefinition,
            details: { ...baseProcessDefinition.details, forms: [] },
          }),
        }),
      ).toBe(false);
    });
  });

  describe("selectBpmnProcessDefinitionFile", () => {
    it("should trigger a click on the hidden file input", () => {
      const clickSpy = jest.spyOn(
        component["bpmnProcessDefinitionFileInput"].nativeElement,
        "click",
      );

      component["selectBpmnProcessDefinitionFile"]();

      expect(clickSpy).toHaveBeenCalled();
    });
  });

  describe("bpmnProcessDefinitionFileSelected", () => {
    it("should do nothing when target is not an HTMLInputElement", () => {
      component["bpmnProcessDefinitionFileSelected"]({
        target: {},
      } as unknown as Event);
      expect(readFileContent).not.toHaveBeenCalled();
    });

    it("should upload file content on success", async () => {
      const fileContent = "<bpmn/>";
      (readFileContent as jest.Mock).mockResolvedValue(fileContent);

      const file = new File([fileContent], "process.bpmn");
      const input = document.createElement("input");
      Object.defineProperty(input, "files", { value: [file] });

      const mutateMock = jest.fn();
      Object.defineProperty(component, "uploadMutation", {
        value: { mutate: mutateMock },
        writable: true,
      });

      component["bpmnProcessDefinitionFileSelected"]({
        target: input,
      } as unknown as Event);
      await flushPromises();

      expect(readFileContent).toHaveBeenCalledWith(file);
      expect(mutateMock).toHaveBeenCalledWith(
        { filename: "process.bpmn", content: fileContent },
        expect.objectContaining({ onSuccess: expect.any(Function) }),
      );
    });

    it("should reset input value after file selection so re-uploading the same file works", () => {
      const input = document.createElement("input");
      Object.defineProperty(input, "files", {
        value: [new File(["<bpmn/>"], "process.bpmn")],
      });

      component["bpmnProcessDefinitionFileSelected"]({
        target: input,
      } as unknown as Event);

      expect(input.value).toBe("");
    });

    it("should call foutAfhandelingService when readFileContent rejects", async () => {
      const error = new Error("read error");
      (readFileContent as jest.Mock).mockRejectedValue(error);

      const input = document.createElement("input");
      Object.defineProperty(input, "files", {
        value: [new File(["<bad>"], "bad.bpmn")],
      });

      component["bpmnProcessDefinitionFileSelected"]({
        target: input,
      } as unknown as Event);
      await flushPromises();

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
    });
  });

  describe("bpmnProcessDefinitionFileDropped", () => {
    it("should upload the first dropped file", async () => {
      const fileContent = "<bpmn/>";
      (readFileContent as jest.Mock).mockResolvedValue(fileContent);

      const file = new File([fileContent], "dropped.bpmn");
      const fileList = makeFileList(file);

      const mutateMock = jest.fn();
      Object.defineProperty(component, "uploadMutation", {
        value: { mutate: mutateMock },
        writable: true,
      });

      component["bpmnProcessDefinitionFileDropped"](fileList);
      await flushPromises();

      expect(readFileContent).toHaveBeenCalledWith(file);
      expect(mutateMock).toHaveBeenCalledWith(
        { filename: "dropped.bpmn", content: fileContent },
        expect.objectContaining({ onSuccess: expect.any(Function) }),
      );
    });

    it("should do nothing when FileList is empty", () => {
      const fileList = makeFileList();

      component["bpmnProcessDefinitionFileDropped"](fileList);

      expect(readFileContent).not.toHaveBeenCalled();
    });

    it("should ignore non-bpmn files", () => {
      const file = new File(["{}"], "form.json");
      const fileList = makeFileList(file);

      component["bpmnProcessDefinitionFileDropped"](fileList);

      expect(readFileContent).not.toHaveBeenCalled();
    });

    it("should accept .BPMN files (case-insensitive)", async () => {
      const fileContent = "<bpmn/>";
      (readFileContent as jest.Mock).mockResolvedValue(fileContent);

      const file = new File([fileContent], "process.BPMN");
      const fileList = makeFileList(file);

      const mutateMock = jest.fn();
      Object.defineProperty(component, "uploadMutation", {
        value: { mutate: mutateMock },
        writable: true,
      });

      component["bpmnProcessDefinitionFileDropped"](fileList);
      await flushPromises();

      expect(mutateMock).toHaveBeenCalledWith(
        { filename: "process.BPMN", content: fileContent },
        expect.objectContaining({ onSuccess: expect.any(Function) }),
      );
    });

    it("should call foutAfhandelingService when readFileContent rejects", async () => {
      const error = new Error("read error");
      (readFileContent as jest.Mock).mockRejectedValue(error);

      const file = new File(["<bad>"], "bad.bpmn");
      const fileList = makeFileList(file);

      component["bpmnProcessDefinitionFileDropped"](fileList);
      await flushPromises();

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
    });
  });

  describe("deleteProcessDefinition", () => {
    it("should open a confirm dialog with the correct translation key and name", () => {
      component["deleteProcessDefinition"]({ key: "key-a", name: "Process A" });

      const dialogData = dialogOpenSpy.mock.calls[0][1].data;
      expect(dialogData._melding.key).toBe(
        "msg.bpmn-procesdefinitie.verwijderen.bevestigen",
      );
      expect(dialogData._melding.args).toEqual({ naam: "Process A" });
    });

    it("should call delete mutation and show snackbar when dialog is confirmed", async () => {
      dialogOpenSpy.mockReturnValue({ afterClosed: () => of(true) } as never);

      component["deleteProcessDefinition"]({ key: "key-a", name: "Process A" });
      await fixture.whenStable();

      expect(deleteMutationFn).toHaveBeenCalledWith("key-a");
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.bpmn-procesdefinitie.verwijderen.uitgevoerd",
        { naam: "Process A" },
      );
    });

    it("should not show snackbar when dialog is cancelled", () => {
      component["deleteProcessDefinition"]({ key: "key-a", name: "Process A" });
      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });
  });

  describe("refreshDefinitions", () => {
    it("should not throw when called", () => {
      expect(() => component["refreshDefinitions"]()).not.toThrow();
    });
  });

  describe("asProcessDefinition", () => {
    it("should return the node cast as a process definition", () => {
      const result = component["asProcessDefinition"](baseProcessDefinition);
      expect(result).toBe(baseProcessDefinition);
    });
  });
});
