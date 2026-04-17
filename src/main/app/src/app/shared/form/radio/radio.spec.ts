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
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatErrorHarness } from "@angular/material/form-field/testing";
import { MatRadioModule } from "@angular/material/radio";
import {
  MatRadioButtonHarness,
  MatRadioGroupHarness,
} from "@angular/material/radio/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { PipesModule } from "../../pipes/pipes.module";
import { ZacRadio } from "./radio";

interface TestOption {
  id: number;
  label: string;
}

interface TestForm extends Record<string, AbstractControl> {
  choice: FormControl<TestOption | null>;
}

type TestZacRadio = ZacRadio<
  TestForm,
  keyof TestForm,
  TestOption,
  keyof TestOption | ((option: TestOption) => string)
>;

const optionA: TestOption = { id: 1, label: "Option A" };
const optionB: TestOption = { id: 2, label: "Option B" };

const makeForm = (
  initialValue: TestOption | null = null,
  required = false,
) => {
  const validators = required ? [Validators.required] : [];
  return new FormGroup<TestForm>({
    choice: new FormControl<TestOption | null>(initialValue, validators),
  });
};

describe(ZacRadio.name, () => {
  let component: TestZacRadio;
  let componentRef: ComponentRef<TestZacRadio>;
  let fixture: ComponentFixture<TestZacRadio>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZacRadio,
        ReactiveFormsModule,
        MatRadioModule,
        TranslateModule.forRoot(),
        PipesModule,
        NoopAnimationsModule,
      ],
      providers: [TranslateService],
    }).compileComponents();

    fixture = TestBed.createComponent(
      ZacRadio<
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

  describe("radio group rendering", () => {
    beforeEach(() => {
      componentRef.setInput("form", makeForm());
      componentRef.setInput("key", "choice");
      componentRef.setInput("options", [optionA, optionB]);
      fixture.detectChanges();
    });

    it("renders a mat-radio-group bound to the form control", async () => {
      const radioGroup = await loader.getHarness(MatRadioGroupHarness);
      expect(radioGroup).not.toBeNull();
    });

    it("renders one mat-radio-button per option", async () => {
      const buttons = await loader.getAllHarnesses(MatRadioButtonHarness);
      expect(buttons.length).toBe(2);
    });

    it("displays option labels via displayWith", async () => {
      componentRef.setInput("optionDisplayValue", "label");
      fixture.detectChanges();

      const buttons = await loader.getAllHarnesses(MatRadioButtonHarness);
      expect(await buttons[0].getLabelText()).toBe("Option A");
      expect(await buttons[1].getLabelText()).toBe("Option B");
    });

    it("renders a label for the group", () => {
      const label = fixture.nativeElement.querySelector("mat-label");
      expect(label).not.toBeNull();
    });
  });

  describe("required indicator", () => {
    it("shows asterisk when control has required validator", () => {
      componentRef.setInput("form", makeForm(null, true));
      componentRef.setInput("key", "choice");
      componentRef.setInput("options", [optionA]);
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain("*");
    });

    it("does not show asterisk when control is not required", () => {
      componentRef.setInput("form", makeForm(null, false));
      componentRef.setInput("key", "choice");
      componentRef.setInput("options", [optionA]);
      fixture.detectChanges();

      const label = fixture.nativeElement.querySelector("mat-label");
      expect(label.textContent).not.toContain("*");
    });
  });

  describe("form control binding", () => {
    it("marks option as checked when form control has that value", async () => {
      componentRef.setInput("form", makeForm(optionA));
      componentRef.setInput("key", "choice");
      componentRef.setInput("options", [optionA, optionB]);
      fixture.detectChanges();

      const checkedButtons = await loader.getAllHarnesses(
        MatRadioButtonHarness.with({ checked: true }),
      );
      expect(checkedButtons.length).toBeGreaterThan(0);
    });

    it("reflects no checked state when form control value is null", async () => {
      componentRef.setInput("form", makeForm(null));
      componentRef.setInput("key", "choice");
      componentRef.setInput("options", [optionA, optionB]);
      fixture.detectChanges();

      const checkedButtons = await loader.getAllHarnesses(
        MatRadioButtonHarness.with({ checked: true }),
      );
      expect(checkedButtons.length).toBe(0);
    });
  });

  describe("disabled state", () => {
    it("disables the form control when readonly input is true", () => {
      const form = makeForm();
      componentRef.setInput("form", form);
      componentRef.setInput("key", "choice");
      componentRef.setInput("options", [optionA]);
      componentRef.setInput("readonly", true);
      fixture.detectChanges();

      expect(form.controls.choice.disabled).toBe(true);
    });
  });

  describe("displayWith", () => {
    beforeEach(() => {
      componentRef.setInput("form", makeForm());
      componentRef.setInput("key", "choice");
      componentRef.setInput("options", [optionA, optionB]);
      fixture.detectChanges();
    });

    it("returns string representation when no optionDisplayValue is given", () => {
      const result = component["displayWith"](optionA);
      expect(typeof result).toBe("string");
    });

    it("returns key property when optionDisplayValue is a key", () => {
      componentRef.setInput("optionDisplayValue", "label");
      fixture.detectChanges();

      expect(component["displayWith"](optionA)).toBe("Option A");
    });

    it("returns empty string for null/undefined option", () => {
      expect(component["displayWith"](null)).toBe("");
      expect(component["displayWith"](undefined)).toBe("");
    });

    it("returns function result when optionDisplayValue is a function", () => {
      const displayFn = (option: TestOption) => `#${option.id}`;
      componentRef.setInput("optionDisplayValue", displayFn);
      fixture.detectChanges();

      expect(component["displayWith"](optionA)).toBe("#1");
    });
  });

  describe("compareWith", () => {
    beforeEach(() => {
      componentRef.setInput("form", makeForm());
      componentRef.setInput("key", "choice");
      componentRef.setInput("options", [optionA, optionB]);
      fixture.detectChanges();
    });

    it("uses reference equality by default", () => {
      expect(component["compareWith"](optionA, optionA)).toBe(true);
      expect(component["compareWith"](optionA, optionB)).toBe(false);
    });

    it("uses displayWith equality when optionDisplayValue is set", () => {
      componentRef.setInput("optionDisplayValue", "label");
      fixture.detectChanges();

      const sameLabel: TestOption = { id: 99, label: "Option A" };
      expect(component["compareWith"](optionA, sameLabel)).toBe(true);
    });

    it("uses custom compare function when provided", () => {
      const compareFn = (a: TestOption, b: TestOption) =>
        a != null && b != null && a.id === b.id;
      componentRef.setInput("compare", compareFn);
      fixture.detectChanges();

      const sameId: TestOption = { id: 1, label: "Different" };
      expect(component["compareWith"](optionA, sameId)).toBe(true);
      expect(component["compareWith"](optionA, optionB)).toBe(false);
    });
  });

  describe("error display", () => {
    it("shows mat-error when control is invalid and touched", async () => {
      const form = makeForm(null, true);
      componentRef.setInput("form", form);
      componentRef.setInput("key", "choice");
      componentRef.setInput("options", [optionA]);
      fixture.detectChanges();

      form.controls.choice.markAsTouched();
      form.controls.choice.updateValueAndValidity();
      await fixture.whenStable();
      fixture.detectChanges();

      const errors = await loader.getAllHarnesses(MatErrorHarness);
      expect(errors.length).toBeGreaterThan(0);
    });
  });
});
