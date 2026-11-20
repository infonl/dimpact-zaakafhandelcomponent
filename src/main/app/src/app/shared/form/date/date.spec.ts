/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentRef, LOCALE_ID } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ValidatorFn,
  Validators,
} from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService, TranslateService } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import {
  addDays,
  isPickedDate,
  now,
  startOfDay,
  today,
} from "../../../date-time";
import { ZacDate } from "./date";

interface TestForm extends Record<string, AbstractControl> {
  date: FormControl<string | null>;
}

describe(ZacDate.name, () => {
  let component: ZacDate<TestForm, keyof TestForm, string | null>;
  let componentRef: ComponentRef<typeof component>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;

  const createTestForm = (validators: ValidatorFn[] = []) => {
    return new FormGroup<TestForm>({
      date: new FormControl<string | null>(null, validators),
    });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZacDate, NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        { provide: LOCALE_ID, useValue: "nl-NL" },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(
      ZacDate<TestForm, keyof TestForm, string | null>,
    );
    componentRef = fixture.componentRef;
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("Basic rendering", () => {
    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "date");
      fixture.detectChanges();
    });

    it("should render the form field", async () => {
      const formField = await loader.getHarness(MatFormFieldHarness);
      expect(formField).toBeTruthy();
    });

    it("should render the date input", async () => {
      const input = await loader.getHarness(MatInputHarness);
      expect(input).toBeTruthy();
    });
  });

  describe("Label display", () => {
    beforeEach(() => {
      const form = createTestForm();
      componentRef.setInput("form", form);
      componentRef.setInput("key", "date");
      fixture.detectChanges();
    });

    it("should show custom label when provided", async () => {
      componentRef.setInput("label", "Mijn datum");
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const label = await formField.getLabel();
      expect(label).toBe("Mijn datum");
    });
  });

  describe("min / max from validators", () => {
    it("should compute min from Validators.min", () => {
      const minMs = startOfDay(addDays(now(), 1)).valueOf();
      const form = new FormGroup<TestForm>({
        date: new FormControl<string | null>(null, [Validators.min(minMs)]),
      });
      componentRef.setInput("form", form);
      componentRef.setInput("key", "date");
      fixture.detectChanges();

      const minValue = component["min"]();
      expect(minValue).not.toBeNull();
      expect(isPickedDate(minValue)).toBe(true);
    });

    it("should return null for min when no Validators.min is set", () => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "date");
      fixture.detectChanges();

      expect(component["min"]()).toBeNull();
    });

    it("should compute max from Validators.max", () => {
      const maxMs = addDays(today(), 31).valueOf() - 1;
      const form = new FormGroup<TestForm>({
        date: new FormControl<string | null>(null, [Validators.max(maxMs)]),
      });
      componentRef.setInput("form", form);
      componentRef.setInput("key", "date");
      fixture.detectChanges();

      const maxValue = component["max"]();
      expect(maxValue).not.toBeNull();
      expect(isPickedDate(maxValue)).toBe(true);
    });

    it("should return null for max when no Validators.max is set", () => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "date");
      fixture.detectChanges();

      expect(component["max"]()).toBeNull();
    });

    it("should update min when Validators.min changes after render", () => {
      const form = createTestForm();
      componentRef.setInput("form", form);
      componentRef.setInput("key", "date");
      fixture.detectChanges();

      expect(component["min"]()).toBeNull();

      const minMs = startOfDay(addDays(now(), 5)).valueOf();
      form.controls.date.addValidators(Validators.min(minMs));
      form.controls.date.updateValueAndValidity();
      fixture.detectChanges();

      expect(component["min"]()?.valueOf()).toBe(minMs);
    });
  });

  describe("Clear button", () => {
    it("should not show clear button when no value is set", async () => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "date");
      fixture.detectChanges();

      const clearButtons = await loader.getAllHarnesses(
        MatButtonHarness.with({ text: "clear" }),
      );
      expect(clearButtons.length).toBe(0);
    });
  });

  describe("showAmountOfDays", () => {
    it("should not show days suffix when showAmountOfDays is false (default)", async () => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "date");
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const suffixText = await formField.getSuffixText();
      expect(suffixText).toBe("");
    });
  });

  describe("Disabled state", () => {
    it("should disable the control when readonly is true", () => {
      const form = createTestForm();
      componentRef.setInput("form", form);
      componentRef.setInput("key", "date");
      componentRef.setInput("readonly", true);
      fixture.detectChanges();

      expect(form.controls.date.disabled).toBe(true);
    });
  });

  describe("Validation errors", () => {
    it("should show error when field is required and touched with no value", async () => {
      const form = new FormGroup<TestForm>({
        date: new FormControl<string | null>(null, [Validators.required]),
      });
      form.controls.date.markAsTouched();
      form.controls.date.updateValueAndValidity();
      componentRef.setInput("form", form);
      componentRef.setInput("key", "date");
      fixture.detectChanges();

      const formField = await loader.getHarness(MatFormFieldHarness);
      const hasErrors = await formField.hasErrors();
      expect(hasErrors).toBe(true);
    });
  });

  describe("a typed date outside the bounds", () => {
    function readDutchTranslations() {
      const file = join(__dirname, "../../../../assets/i18n/nl.json");
      return JSON.parse(readFileSync(file, "utf-8"));
    }

    async function typeDate(date: string) {
      const user = userEvent.setup();
      await user.type(screen.getByLabelText(/date/i), date, {
        initialSelectionStart: 0,
        initialSelectionEnd: Infinity,
      });
      await user.tab();
      fixture.detectChanges();
    }

    beforeEach(() => {
      const translateService = TestBed.inject(TranslateService);
      translateService.setTranslation("nl", readDutchTranslations());
      translateService.use("nl");
      componentRef.setInput(
        "form",
        createTestForm([
          Validators.min(new Date(2026, 6, 30).valueOf()),
          Validators.max(new Date(2026, 7, 9).valueOf()),
        ]),
      );
      componentRef.setInput("key", "date");
      fixture.detectChanges();
    });

    it("names the earliest date that can be picked when the typed date is earlier", async () => {
      await typeDate("2026-07-29");

      expect(
        screen.getByText("De minimum datum is 30-07-2026"),
      ).toBeInTheDocument();
    });

    it("names the latest date that can be picked when the typed date is later", async () => {
      await typeDate("2026-08-10");

      expect(
        screen.getByText("De maximum datum is 09-08-2026"),
      ).toBeInTheDocument();
    });
  });
});
