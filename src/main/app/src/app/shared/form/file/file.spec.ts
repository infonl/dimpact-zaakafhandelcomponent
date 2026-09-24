/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  AbstractControl,
  FormControl,
  FormGroup,
  Validators,
} from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { waitFor } from "@testing-library/angular";
import { of } from "rxjs";
import { ConfiguratieService } from "src/app/configuratie/configuratie.service";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../setupJest";
import { GeneratedType } from "../../utils/generated-types";
import { ZacFile } from "./file";

const ALLOWED_FILE_TYPES_QUERY_KEY = ["/rest/configuratie/file-types"];

interface TestForm extends Record<string, AbstractControl> {
  document: FormControl<File | null>;
  attachment: FormControl<File | null>;
  requiredDocument: FormControl<File | null>;
}

// These tests are not perfect as they are calling internal (protected) methods -- e.g., `component['selectedFile']`
// It is (near) impossible to mock the actual file uploading so we fake it in these tests
describe(ZacFile.name, () => {
  let component: ZacFile<TestForm, keyof TestForm>;
  let componentRef: ComponentRef<typeof component>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;
  let translateService: TranslateService;
  let configuratieService: ConfiguratieService;

  const createTestForm = () => {
    return new FormGroup<TestForm>({
      document: new FormControl<File | null>(null, { nonNullable: true }),
      attachment: new FormControl<File | null>(null, { nonNullable: true }),
      requiredDocument: new FormControl<File | null>(null, {
        nonNullable: true,
      }),
    });
  };

  const createMockFile = (
    name: string,
    size: number,
    type: string = "text/plain",
  ): File => {
    const file = new File(["test content"], name, { type });
    Object.defineProperty(file, "size", { value: size });
    return file;
  };

  const loadAllowedFileTypes = async () => {
    fixture.detectChanges();
    await waitFor(() =>
      expect(component["allowedFileTypesQuery"].isPending()).toBe(false),
    );
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZacFile, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        TranslateService,
        provideHttpClient(),
        provideTanStackQuery(testQueryClient),
      ],
    }).compileComponents();

    translateService = TestBed.inject(TranslateService);

    configuratieService = TestBed.inject(ConfiguratieService);
    jest
      .spyOn(configuratieService, "readMaxFileSizeMB")
      .mockReturnValue(of(10));
    jest
      .spyOn(configuratieService, "readAllowedFileTypesQuery")
      .mockReturnValue(
        fromPartial({
          queryKey: ALLOWED_FILE_TYPES_QUERY_KEY,
          queryFn: () =>
            Promise.resolve([
              { extension: ".txt", mediaType: "text/plain" },
              { extension: ".pdf", mediaType: "application/pdf" },
            ]),
        }),
      );

    fixture = TestBed.createComponent(ZacFile<TestForm, keyof TestForm>);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("Basic functionality", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should display the form field", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      expect(formField).toBeTruthy();
    });

    it("should display the input field", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(input).toBeTruthy();
    });

    it("should be readonly by default", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isReadonly()).toBe(true);
    });
  });

  describe("Label display", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      translateService.setTranslation("en", {
        document: "Test document label",
      });
      translateService.use("en");
      fixture.detectChanges();
    });

    it("should display translated key as label when no label is provided", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      const label = await formField.getLabel();
      expect(label).toBe("Test document label");
    });

    it("should display custom label when provided", async () => {
      componentRef.setInput("label", "Custom Document Label");
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const label = await formField.getLabel();
      expect(label).toBe("Custom Document Label");
    });
  });

  describe("File selection", () => {
    beforeEach(async () => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      await loadAllowedFileTypes();
    });

    it("should handle file selection via input change event", async () => {
      const mockFile = createMockFile("test.txt", 1024);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [mockFile],
        }),
      });

      await component["selectedFile"](mockEvent);

      expect(component.form().controls.document.value).toBe(mockFile);
    });

    it("should handle file drop", async () => {
      const mockFile = createMockFile("test.txt", 1024);
      const mockFileList = [mockFile] as unknown as FileList;

      await component["droppedFile"](mockFileList);

      expect(component.form().controls.document.value).toBe(mockFile);
    });

    it("should not process empty file list", () => {
      const emptyFileList = [] as unknown as FileList;
      const initialValue = component.form().controls.document.value;

      component["droppedFile"](emptyFileList);

      expect(component.form().controls.document.value).toBe(initialValue);
    });
  });

  describe("File validation", () => {
    beforeEach(async () => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      await loadAllowedFileTypes();
    });

    it("should validate file type", async () => {
      componentRef.setInput("allowedFileTypes", [".txt", ".pdf"]);
      fixture.detectChanges();
      await fixture.whenStable();
      const invalidFile = createMockFile("test.doc", 1024);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [invalidFile],
        }),
      });

      await component["selectedFile"](mockEvent);

      expect(component.form().controls.document.errors).toEqual({
        fileTypeInvalid: { type: "doc" },
      });
    });

    it("should validate file size", async () => {
      componentRef.setInput("maxFileSizeMB", 1);
      const largeFile = createMockFile("test.txt", 2 * 1024 * 1024); // 2MB
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [largeFile],
        }),
      });

      await component["selectedFile"](mockEvent);

      expect(component.form().controls.document.errors).toEqual({
        fileTooLarge: { size: 2 },
      });
    });

    it("should validate empty file", async () => {
      const emptyFile = createMockFile("test.txt", 0);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [emptyFile],
        }),
      });

      await component["selectedFile"](mockEvent);

      expect(component.form().controls.document.errors).toEqual({
        fileEmpty: true,
      });
    });

    it("should accept valid file", async () => {
      componentRef.setInput("allowedFileTypes", [".txt"]);
      componentRef.setInput("maxFileSizeMB", 5);
      const validFile = createMockFile("test.txt", 1024);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [validFile],
        }),
      });

      await component["selectedFile"](mockEvent);

      expect(component.form().controls.document.value).toBe(validFile);
      expect(component.form().controls.document.errors).toBeNull();
    });
  });

  describe("File reset functionality", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should reset file when reset button is clicked", async () => {
      // First set a file
      const mockFile = createMockFile("test.txt", 1024);
      component.form().controls.document.setValue(mockFile);
      fixture.detectChanges();

      // Find and click the reset button
      const resetButton = await loader.getHarness(MatButtonHarness);
      await resetButton.click();
      fixture.detectChanges();

      expect(component.form().controls.document.value).toBeNull();
    });

    it("should clear display control when file is reset", async () => {
      const mockFile = createMockFile("test.txt", 1024);
      component.form().controls.document.setValue(mockFile);
      fixture.detectChanges();

      const resetButton = await loader.getHarness(MatButtonHarness);
      await resetButton.click();
      fixture.detectChanges();

      expect(component["displayControl"].value).toBeNull();
    });
  });

  describe("Button visibility", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should show upload button when no file is selected", async () => {
      const uploadButton = await loader.getHarness(MatButtonHarness);
      expect(uploadButton).toBeTruthy();
    });

    it("should show delete button when file is selected", async () => {
      const mockFile = createMockFile("test.txt", 1024);
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      component.form().controls.document.setValue(mockFile);
      fixture.detectChanges();

      const deleteButton = await loader.getHarness(MatButtonHarness);
      expect(deleteButton).toBeTruthy();
    });
  });

  describe("Required field validation", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "requiredDocument");
      component.ngOnInit();
      component
        .form()
        .controls.requiredDocument.addValidators(Validators.required);
      fixture.detectChanges();
    });

    it("should indicate required field", () => {
      expect(component["isRequired"]()).toBe(true);
    });

    it("should show required attribute on input", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isRequired()).toBe(true);
    });
  });

  describe("File type restrictions", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should accept file with allowed extension", async () => {
      componentRef.setInput("allowedFileTypes", [".txt", ".pdf"]);
      fixture.detectChanges();
      const validFile = createMockFile("document.txt", 1024);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [validFile],
        }),
      });

      await component["selectedFile"](mockEvent);

      expect(component.form().controls.document.value).toBe(validFile);
      expect(component.form().controls.document.errors).toBeNull();
    });
  });

  describe("File size restrictions", () => {
    beforeEach(async () => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      await loadAllowedFileTypes();
    });

    it("should accept file within size limit", async () => {
      componentRef.setInput("maxFileSizeMB", 2);
      fixture.detectChanges();
      const validFile = createMockFile("test.txt", 1024 * 1024); // 1MB
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [validFile],
        }),
      });

      await component["selectedFile"](mockEvent);

      expect(component.form().controls.document.value).toBe(validFile);
      expect(component.form().controls.document.errors).toBeNull();
    });
  });

  describe("Display control updates", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should update display control with file name without extension", () => {
      const mockFile = createMockFile("test-document.txt", 1024);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [mockFile],
        }),
      });

      component["selectedFile"](mockEvent);

      expect(component["displayControl"].value).toBe("test-document");
    });

    it("should clear display control when file is reset", async () => {
      const mockFile = createMockFile("test.txt", 1024);
      component.form().controls.document.setValue(mockFile);
      component["displayControl"].setValue("test");

      const resetButton = await loader.getHarness(
        MatButtonHarness.with({ text: "delete" }),
      );
      await resetButton.click();

      expect(component["displayControl"].value).toBeNull();
    });
  });

  describe("Component lifecycle", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should handle destroy when no subscription exists", () => {
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe("Readonly mode", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should be readonly when readonly input is true", async () => {
      componentRef.setInput("readonly", true);
      fixture.detectChanges();
      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isReadonly()).toBe(true);
    });
  });

  describe("Hint display", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      componentRef.setInput("maxFileSizeMB", 5);
      componentRef.setInput("allowedFileTypes", [".txt", ".pdf"]);
      fixture.detectChanges();
      translateService.setTranslation("en", {
        "form.input.file.hint.max-size": "Max size: {{sizeInMB}}MB",
        "form.input.file.hint.formats": "Formats: {{formats}}",
      });
      translateService.use("en");
      fixture.detectChanges();
    });

    it("should display hint with file size and formats", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      const [hint] = await formField.getTextHints();
      expect(hint).toContain("Max size: 5MB");
      expect(hint).toContain("Formats: .txt, .pdf");
    });
  });

  describe("While the allowed file types are loading", () => {
    beforeEach(() => {
      jest
        .spyOn(configuratieService, "readAllowedFileTypesQuery")
        .mockReturnValue(
          fromPartial({
            queryKey: ALLOWED_FILE_TYPES_QUERY_KEY,
            queryFn: () =>
              new Promise<GeneratedType<"RestAllowedFileType">[]>(() => {}),
          }),
        );

      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      componentRef.setInput("maxFileSizeMB", 5);
      component.ngOnInit();
      translateService.setTranslation("en", {
        "form.input.file.hint.max-size": "Max size: {{sizeInMB}}MB",
        "form.input.file.hint.formats": "Formats: {{formats}}",
        "form.input.file.hint.formats-loading": "Loading formats",
      });
      translateService.use("en");
      fixture.detectChanges();
    });

    it("should disable the field", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isDisabled()).toBe(true);
    });

    it("should tell the user the allowed formats are being loaded", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      const [hint] = await formField.getTextHints();
      expect(hint).toContain("Loading formats");
      expect(hint).not.toContain("Formats:");
    });

    it("should ignore a dropped file", async () => {
      await component["droppedFile"]([
        createMockFile("test.txt", 1024),
      ] as unknown as FileList);

      expect(component.form().controls.document.value).toBeNull();
    });

    it("should not open the file picker", () => {
      const fileInput = component["fileInput"]()!.nativeElement;
      const openPicker = jest.spyOn(fileInput, "click");

      component["openFilePicker"]();

      expect(openPicker).not.toHaveBeenCalled();
    });
  });

  describe("When the allowed file types failed to load", () => {
    beforeEach(async () => {
      jest
        .spyOn(configuratieService, "readAllowedFileTypesQuery")
        .mockReturnValue(
          fromPartial({
            queryKey: ALLOWED_FILE_TYPES_QUERY_KEY,
            queryFn: () => Promise.reject(new Error("fakeNetworkFailure")),
          }),
        );

      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      componentRef.setInput("maxFileSizeMB", 5);
      component.ngOnInit();
      translateService.setTranslation("en", {
        "form.input.file.hint.max-size": "Max size: {{sizeInMB}}MB",
        "form.input.file.hint.formats-loading": "Loading formats",
        "form.input.file.hint.formats-unavailable": "Formats unavailable",
      });
      translateService.use("en");
      await loadAllowedFileTypes();
    });

    it("should keep the field disabled", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isDisabled()).toBe(true);
    });

    it("should tell the user the allowed formats could not be loaded", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      const [hint] = await formField.getTextHints();
      expect(hint).toContain("Formats unavailable");
    });

    it("should ignore a dropped file", async () => {
      await component["droppedFile"]([
        createMockFile("test.txt", 1024),
      ] as unknown as FileList);

      expect(component.form().controls.document.value).toBeNull();
    });
  });

  describe("Once the allowed file types have loaded", () => {
    beforeEach(async () => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      translateService.setTranslation("en", {
        "validators.fileTypeInvalid": "Type {{type}} is not allowed",
      });
      translateService.use("en");
      await loadAllowedFileTypes();
    });

    it("should enable the field", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isDisabled()).toBe(false);
    });

    it("should show an error for a dropped file of a disallowed type", async () => {
      await component["droppedFile"]([
        createMockFile("test.exe", 1024),
      ] as unknown as FileList);
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      expect(await formField.getTextErrors()).toEqual([
        "Type exe is not allowed",
      ]);
    });
  });

  describe("Removing the chosen file", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should not reopen the file picker", async () => {
      component
        .form()
        .controls.document.setValue(createMockFile("a.txt", 1024));
      fixture.detectChanges();
      const fileInput = component["fileInput"]()!.nativeElement;
      const openPicker = jest.spyOn(fileInput, "click");

      const deleteButton = await loader.getHarness(
        MatButtonHarness.with({ text: "delete" }),
      );
      await deleteButton.click();

      expect(openPicker).not.toHaveBeenCalled();
    });
  });

  describe("Dismissing the operating system file picker", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "document");
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should keep the native cancel event from reaching the surrounding form", () => {
      const fileInput = component["fileInput"]()!.nativeElement;
      const cancelListener = jest.fn();
      fixture.nativeElement.addEventListener("cancel", cancelListener);

      fileInput.dispatchEvent(new Event("cancel", { bubbles: true }));

      expect(cancelListener).not.toHaveBeenCalled();
    });
  });
});
