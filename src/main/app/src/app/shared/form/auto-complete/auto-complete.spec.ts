/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  AbstractControl,
  FormControl,
  FormGroup,
  Validators,
} from "@angular/forms";
import { MatAutocompleteHarness } from "@angular/material/autocomplete/testing";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { MaterialFormBuilderModule } from "../../material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../material/material.module";
import { PipesModule } from "../../pipes/pipes.module";
import { ZacAutoComplete } from "./auto-complete";

interface TestOption {
  id: number;
  name: string;
}

interface TestForm extends Record<string, AbstractControl> {
  option: FormControl<TestOption | null>;
}

const makeOption = (fields: Partial<TestOption> = {}): TestOption =>
  ({ id: 1, name: "Option A", ...fields } as Partial<TestOption> as unknown as TestOption);

describe(ZacAutoComplete.name, () => {
  let component: ZacAutoComplete<
    TestForm,
    keyof TestForm,
    TestOption,
    keyof TestOption | ((option: TestOption) => string)
  >;
  let componentRef: ComponentRef<typeof component>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;

  const createTestForm = () =>
    new FormGroup<TestForm>({
      option: new FormControl<TestOption | null>(null, { nonNullable: true }),
    });

  const testOptions: TestOption[] = [
    makeOption({ id: 1, name: "Alpha" }),
    makeOption({ id: 2, name: "Beta" }),
    makeOption({ id: 3, name: "Gamma" }),
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZacAutoComplete,
        MaterialModule,
        TranslateModule.forRoot(),
        PipesModule,
        MaterialFormBuilderModule,
        NoopAnimationsModule,
      ],
      providers: [TranslateService],
    }).compileComponents();

    fixture = TestBed.createComponent(
      ZacAutoComplete<
        TestForm,
        keyof TestForm,
        TestOption,
        keyof TestOption | ((option: TestOption) => string)
      >,
    );
    componentRef = fixture.componentRef;
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("Basic rendering", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "option");
      componentRef.setInput("options", testOptions);
      fixture.detectChanges();
    });

    it("should render the form field", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      expect(formField).toBeTruthy();
    });

    it("should render the text input", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(input).toBeTruthy();
    });

    it("should show search icon button when no value is set", () => {
      const buttons = Array.from(
        fixture.nativeElement.querySelectorAll("button[matSuffix]"),
      ) as HTMLElement[];
      const searchButton = buttons.find(
        (btn) => btn.querySelector("mat-icon")?.textContent?.trim() === "search",
      );
      expect(searchButton).toBeTruthy();
    });
  });

  describe("Label display", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "option");
      componentRef.setInput("options", testOptions);
      fixture.detectChanges();
    });

    it("should display custom label when provided", async () => {
      componentRef.setInput("label", "Mijn optie");
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const label = await formField.getLabel();
      expect(label).toBe("Mijn optie");
    });
  });

  describe("Autocomplete options", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "option");
      componentRef.setInput("options", testOptions);
      componentRef.setInput("optionDisplayValue", "name");
      fixture.detectChanges();
    });

    it("should show all options when the panel opens", async () => {
      const autocomplete = await loader.getHarness(MatAutocompleteHarness);
      await autocomplete.focus();
      fixture.detectChanges();

      const options = await autocomplete.getOptions();
      expect(options.length).toBe(testOptions.length);
    });

    it("should filter options based on typed text", async () => {
      const autocomplete = await loader.getHarness(MatAutocompleteHarness);
      await autocomplete.focus();

      const input = await loader.getHarness(MatInputHarness);
      await input.setValue("Alp");
      fixture.detectChanges();

      const options = await autocomplete.getOptions();
      expect(options.length).toBe(1);
      expect(await options[0].getText()).toBe("Alpha");
    });

    it("should show all options when input is cleared after typing", async () => {
      const input = await loader.getHarness(MatInputHarness);
      const autocomplete = await loader.getHarness(MatAutocompleteHarness);

      await autocomplete.focus();
      await input.setValue("Alp");
      fixture.detectChanges();

      await input.setValue("");
      fixture.detectChanges();

      const options = await autocomplete.getOptions();
      expect(options.length).toBe(testOptions.length);
    });
  });

  describe("Clear button", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "option");
      componentRef.setInput("options", testOptions);
      componentRef.setInput("optionDisplayValue", "name");
      fixture.detectChanges();
    });

    it("should not show clear button when no value is set", () => {
      const buttons = Array.from(
        fixture.nativeElement.querySelectorAll("button[matSuffix]"),
      ) as HTMLElement[];
      const clearButton = buttons.find(
        (btn) => btn.querySelector("mat-icon")?.textContent?.trim() === "clear",
      );
      expect(clearButton).toBeUndefined();
    });
  });

  describe("Disabled state", () => {
    it("should disable the control when readonly is true", () => {
      const form = createTestForm();
      componentRef.setInput("form", form);
      componentRef.setInput("key", "option");
      componentRef.setInput("options", testOptions);
      componentRef.setInput("readonly", true);
      fixture.detectChanges();

      expect(form.controls.option.disabled).toBe(true);
    });
  });

  describe("Validation errors", () => {
    it("should show error when field is required and touched with no value", async () => {
      const form = new FormGroup<TestForm>({
        option: new FormControl<TestOption | null>(null, [Validators.required]),
      });
      form.controls.option.markAsTouched();
      form.controls.option.updateValueAndValidity();
      componentRef.setInput("form", form);
      componentRef.setInput("key", "option");
      componentRef.setInput("options", testOptions);
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const hasErrors = await formField.hasErrors();
      expect(hasErrors).toBe(true);
    });
  });

  describe("reset()", () => {
    it("should clear the form control value and reset filtered options", () => {
      const form = createTestForm();
      form.controls.option.setValue(testOptions[0]);
      componentRef.setInput("form", form);
      componentRef.setInput("key", "option");
      componentRef.setInput("options", testOptions);
      componentRef.setInput("optionDisplayValue", "name");
      fixture.detectChanges();

      component["reset"]();
      fixture.detectChanges();

      expect(form.controls.option.value).toBeNull();
      expect(component["filteredOptions"]()).toEqual(testOptions);
    });
  });
});
