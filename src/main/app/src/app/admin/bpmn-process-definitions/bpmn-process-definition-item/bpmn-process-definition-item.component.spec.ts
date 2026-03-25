/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDialog } from "@angular/material/dialog";
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatRowHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { UtilService } from "../../../core/service/util.service";
import { FoutAfhandelingService } from "../../../fout-afhandeling/fout-afhandeling.service";
import { SharedModule } from "../../../shared/shared.module";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BpmnService } from "../../bpmn.service";
import { readFileContent } from "../file.helper";
import { BpmnProcessDefinitionItemComponent } from "./bpmn-process-definition-item.component";

jest.mock("../file.helper");

function makeFileList(...files: File[]): FileList {
  return {
    ...files,
    length: files.length,
    item: (index: number) => files[index] ?? null,
  } as FileList;
}

const flushPromises = (): Promise<void> =>
  new Promise<void>((resolve) => setTimeout(resolve));

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

describe(BpmnProcessDefinitionItemComponent.name, () => {
  let fixture: ComponentFixture<BpmnProcessDefinitionItemComponent>;
  let component: BpmnProcessDefinitionItemComponent;
  let bpmnService: jest.Mocked<BpmnService>;
  let utilService: jest.Mocked<UtilService>;
  let foutAfhandelingService: jest.Mocked<FoutAfhandelingService>;
  let dialogOpenSpy: jest.SpyInstance;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BpmnProcessDefinitionItemComponent,
        SharedModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        {
          provide: BpmnService,
          useValue: {
            uploadProcessDefinitionForm: jest.fn().mockReturnValue(of(null)),
            deleteProcessDefinitionForm: jest.fn().mockReturnValue(of(null)),
          },
        },
        {
          provide: UtilService,
          useValue: { openSnackbar: jest.fn() },
        },
        {
          provide: FoutAfhandelingService,
          useValue: { foutAfhandelen: jest.fn() },
        },
      ],
    }).compileComponents();

    bpmnService = TestBed.inject(BpmnService) as jest.Mocked<BpmnService>;
    utilService = TestBed.inject(UtilService) as jest.Mocked<UtilService>;
    foutAfhandelingService = TestBed.inject(
      FoutAfhandelingService,
    ) as jest.Mocked<FoutAfhandelingService>;

    fixture = TestBed.createComponent(BpmnProcessDefinitionItemComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput("processDefinition", {
      ...baseProcessDefinition,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    loader = TestbedHarnessEnvironment.loader(fixture);

    const internalDialog = (component as unknown as { dialog: MatDialog })
      .dialog;
    dialogOpenSpy = jest
      .spyOn(internalDialog, "open")
      .mockReturnValue({ afterClosed: () => of(false) } as never);
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  describe("process definition metadata", () => {
    it("should display version and key", () => {
      const text: string = fixture.nativeElement.textContent;
      expect(text).toContain("2");
      expect(text).toContain("test-key");
    });

    it("should display documentation", () => {
      expect(fixture.nativeElement.textContent).toContain("Test documentation");
    });
  });

  describe("inUse indicator", () => {
    it("should not show inUse message when inUse is false", async () => {
      const inUseSpans = await loader.getAllHarnesses(
        MatIconHarness.with({
          selector: "div.explanation mat-icon[color=primary]",
        }),
      );
      expect(inUseSpans.length).toBe(0);
    });

    it("should show inUse message when inUse is true", async () => {
      fixture.componentRef.setInput(
        "processDefinition",
        fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
          ...baseProcessDefinition,
          details: { ...baseProcessDefinition.details, inUse: true },
        }),
      );
      fixture.detectChanges();
      await fixture.whenStable();

      const inUseIcon = fixture.nativeElement.querySelector(
        "div.explanation mat-icon[color=primary]",
      );
      expect(inUseIcon).not.toBeNull();
    });
  });

  describe("missing forms warning", () => {
    it("should show warning icon when there are missing forms", async () => {
      const warnIcons = await loader.getAllHarnesses(
        MatIconHarness.with({ selector: 'mat-icon[color="warn"]' }),
      );
      expect(warnIcons.length).toBeGreaterThan(0);
    });

    it("should not show header warning when all forms are uploaded", async () => {
      fixture.componentRef.setInput(
        "processDefinition",
        fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
          ...baseProcessDefinition,
          details: {
            ...baseProcessDefinition.details,
            forms: [uploadedForm],
          },
        }),
      );
      fixture.detectChanges();
      await fixture.whenStable();

      const warnIconsInHeader = await loader.getAllHarnesses(
        MatIconHarness.with({
          selector: '.explanation mat-icon[color="warn"]',
        }),
      );
      expect(warnIconsInHeader.length).toBe(0);
    });
  });

  describe("forms table", () => {
    it("should render a row for each form", async () => {
      const rows = await loader.getAllHarnesses(MatRowHarness);
      expect(rows.length).toBe(2);
    });

    it("should show check_circle icon for uploaded forms", async () => {
      const checkIcons = await loader.getAllHarnesses(
        MatIconHarness.with({ selector: 'mat-icon[color="primary"]' }),
      );
      expect(checkIcons.length).toBeGreaterThan(0);
    });

    it("should show empty-state message when there are no forms", async () => {
      fixture.componentRef.setInput(
        "processDefinition",
        fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
          ...baseProcessDefinition,
          details: { ...baseProcessDefinition.details, forms: [] },
        }),
      );
      fixture.detectChanges();
      await fixture.whenStable();

      expect(fixture.nativeElement.textContent).toContain(
        "msg.geen.gegevens.gevonden",
      );
    });

    it("should render no rows when there are no forms", async () => {
      fixture.componentRef.setInput(
        "processDefinition",
        fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
          ...baseProcessDefinition,
          details: { ...baseProcessDefinition.details, forms: [] },
        }),
      );
      fixture.detectChanges();
      await fixture.whenStable();

      const rows = await loader.getAllHarnesses(MatRowHarness);
      expect(rows.length).toBe(0);
    });

    it("should disable the delete button when inUse is true", async () => {
      fixture.componentRef.setInput(
        "processDefinition",
        fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
          ...baseProcessDefinition,
          details: { ...baseProcessDefinition.details, inUse: true },
        }),
      );
      fixture.detectChanges();
      await fixture.whenStable();

      const deleteBtn: HTMLButtonElement =
        fixture.nativeElement.querySelector("button#delete");
      expect(deleteBtn.disabled).toBe(true);
    });

    it("should enable the delete button when inUse is false", () => {
      const deleteBtn: HTMLButtonElement =
        fixture.nativeElement.querySelector("button#delete");
      expect(deleteBtn.disabled).toBe(false);
    });

    it("should only render a delete button for uploaded forms", async () => {
      const deleteButtons = await loader.getAllHarnesses(
        MatButtonHarness.with({ selector: "button#delete" }),
      );
      // Only the one uploaded form should have a delete button
      expect(deleteButtons.length).toBe(1);
    });
  });

  describe("uploadBpmnForm", () => {
    it("should trigger a click on the hidden file input", () => {
      const fileInput: HTMLInputElement =
        fixture.nativeElement.querySelector('input[type="file"]');
      const clickSpy = jest.spyOn(fileInput, "click");

      component["uploadBpmnForm"]();

      expect(clickSpy).toHaveBeenCalled();
    });
  });

  describe("bpmnFormFileSelected", () => {
    it("should do nothing when no file is provided", () => {
      const event = {
        target: { files: [] },
      } as unknown as Event;

      component["bpmnFormFileSelected"](event);

      expect(bpmnService.uploadProcessDefinitionForm).not.toHaveBeenCalled();
    });

    it("should upload file content and emit bpmnFormListChanged on success", async () => {
      jest.useFakeTimers();
      const fileContent = '{"form": true}';
      (readFileContent as jest.Mock).mockResolvedValue(fileContent);

      const file = new File([fileContent], "test-form.json", {
        type: "application/json",
      });
      const event = {
        target: { files: [file] },
      } as unknown as Event;
      const emitSpy = jest.spyOn(component.bpmnFormListChanged, "emit");

      component["bpmnFormFileSelected"](event);
      await Promise.resolve(); // readFileContent.then
      await Promise.resolve(); // Promise.all resolves
      await Promise.resolve(); // outer .then → forkJoin subscribes

      expect(readFileContent).toHaveBeenCalledWith(file);
      expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalledWith(
        "test-key",
        { filename: "test-form.json", content: fileContent },
      );
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.bpmn.task-forms.upload.success",
        { namen: "test-form.json" },
      );
      jest.runAllTimers();
      expect(emitSpy).toHaveBeenCalled();
    });

    it("should call foutAfhandelingService when readFileContent rejects", async () => {
      const error = new Error("read error");
      (readFileContent as jest.Mock).mockRejectedValue(error);

      const file = new File(["bad"], "bad.json");
      const event = {
        target: { files: [file] },
      } as unknown as Event;

      component["bpmnFormFileSelected"](event);
      await flushPromises();

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
    });
  });

  describe("bpmnFormFilesDropped", () => {
    it("should do nothing when FileList is empty", () => {
      const fileList = makeFileList();

      component["bpmnFormFilesDropped"](fileList);

      expect(bpmnService.uploadProcessDefinitionForm).not.toHaveBeenCalled();
    });

    it("should ignore non-json files", () => {
      const file = new File(["<bpmn/>"], "process.bpmn");
      const fileList = makeFileList(file);

      component["bpmnFormFilesDropped"](fileList);

      expect(bpmnService.uploadProcessDefinitionForm).not.toHaveBeenCalled();
    });

    it("should accept .JSON files (case-insensitive)", async () => {
      const fileContent = '{"form": true}';
      (readFileContent as jest.Mock).mockResolvedValue(fileContent);

      const file = new File([fileContent], "form.JSON");
      const fileList = makeFileList(file);

      jest.useFakeTimers();
      component["bpmnFormFilesDropped"](fileList);
      await Promise.resolve(); // readFileContent.then
      await Promise.resolve(); // Promise.all resolves
      await Promise.resolve(); // outer .then → forkJoin subscribes

      expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalledWith(
        "test-key",
        { filename: "form.JSON", content: fileContent },
      );
    });

    it("should upload dropped files and emit bpmnFormListChanged on success", async () => {
      jest.useFakeTimers();
      const fileContent = '{"form": true}';
      (readFileContent as jest.Mock).mockResolvedValue(fileContent);

      const file = new File([fileContent], "dropped-form.json", {
        type: "application/json",
      });
      const fileList = makeFileList(file);
      const emitSpy = jest.spyOn(component.bpmnFormListChanged, "emit");

      component["bpmnFormFilesDropped"](fileList);
      await Promise.resolve(); // readFileContent.then
      await Promise.resolve(); // Promise.all resolves
      await Promise.resolve(); // outer .then → forkJoin subscribes

      expect(readFileContent).toHaveBeenCalledWith(file);
      expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalledWith(
        "test-key",
        { filename: "dropped-form.json", content: fileContent },
      );
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.bpmn.task-forms.upload.success",
        { namen: "dropped-form.json" },
      );
      jest.runAllTimers();
      expect(emitSpy).toHaveBeenCalled();
    });

    it("should call foutAfhandelingService when readFileContent rejects", async () => {
      const error = new Error("read error");
      (readFileContent as jest.Mock).mockRejectedValue(error);

      const file = new File(["bad"], "bad.json");
      const fileList = makeFileList(file);

      component["bpmnFormFilesDropped"](fileList);
      await flushPromises();

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
    });
  });

  describe("deleteBpmnForm", () => {
    it("should open a confirm dialog with the correct translation key and form name", () => {
      component["deleteBpmnForm"]("form-uploaded");

      const dialogData = dialogOpenSpy.mock.calls[0][1].data;
      expect(dialogData._melding.key).toBe(
        "msg.bpmn.task-forms.delete.confirm",
      );
      expect(dialogData._melding.args).toEqual({ naam: "form-uploaded" });
    });

    it("should pass the deleteProcessDefinitionForm observable to the dialog", () => {
      const deleteObservable = of({});
      bpmnService.deleteProcessDefinitionForm.mockReturnValue(deleteObservable);

      component["deleteBpmnForm"]("form-uploaded");

      const dialogData = dialogOpenSpy.mock.calls[0][1].data;
      expect(dialogData.observable).toBe(deleteObservable);
    });

    it("should show snackbar and emit bpmnFormListChanged when dialog is confirmed", () => {
      dialogOpenSpy.mockReturnValue({ afterClosed: () => of(true) } as never);
      const emitSpy = jest.spyOn(component.bpmnFormListChanged, "emit");

      component["deleteBpmnForm"]("form-uploaded");

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.bpmn.task-forms.deleted",
        { namen: "form-uploaded" },
      );
      expect(emitSpy).toHaveBeenCalled();
    });

    it("should not show snackbar or emit when dialog is cancelled", () => {
      const emitSpy = jest.spyOn(component.bpmnFormListChanged, "emit");

      component["deleteBpmnForm"]("form-uploaded");

      expect(utilService.openSnackbar).not.toHaveBeenCalled();
      expect(emitSpy).not.toHaveBeenCalled();
    });
  });

  describe("deleteAllOrphanedForms", () => {
    beforeEach(() => {
      fixture.componentRef.setInput(
        "processDefinition",
        fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
          ...baseProcessDefinition,
          details: {
            ...baseProcessDefinition.details,
            orphanedForms: [orphanedForm],
          },
        }),
      );
      fixture.detectChanges();
    });

    it("should call deleteProcessDefinitionForm for each orphaned form", () => {
      component["deleteAllOrphanedForms"]();

      expect(bpmnService.deleteProcessDefinitionForm).toHaveBeenCalledWith(
        "test-key",
        "form-orphaned",
      );
    });

    it("should show snackbar and emit bpmnFormListChanged after all deletions", () => {
      const emitSpy = jest.spyOn(component.bpmnFormListChanged, "emit");

      component["deleteAllOrphanedForms"]();

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.bpmn.task-forms.deleted",
        { namen: "form-orphaned" },
      );
      expect(emitSpy).toHaveBeenCalled();
    });
  });

  describe("orphaned forms section", () => {
    it("should not render the orphaned forms section when the list is empty", () => {
      const chipSet = fixture.nativeElement.querySelector("mat-chip-set");
      expect(chipSet).toBeNull();
    });

    it("should render a chip for each orphaned form", async () => {
      fixture.componentRef.setInput(
        "processDefinition",
        fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
          ...baseProcessDefinition,
          details: {
            ...baseProcessDefinition.details,
            orphanedForms: [orphanedForm],
          },
        }),
      );
      fixture.detectChanges();
      await fixture.whenStable();

      expect(fixture.nativeElement.textContent).toContain("form-orphaned");
    });
  });
});
