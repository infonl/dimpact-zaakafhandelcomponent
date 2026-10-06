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
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
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
  const user = userEvent.setup();
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

    it("should render zac-html-editor for type html-editor", async () => {
      createComponent(createTestForm(), [{ type: "html-editor", key: "name" }]);
      await fixture.whenStable();

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

    it("should hide the form buttons when readonly is true", () => {
      createComponent(createTestForm(), [{ type: "input", key: "name" }]);

      componentRef.setInput("readonly", true);
      fixture.detectChanges();

      expect(screen.queryByRole("button")).toBeNull();
    });
  });

  describe("submit button", () => {
    it("should be disabled when form is invalid", () => {
      const form = createTestForm();
      form.controls.name.addValidators(Validators.required);
      form.controls.name.updateValueAndValidity();
      createComponent(form, [{ type: "input", key: "name" }]);

      const submitButton = screen.getByRole("button", {
        name: "actie.verstuur",
      });
      expect(submitButton).toBeDisabled();
    });

    it("should be disabled when loading is true", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      componentRef.setInput("loading", true);
      fixture.detectChanges();

      const submitButton = screen.getByRole("button", {
        name: "actie.verstuur",
      });
      expect(submitButton).toBeDisabled();
    });

    it("should stay enabled on success by default", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      componentRef.setInput("success", true);
      fixture.detectChanges();

      const submitButton = screen.getByRole("button", {
        name: "actie.verstuur",
      });
      expect(submitButton).toBeEnabled();
    });

    it("should be disabled on success when disableAfterSuccess is set", () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      componentRef.setInput("disableAfterSuccess", true);
      componentRef.setInput("success", true);
      fixture.detectChanges();

      const submitButton = screen.getByRole("button", {
        name: "actie.verstuur",
      });
      expect(submitButton).toBeDisabled();
    });
  });

  describe("outputs", () => {
    it("should emit formSubmitted with form group when submitted", async () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }]);

      let emitted: FormGroup<TestForm> | undefined;
      componentRef.instance["formSubmitted"].subscribe(
        (value: FormGroup<TestForm>) => (emitted = value),
      );

      await user.click(screen.getByRole("button", { name: "actie.verstuur" }));

      expect(emitted).toBe(form);
    });

    it("should emit formCancelled and reset form when cancel is clicked", async () => {
      const form = createTestForm();
      form.controls.name.setValue("test");
      createComponent(form, [{ type: "input", key: "name" }]);

      let emitted = false;
      componentRef.instance["formCancelled"].subscribe(() => (emitted = true));

      await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

      expect(emitted).toBe(true);
      expect(form.controls.name.value).toBeNull();
    });

    it("should emit formPartiallySubmitted when partial submit is clicked", async () => {
      const form = createTestForm();
      createComponent(form, [{ type: "input", key: "name" }], {
        partialSubmitLabel: "save",
        hideCancelButton: true,
      });

      let emitted: FormGroup<TestForm> | undefined;
      componentRef.instance["formPartiallySubmitted"].subscribe(
        (value: FormGroup<TestForm>) => (emitted = value),
      );

      await user.click(screen.getByRole("button", { name: "save" }));

      expect(emitted).toBe(form);
    });
  });

  describe("config", () => {
    it("should show cancel button by default", () => {
      createComponent(createTestForm(), [{ type: "input", key: "name" }]);

      expect(
        screen.getByRole("button", { name: "actie.annuleren" }),
      ).toBeInTheDocument();
    });

    it("should hide cancel button when hideCancelButton is true", () => {
      createComponent(createTestForm(), [{ type: "input", key: "name" }], {
        partialSubmitLabel: "save",
        hideCancelButton: true,
      });

      expect(
        screen.queryByRole("button", { name: "actie.annuleren" }),
      ).not.toBeInTheDocument();
    });

    it("should show partial submit button when hideCancelButton is true", () => {
      createComponent(createTestForm(), [{ type: "input", key: "name" }], {
        partialSubmitLabel: "save",
        hideCancelButton: true,
      });

      expect(screen.getByRole("button", { name: "save" })).toBeInTheDocument();
    });
  });
});
