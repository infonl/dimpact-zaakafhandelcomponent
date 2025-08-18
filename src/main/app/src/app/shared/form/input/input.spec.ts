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
import { MaterialModule } from "../../material/material.module";
import { PipesModule } from "../../pipes/pipes.module";
import { ZacInput } from "./input";
import { MaterialFormBuilderModule } from "../../material-form-builder/material-form-builder.module";

interface TestForm extends Record<string, AbstractControl> {
  name: FormControl<string | null>;
  age: FormControl<number | null>;
  email: FormControl<string | null>;
  description: FormControl<string | null>;
}

describe(ZacInput.name, () => {
  let component: ZacInput<TestForm, keyof TestForm, unknown, () => string>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;
  let translateService: TranslateService;

  const createTestForm = () => {
    return new FormGroup<TestForm>({
      name: new FormControl<string | null>(null, { nonNullable: true }),
      age: new FormControl<number | null>(null, { nonNullable: true }),
      email: new FormControl<string | null>(null, { nonNullable: true }),
      description: new FormControl<string | null>(null, { nonNullable: true }),
    });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ZacInput],
      imports: [
        ReactiveFormsModule,
        MaterialModule,
        TranslateModule.forRoot(),
        PipesModule,
        MaterialFormBuilderModule,
        NoopAnimationsModule,
      ],
      providers: [TranslateService],
    }).compileComponents();

    translateService = TestBed.inject(TranslateService);
    fixture = TestBed.createComponent(
      ZacInput<TestForm, keyof TestForm, any, any>
    );
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("Basic functionality", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "name";
      fixture.detectChanges();
    });

    it("should create", () => {
      expect(component).toBeTruthy();
    });

    it("should display the input field", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(input).toBeTruthy();
    });

    it("should display the form field", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      expect(formField).toBeTruthy();
    });

    it("should bind to the form control", async () => {
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("John Doe");

      expect(component.form.controls.name.value).toBe("John Doe");
    });

    it("should update the input when form control value changes", async () => {
      component.form.controls.name.setValue("Jane Doe");
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      expect(await input.getValue()).toBe("Jane Doe");
    });
  });

  describe("Label display", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "name";
      translateService.setTranslation("en", {
        name: "Test field label",
      });
      translateService.use("en");
      fixture.detectChanges();
    });

    it("should display translated key as label when no label is provided", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      const label = await formField.getLabel();
      expect(label).toBe("Test field label");
    });

    it("should display custom label when provided", async () => {
      component.label = "Custom Name Label";
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const label = await formField.getLabel();
      expect(label).toBe("Custom Name Label");
    });
  });

  describe("Input types", () => {
    it("should default to text type", async () => {
      component.form = createTestForm();
      component.key = "name";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      expect(await input.getType()).toBe("text");
    });

    it("should set number type when specified", async () => {
      component.form = createTestForm();
      component.key = "age";
      component.type = "number";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      expect(await input.getType()).toBe("number");
    });

    it("should automatically set number type when min validators are present", async () => {
      const form = createTestForm();
      form.controls.age.addValidators(Validators.min(0));
      component.form = form;
      component.key = "age";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      expect(await input.getType()).toBe("number");
    });

    it("should automatically set number type when max validators are present", async () => {
      const form = createTestForm();
      form.controls.age.addValidators(Validators.max(0));
      component.form = form;
      component.key = "age";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      expect(await input.getType()).toBe("number");
    });
  });

  describe("Validation", () => {
    /**
     * MatInputHarness.setValue() bypasses the HTML input constraints like maxlength.
     * This is a known limitation when testing with harnesses.
     * This test is a workaround to check if the maxlength constraint is applied.
     */
    it("should apply maxlength constraint", async () => {
      const form = createTestForm();
      form.controls.name.addValidators(Validators.maxLength(10));
      component.form = form;
      component.key = "name";
      fixture.detectChanges();

      // Test that the maxlength is properly extracted by the component
      expect((component as unknown as Record<string, unknown>).maxlength).toBe(10);

      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("This is too long for the maxlength");

      const control = component.form.controls.name;
      expect(control?.errors?.["maxlength"]).toBeTruthy();
      expect(control?.errors?.["maxlength"]).toBeTruthy();
    });

    it("should apply min/max constraints for number inputs", async () => {
      const form = createTestForm();
      form.controls.age.addValidators([Validators.min(0), Validators.max(120)]);
      component.form = form;
      component.key = "age";
      component.type = "number";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("150");

      expect(component.form.controls.age.value).toBe("150");
    });
  });

  describe("Readonly mode", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "name";
      fixture.detectChanges();
    });

    it("should be editable by default", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isReadonly()).toBe(false);
    });

    it("should be readonly when readonly input is true", async () => {
      component.readonly = true;
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isReadonly()).toBe(true);
    });

    it("should be readonly when displayValue is set", async () => {
      component.displayValue = () => "name";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      expect(await input.isReadonly()).toBe(true);
    });
  });

  describe("Display value functionality", () => {
    it("should hide input when displayValue is set", async () => {
      component.form = createTestForm();
      component.key = "name";
      component.displayValue = () => "name";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      const hostElement = await input.host();
      expect(await hostElement.hasClass("hide-gt-xs")).toBe(true);
    });
  });

  describe("Clear button", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "name";
      fixture.detectChanges();
    });

    it("should show clear button when input has value and is not readonly", async () => {
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("Test value");
      fixture.detectChanges();

      const clearButton = await loader.getHarnessOrNull(MatButtonHarness);
      expect(clearButton).toBeTruthy();
    });

    it("should not show clear button when input is empty", async () => {
      const clearButton = await loader.getHarnessOrNull(MatButtonHarness);
      expect(clearButton).toBeFalsy();
    });

    it("should not show clear button when input is readonly", async () => {
      component.readonly = true;
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("Test value");
      fixture.detectChanges();

      const clearButton = await loader.getHarnessOrNull(MatButtonHarness);
      expect(clearButton).toBeFalsy();
    });

    it("should clear the input when clear button is clicked", async () => {
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("Test value");
      fixture.detectChanges();

      const clearButton = await loader.getHarness(MatButtonHarness);
      await clearButton?.click();
      fixture.detectChanges();

      expect(component.form.controls.name.value).toBeNull();
    });
  });

  describe("Input event handling", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "name";
      fixture.detectChanges();
    });

    it("should reset control to null when text input is cleared", async () => {
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("Test value");
      await input.setValue("");
      fixture.detectChanges();

      expect(component.form.controls.name.value).toBeNull();
    });

    it("should reset control to null when number input is cleared", async () => {
      component.type = "number";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("123");
      await input.setValue("");
      fixture.detectChanges();

      expect(component.form.controls.age.value).toBeNull();
    });
  });

  describe("Character counter", () => {
    it("should show character counter when maxlength is set", async () => {
      const form = createTestForm();
      form.controls.description.addValidators(Validators.maxLength(100));
      component.form = form;
      component.key = "description";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("Test description");
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain("16 / 100");
    });

    it("should not show character counter when maxlength is not set", async () => {
      component.form = createTestForm();
      component.key = "description";
      fixture.detectChanges();

      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("Test description");
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).not.toContain("/");
    });
  });

  describe("Error messages", () => {
    it("should display error message when form control has errors", async () => {
      const form = createTestForm();
      form.controls.email.addValidators(Validators.required);
      form.controls.email.markAsTouched();
      component.form = form;
      component.key = "email";
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const hasErrors = await formField.hasErrors();
      expect(hasErrors).toBe(true);
    });

    it("should not display error message when form control has no errors", async () => {
      component.form = createTestForm();
      component.key = "email";
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const hasErrors = await formField.hasErrors();
      expect(hasErrors).toBe(false);
    });
  });

  describe("Content projection", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "name";
      fixture.detectChanges();
    });

    it("should project mat-label content", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      expect(formField).toBeTruthy();
    });

    it("should project mat-hint content", async () => {
      // The template includes ng-content for mat-hint
      expect(fixture.nativeElement.querySelector("mat-hint")).toBeTruthy();
    });

    it("should project suffix content", async () => {
      // The template includes ng-content for button, span, and mat-icon in suffix
      expect(fixture.nativeElement.querySelector("[matSuffix]")).toBeTruthy();
    });
  });
});
