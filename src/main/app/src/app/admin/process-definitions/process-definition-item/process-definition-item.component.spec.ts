/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
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
import { ProcessDefinitionItemComponent } from "./process-definition-item.component";

jest.mock("../file.helper");

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

describe(ProcessDefinitionItemComponent.name, () => {
  let fixture: ComponentFixture<ProcessDefinitionItemComponent>;
  let component: ProcessDefinitionItemComponent;
  let bpmnService: jest.Mocked<BpmnService>;
  let utilService: jest.Mocked<UtilService>;
  let foutAfhandelingService: jest.Mocked<FoutAfhandelingService>;
  let dialogOpenSpy: jest.SpyInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ProcessDefinitionItemComponent,
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

    fixture = TestBed.createComponent(ProcessDefinitionItemComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput("processDefinition", {
      ...baseProcessDefinition,
    });
    fixture.detectChanges();
    await fixture.whenStable();

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
    it("should not show inUse message when inUse is false", () => {
      const inUseSpans: NodeList = fixture.nativeElement.querySelectorAll(
        "span.icon-align mat-icon[color=primary]",
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
        "span.icon-align mat-icon[color=primary]",
      );
      expect(inUseIcon).not.toBeNull();
    });
  });

  describe("missing forms warning", () => {
    it("should show warning icon when there are missing forms", () => {
      const warnIcons: NodeList = fixture.nativeElement.querySelectorAll(
        'mat-icon[color="warn"]',
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

      const warnIconsInHeader: NodeList =
        fixture.nativeElement.querySelectorAll(
          '.icon-align mat-icon[color="warn"]',
        );
      expect(warnIconsInHeader.length).toBe(0);
    });
  });

  describe("forms table", () => {
    it("should render a row for each form", () => {
      const rows: NodeList =
        fixture.nativeElement.querySelectorAll("tr[mat-row]");
      expect(rows.length).toBe(2);
    });

    it("should show check_circle icon for uploaded forms", () => {
      const checkIcons: NodeList = fixture.nativeElement.querySelectorAll(
        'mat-icon[color="primary"]',
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

      const rows: NodeList =
        fixture.nativeElement.querySelectorAll("tr[mat-row]");
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

    it("should only render a delete button for uploaded forms", () => {
      const deleteButtons: NodeList =
        fixture.nativeElement.querySelectorAll("button#delete");
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
      await Promise.resolve(); // MT1: inner .then(content => ...) runs
      await Promise.resolve(); // MT2: Promise.all internal resolution
      await Promise.resolve(); // MT3: outer .then runs → forkJoin subscribes

      expect(readFileContent).toHaveBeenCalledWith(file);
      expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalledWith(
        "test-key",
        { filename: "test-form.json", content: fileContent },
      );
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.formioformulier.uploaden.uitgevoerd",
        { naam: "test-form.json" },
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
      // Rejection propagates through four microtask levels via Promise.all:
      // MT1: inner .then pass-through; MT2: Promise.all rejects;
      // MT3: outer .then pass-through; MT4: .catch() runs
      await Promise.resolve();
      await Promise.resolve();
      await Promise.resolve();
      await Promise.resolve();

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
    });
  });

  describe("deleteBpmnForm", () => {
    it("should open a confirm dialog with the correct translation key and form name", () => {
      component["deleteBpmnForm"]("form-uploaded");

      const dialogData = dialogOpenSpy.mock.calls[0][1].data;
      expect(dialogData._melding.key).toBe(
        "msg.formioformulier.verwijderen.bevestigen",
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
        "msg.formioformulier.verwijderen.uitgevoerd",
        { naam: "form-uploaded" },
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

  describe("deleteOrphanedForm", () => {
    it("should call deleteProcessDefinitionForm with the correct key and form name", () => {
      component["deleteOrphanedForm"]("orphan-form");

      expect(bpmnService.deleteProcessDefinitionForm).toHaveBeenCalledWith(
        "test-key",
        "orphan-form",
      );
    });

    it("should show snackbar and emit bpmnFormListChanged after deletion", () => {
      const emitSpy = jest.spyOn(component.bpmnFormListChanged, "emit");

      component["deleteOrphanedForm"]("orphan-form");

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.formioformulier.verwijderen.uitgevoerd",
        { naam: "orphan-form" },
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

      const chips: NodeList =
        fixture.nativeElement.querySelectorAll("mat-chip");
      expect(chips.length).toBe(1);
      expect(fixture.nativeElement.textContent).toContain("form-orphaned");
    });

    it("should call deleteOrphanedForm when a chip's remove button is clicked", async () => {
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

      const deleteOrphanedSpy = jest.spyOn(
        component as ProcessDefinitionItemComponent & {
          deleteOrphanedForm: (key: string) => void;
        },
        "deleteOrphanedForm",
      );
      const removeButton: HTMLButtonElement =
        fixture.nativeElement.querySelector("button[matChipRemove]");
      removeButton.click();

      expect(deleteOrphanedSpy).toHaveBeenCalledWith("form-orphaned");
    });
  });
});
