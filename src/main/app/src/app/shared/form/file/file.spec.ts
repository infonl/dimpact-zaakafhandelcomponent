/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { FileDragAndDropDirective } from "../../directives/file-drag-and-drop.directive";
import { MaterialFormBuilderModule } from "../../material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../material/material.module";
import { PipesModule } from "../../pipes/pipes.module";
import { ZacFile } from "./file";

interface TestForm extends Record<string, AbstractControl> {
  document: FormControl<File | null>;
  attachment: FormControl<File | null>;
  requiredDocument: FormControl<File | null>;
}

// These tests are not perfect as they are calling internal (protected) methods -- e.g., `component['selectedFile']`
// It is (near) impossible to mock the actual file uploading so we fake it in these tests
describe(ZacFile.name, () => {
  let component: ZacFile<TestForm, keyof TestForm>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;
  let translateService: TranslateService;

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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ZacFile],
      imports: [
        ReactiveFormsModule,
        MaterialModule,
        TranslateModule.forRoot(),
        PipesModule,
        FileDragAndDropDirective,
        MaterialFormBuilderModule,
        NoopAnimationsModule,
      ],
      providers: [TranslateService],
    }).compileComponents();

    translateService = TestBed.inject(TranslateService);
    fixture = TestBed.createComponent(ZacFile<TestForm, keyof TestForm>);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("Basic functionality", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should create", () => {
      expect(component).toBeTruthy();
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
      component.form = createTestForm();
      component.key = "document";
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
      component.label = "Custom Document Label";
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const label = await formField.getLabel();
      expect(label).toBe("Custom Document Label");
    });
  });

  describe("File selection", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should handle file selection via input change event", () => {
      const mockFile = createMockFile("test.txt", 1024);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [mockFile],
        }),
      });

      component["selectedFile"](mockEvent);

      expect(component.form.controls.document.value).toBe(mockFile);
    });

    it("should handle file drop", () => {
      const mockFile = createMockFile("test.txt", 1024);
      const mockFileList = [mockFile] as unknown as FileList;

      component["droppedFile"](mockFileList);

      expect(component.form.controls.document.value).toBe(mockFile);
    });

    it("should not process empty file list", () => {
      const emptyFileList = [] as unknown as FileList;
      const initialValue = component.form.controls.document.value;

      component["droppedFile"](emptyFileList);

      expect(component.form.controls.document.value).toBe(initialValue);
    });
  });

  describe("File validation", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should validate file type", async () => {
      component.allowedFileTypes = [".txt", ".pdf"];
      const invalidFile = createMockFile("test.doc", 1024);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [invalidFile],
        }),
      });

      component["selectedFile"](mockEvent);

      expect(component.form.controls.document.errors).toEqual({
        fileTypeInvalid: { type: "doc" },
      });
    });

    it("should validate file size", () => {
      component.maxFileSizeMB = 1;
      const largeFile = createMockFile("test.txt", 2 * 1024 * 1024); // 2MB
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [largeFile],
        }),
      });

      component["selectedFile"](mockEvent);

      expect(component.form.controls.document.errors).toEqual({
        fileTooLarge: { size: 2 },
      });
    });

    it("should validate empty file", () => {
      const emptyFile = createMockFile("test.txt", 0);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [emptyFile],
        }),
      });

      component["selectedFile"](mockEvent);

      expect(component.form.controls.document.errors).toEqual({
        fileEmpty: true,
      });
    });

    it("should accept valid file", () => {
      component.allowedFileTypes = [".txt"];
      component.maxFileSizeMB = 5;
      const validFile = createMockFile("test.txt", 1024);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [validFile],
        }),
      });

      component["selectedFile"](mockEvent);

      expect(component.form.controls.document.value).toBe(validFile);
      expect(component.form.controls.document.errors).toBeNull();
    });
  });

  describe("File reset functionality", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should reset file when reset button is clicked", async () => {
      // First set a file
      const mockFile = createMockFile("test.txt", 1024);
      component.form.controls.document.setValue(mockFile);
      fixture.detectChanges();

      // Find and click the reset button
      const resetButton = await loader.getHarness(MatButtonHarness);
      await resetButton.click();
      fixture.detectChanges();

      expect(component.form.controls.document.value).toBeNull();
    });

    it("should clear display control when file is reset", async () => {
      const mockFile = createMockFile("test.txt", 1024);
      component.form.controls.document.setValue(mockFile);
      fixture.detectChanges();

      const resetButton = await loader.getHarness(MatButtonHarness);
      await resetButton.click();
      fixture.detectChanges();

      expect(component["displayControl"].value).toBeNull();
    });
  });

  describe("Button visibility", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should show upload button when no file is selected", async () => {
      const uploadButton = await loader.getHarness(MatButtonHarness);
      expect(uploadButton).toBeTruthy();
    });

    it("should show delete button when file is selected", async () => {
      const mockFile = createMockFile("test.txt", 1024);
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      component.form.controls.document.setValue(mockFile);
      fixture.detectChanges();

      const deleteButton = await loader.getHarness(MatButtonHarness);
      expect(deleteButton).toBeTruthy();
    });
  });

  describe("Required field validation", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "requiredDocument";
      component.ngOnInit();
      component.form.controls.requiredDocument.addValidators(
        Validators.required,
      );
      fixture.detectChanges();
    });

    it("should indicate required field", () => {
      expect(component["required"]).toBe(true);
    });

    it("should show required attribute on input", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isRequired()).toBe(true);
    });
  });

  describe("File type restrictions", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should accept file with allowed extension", () => {
      component.allowedFileTypes = [".txt", ".pdf"];
      fixture.detectChanges();
      const validFile = createMockFile("document.txt", 1024);
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [validFile],
        }),
      });

      component["selectedFile"](mockEvent);

      expect(component.form.controls.document.value).toBe(validFile);
      expect(component.form.controls.document.errors).toBeNull();
    });
  });

  describe("File size restrictions", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should accept file within size limit", () => {
      component.maxFileSizeMB = 2;
      fixture.detectChanges();
      const validFile = createMockFile("test.txt", 1024 * 1024); // 1MB
      const mockEvent = fromPartial<Event>({
        target: fromPartial<HTMLInputElement>({
          files: [validFile],
        }),
      });

      component["selectedFile"](mockEvent);

      expect(component.form.controls.document.value).toBe(validFile);
      expect(component.form.controls.document.errors).toBeNull();
    });
  });

  describe("Display control updates", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
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
      component.form.controls.document.setValue(mockFile);
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
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should handle destroy when no subscription exists", () => {
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe("Readonly mode", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      fixture.detectChanges();
    });

    it("should be readonly when readonly input is true", async () => {
      component.readonly = true;
      fixture.detectChanges();
      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isReadonly()).toBe(true);
    });
  });

  describe("Hint display", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "document";
      component.ngOnInit();
      component.maxFileSizeMB = 5;
      component.allowedFileTypes = [".txt", ".pdf"];
      fixture.detectChanges();
      translateService.setTranslation("en", {
        "form.input.file.hint": "Max size: 5MB, Formats: .txt, .pdf",
      });
      translateService.use("en");
      fixture.detectChanges();
    });

    it("should display hint with file size and formats", () => {
      const hintElement = fixture.nativeElement.querySelector("mat-hint");
      expect(hintElement).toBeTruthy();
      expect(hintElement.textContent).toContain("Max size: 5MB");
      expect(hintElement.textContent).toContain("Formats: .txt, .pdf");
    });
  });
});
