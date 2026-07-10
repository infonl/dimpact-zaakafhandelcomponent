/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  AbstractControl,
  FormControl,
  FormGroup,
  Validators,
} from "@angular/forms";
import { provideNativeDateAdapter } from "@angular/material/core";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { ZacHtmlEditor } from "../html-editor/html-editor";
import { ZacInput } from "../input/input";
import { ZacSelect } from "../select/select";
import { ZacTextarea } from "../textarea/textarea";
import { ZacComposedForm } from "./composed-form.component";
import { FormConfig, FormField } from "./form-field.types";

interface TestForm extends Record<string, AbstractControl> {
  name: FormControl<string | null>;
  description: FormControl<string | null>;
}

describe(ZacComposedForm.name, () => {
  let fixture: ComponentFixture<ZacComposedForm<TestForm>>;
  let componentRef: ComponentRef<ZacComposedForm<TestForm>>;

  const createTestForm = () =>
    new FormGroup<TestForm>({
      name: new FormControl<string | null>(null),
      description: new FormControl<string | null>(null),
    });

  const createComponent = (
    form: FormGroup<TestForm>,
    fields: FormField[],
    config?: FormConfig,
  ) => {
    fixture = TestBed.createComponent(ZacComposedForm<TestForm>);
    componentRef = fixture.componentRef;
    componentRef.setInput("form", form);
    componentRef.setInput("fields", fields);
    if (config) componentRef.setInput("config", config);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZacComposedForm,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideNativeDateAdapter(),
      ],
    }).compileComponents();
  });

  describe("field rendering", () => {
    it("should not render hidden fields", () => {
      const form = createTestForm();
      const fields: FormField[] = [
        { type: "input", key: "name", hidden: true },
      ];
      createComponent(form, fields);

      const input = fixture.debugElement.query(By.directive(ZacInput));
      expect(input).toBeNull();
    });

    it("should render zac-input for type input", () => {
      createComponent(createTestForm(), [{ type: "input", key: "name" }]);

      expect(fixture.debugElement.query(By.directive(ZacInput))).toBeTruthy();
    });

    it("should render zac-select for type select", () => {
      createComponent(createTestForm(), [
        { type: "select", key: "name", options: [] },
      ]);

      expect(fixture.debugElement.query(By.directive(ZacSelect))).toBeTruthy();
    });

    it("should render zac-textarea for type textarea", () => {
      createComponent(createTestForm(), [{ type: "textarea", key: "name" }]);

      expect(
        fixture.debugElement.query(By.directive(ZacTextarea)),
      ).toBeTruthy();
    });

    it("should render zac-html-editor for type html-editor", () => {
      createComponent(createTestForm(), [{ type: "html-editor", key: "name" }]);

      expect(
        fixture.debugElement.query(By.directive(ZacHtmlEditor)),
      ).toBeTruthy();
    });
  });

  describe("readonly effect", () => {
    it("should disable form group when readonly is true", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      componentRef.setInput("readonly", true);
      fixture.detectChanges();

      expect(form.disabled).toBe(true);
    });

    it("should re-enable form group when readonly switches to false", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      componentRef.setInput("readonly", true);
      fixture.detectChanges();
      componentRef.setInput("readonly", false);
      fixture.detectChanges();

      expect(form.enabled).toBe(true);
    });

    it("should keep field.readonly controls disabled when readonly switches to false", () => {
      const form = createTestForm();
      createComponent(form, [
        { type: "input", key: "name", readonly: true },
        { type: "input", key: "description" },
      ]);

      componentRef.setInput("readonly", true);
      fixture.detectChanges();
      componentRef.setInput("readonly", false);
      fixture.detectChanges();

      expect(form.controls.name.disabled).toBe(true);
      expect(form.controls.description.enabled).toBe(true);
    });

    it("should hide buttons fieldset when readonly is true", () => {
      createComponent(createTestForm(), [{ type: "input", key: "name" }]);

      componentRef.setInput("readonly", true);
      fixture.detectChanges();

      const fieldsets = fixture.nativeElement.querySelectorAll("fieldset");
      expect(fieldsets.length).toBe(1);
    });
  });

  describe("submit button", () => {
    it("should be disabled when form is invalid", () => {
      const form = createTestForm();
      form.controls.name.addValidators(Validators.required);
      form.controls.name.updateValueAndValidity();
      createComponent(form, [{ type: "input", key: "name" }]);

      const submitButton = fixture.nativeElement.querySelector(
        "button[type=submit]",
      );
      expect(submitButton.disabled).toBe(true);
    });

    it("should be disabled when loading is true", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      componentRef.setInput("loading", true);
      fixture.detectChanges();

      const submitButton = fixture.nativeElement.querySelector(
        "button[type=submit]",
      );
      expect(submitButton.disabled).toBe(true);
    });

    it("should stay enabled on success by default", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      componentRef.setInput("success", true);
      fixture.detectChanges();

      const submitButton = fixture.nativeElement.querySelector(
        "button[type=submit]",
      );
      expect(submitButton.disabled).toBe(false);
    });

    it("should be disabled on success when disableAfterSuccess is set", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      componentRef.setInput("disableAfterSuccess", true);
      componentRef.setInput("success", true);
      fixture.detectChanges();

      const submitButton = fixture.nativeElement.querySelector(
        "button[type=submit]",
      );
      expect(submitButton.disabled).toBe(true);
    });
  });

  describe("outputs", () => {
    it("should emit formSubmitted with form group when submitted", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      let emitted: FormGroup<TestForm> | undefined;
      componentRef.instance["formSubmitted"].subscribe(
        (value: FormGroup<TestForm>) => (emitted = value),
      );

      fixture.nativeElement
        .querySelector("form")
        .dispatchEvent(new Event("submit"));
      fixture.detectChanges();

      expect(emitted).toBe(form);
    });

    it("should emit formCancelled and reset form when cancel is clicked", () => {
      const form = createTestForm();
      form.controls.name.setValue("test");
      createComponent(form, [{ type: "input", key: "name" }]);

      let emitted = false;
      componentRef.instance["formCancelled"].subscribe(() => (emitted = true));

      const cancelButton =
        fixture.nativeElement.querySelector("button[type=reset]");
      cancelButton.click();
      fixture.detectChanges();

      expect(emitted).toBe(true);
      expect(form.controls.name.value).toBeNull();
    });

    it("should emit formPartiallySubmitted when partial submit is clicked", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }], {
        partialSubmitLabel: "save",
        hideCancelButton: true,
      });

      let emitted: FormGroup<TestForm> | undefined;
      componentRef.instance["formPartiallySubmitted"].subscribe(
        (value: FormGroup<TestForm>) => (emitted = value),
      );

      fixture.nativeElement.querySelector("button[type=button]").click();
      fixture.detectChanges();

      expect(emitted).toBe(form);
    });
  });

  describe("config", () => {
    it("should show cancel button by default", () => {
      createComponent(createTestForm(), [{ type: "input", key: "name" }]);

      const cancelButton =
        fixture.nativeElement.querySelector("button[type=reset]");
      expect(cancelButton).toBeTruthy();
    });

    it("should hide cancel button when hideCancelButton is true", () => {
      createComponent(createTestForm(), [{ type: "input", key: "name" }], {
        partialSubmitLabel: "save",
        hideCancelButton: true,
      });

      const cancelButton =
        fixture.nativeElement.querySelector("button[type=reset]");
      expect(cancelButton).toBeNull();
    });

    it("should show partial submit button when hideCancelButton is true", () => {
      createComponent(createTestForm(), [{ type: "input", key: "name" }], {
        partialSubmitLabel: "save",
        hideCancelButton: true,
      });

      const partialButton = fixture.nativeElement.querySelector(
        "button[type=button]",
      );
      expect(partialButton).toBeTruthy();
    });
  });
});
