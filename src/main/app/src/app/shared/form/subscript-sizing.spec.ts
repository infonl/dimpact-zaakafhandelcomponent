/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Type } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { FormControl, FormGroup } from "@angular/forms";
import { MatFormField } from "@angular/material/form-field";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { ZacAutoComplete } from "./auto-complete/auto-complete";
import { ZacDate } from "./date/date";
import { ZacInput } from "./input/input";
import { ZacSelect } from "./select/select";
import { ZacTextarea } from "./textarea/textarea";

const formFields: [string, Type<unknown>, Record<string, unknown>][] = [
  [ZacAutoComplete.name, ZacAutoComplete, { options: [] }],
  [ZacDate.name, ZacDate, {}],
  [ZacInput.name, ZacInput, {}],
  [ZacSelect.name, ZacSelect, { options: [] }],
  [ZacTextarea.name, ZacTextarea, {}],
];

describe("subscript sizing of the form fields", () => {
  const renderFormField = (
    component: Type<unknown>,
    inputs: Record<string, unknown>,
  ) => {
    TestBed.configureTestingModule({
      imports: [component, NoopAnimationsModule],
      providers: [provideTranslateService()],
    });
    const fixture = TestBed.createComponent(component);
    fixture.componentRef.setInput(
      "form",
      new FormGroup({ field: new FormControl(null) }),
    );
    fixture.componentRef.setInput("key", "field");
    for (const [name, value] of Object.entries(inputs))
      fixture.componentRef.setInput(name, value);
    fixture.detectChanges();

    return () =>
      fixture.debugElement.query(By.directive(MatFormField)).componentInstance
        .subscriptSizing;
  };

  it.each(formFields)(
    "%s keeps a fixed hint height by default, so the fields of a form stay aligned",
    (_, component, inputs) => {
      const subscriptSizing = renderFormField(component, inputs);

      expect(subscriptSizing()).toBe("fixed");
    },
  );

  it.each(formFields)(
    "%s lets a long hint grow the field when asked, so the hint does not run under the next field",
    (_, component, inputs) => {
      const subscriptSizing = renderFormField(component, {
        ...inputs,
        subscriptSizing: "dynamic",
      });

      expect(subscriptSizing()).toBe("dynamic");
    },
  );
});
