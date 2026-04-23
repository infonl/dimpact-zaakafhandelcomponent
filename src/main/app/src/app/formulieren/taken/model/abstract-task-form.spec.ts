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
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
} from "@angular/forms";
import { MatAutocompleteHarness } from "@angular/material/autocomplete/testing";
import { MatCheckboxHarness } from "@angular/material/checkbox/testing";
import { MatDatepickerInputHarness } from "@angular/material/datepicker/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatRadioGroupHarness } from "@angular/material/radio/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { FormField, ZacForm } from "../../../shared/form/form";
import { MaterialFormBuilderModule } from "../../../shared/material-form-builder/material-form-builder.module";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { AbstractTaskForm } from "./abstract-task-form";

class TestForm extends AbstractTaskForm {
  async requestForm(zaak: GeneratedType<"RestZaak">): Promise<FormField[]> {
    void zaak;
    return [
      {
        type: "input",
        key: "inputField",
        label: "input-label",
        control: this.formBuilder.control(""),
      },
      {
        type: "textarea",
        key: "textareaField",
        label: "textarea-label",
        control: this.formBuilder.control(""),
      },
      {
        type: "date",
        key: "dateField",
        label: "date-label",
        control: this.formBuilder.control(null),
      },
      {
        type: "select",
        key: "selectField",
        label: "select-label",
        options: [{ key: "a", value: "Option A" }],
        control: this.formBuilder.control(null),
      },
      {
        type: "radio",
        key: "radioField",
        label: "radio-label",
        options: ["option-1", "option-2"],
        control: this.formBuilder.control(null),
      },
      {
        type: "checkbox",
        key: "checkboxField",
        label: "checkbox-label",
        control: this.formBuilder.control(false),
      },
      {
        type: "auto-complete",
        key: "autoCompleteField",
        label: "auto-complete-label",
        options: [{ id: "1", naam: "User A" }],
        control: this.formBuilder.control(null),
      },
      {
        type: "documents",
        key: "documentsField",
        label: "documents-label",
        options: of([]),
      },
      {
        type: "html-editor",
        key: "htmlEditorField",
        label: "html-editor-label",
        variables: [],
        control: this.formBuilder.control("<p>some html</p>"),
      },
      {
        type: "plain-text",
        key: "plainTextField",
        label: "plain-text-label",
        control: this.formBuilder.control("some text"),
      },
      {
        type: "input",
        key: "hiddenField",
        label: "hidden-label",
        hidden: true,
        control: this.formBuilder.control("hidden-value"),
      },
    ];
  }

  async handleForm(taak: GeneratedType<"RestTask">): Promise<FormField[]> {
    void taak;
    return [];
  }
}

describe(AbstractTaskForm.name, () => {
  let formulier: TestForm;
  let fixture: ComponentFixture<
    ZacForm<Record<string, AbstractControl<unknown, unknown>>>
  >;
  let componentRef: ComponentRef<
    ZacForm<Record<string, AbstractControl<unknown, unknown>>>
  >;
  let loader: HarnessLoader;
  let formGroup: FormGroup;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        RouterModule.forRoot([]),
        MaterialFormBuilderModule,
      ],
      providers: [FormBuilder],
    }).compileComponents();

    formulier = TestBed.runInInjectionContext(() => new TestForm());
  });

  describe("requestForm rendering", () => {
    beforeEach(async () => {
      const fields = await formulier.requestForm({
        uuid: "zaak-uuid",
      } as Partial<
        GeneratedType<"RestZaak">
      > as unknown as GeneratedType<"RestZaak">);

      formGroup = new FormGroup({});
      for (const field of fields) {
        formGroup.addControl(
          field.key,
          field.control ?? new FormBuilder().control(null),
        );
      }

      fixture = TestBed.createComponent(ZacForm);
      componentRef = fixture.componentRef;
      componentRef.setInput("fields", fields);
      componentRef.setInput("form", formGroup);
      fixture.detectChanges();

      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    it("should render an input field", async () => {
      const inputs = await loader.getAllHarnesses(MatInputHarness);
      // textarea also resolves as MatInputHarness — filter to type=text only
      const textInputs = await Promise.all(
        inputs.map(async (h) => ({ h, type: await h.getType() })),
      );
      expect(textInputs.some(({ type }) => type === "text")).toBe(true);
    });

    it("should render a textarea field", async () => {
      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const types = await Promise.all(inputs.map((h) => h.getType()));
      expect(types).toContain("textarea");
    });

    it("should render a date field", async () => {
      const datepicker = await loader.getHarnessOrNull(
        MatDatepickerInputHarness,
      );
      expect(datepicker).not.toBeNull();
    });

    it("should render a select field", async () => {
      const select = await loader.getHarnessOrNull(MatSelectHarness);
      expect(select).not.toBeNull();
    });

    it("should render a radio group", async () => {
      const radioGroup = await loader.getHarnessOrNull(MatRadioGroupHarness);
      expect(radioGroup).not.toBeNull();
    });

    it("should render a checkbox", async () => {
      const checkbox = await loader.getHarnessOrNull(MatCheckboxHarness);
      expect(checkbox).not.toBeNull();
    });

    it("should render an auto-complete field", async () => {
      const autocomplete = await loader.getHarnessOrNull(
        MatAutocompleteHarness,
      );
      expect(autocomplete).not.toBeNull();
    });

    // No Material harness available — zac-documents is a custom component
    it("should render a documents field", () => {
      expect(
        fixture.nativeElement.querySelector("zac-documents"),
      ).not.toBeNull();
    });

    // No Material harness available — zac-html-editor wraps ngx-editor
    it("should render an html-editor field", () => {
      expect(
        fixture.nativeElement.querySelector("zac-html-editor"),
      ).not.toBeNull();
    });

    // No Material harness available — plain-text renders as a <section>
    it("should render a plain-text field", () => {
      expect(
        fixture.nativeElement.querySelector("fieldset section"),
      ).not.toBeNull();
    });

    it("should not render hidden fields", () => {
      // @if (!field.hidden) suppresses the hidden field — only one zac-input in the DOM
      const zacInputs = fixture.nativeElement.querySelectorAll("zac-input");
      expect(zacInputs.length).toBe(1);
    });
  });
});
