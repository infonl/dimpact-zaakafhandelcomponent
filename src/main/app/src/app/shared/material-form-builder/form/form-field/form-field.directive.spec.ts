/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CUSTOM_ELEMENTS_SCHEMA, Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { MaterialFormBuilderService } from "../../material-form-builder.service";
import { FormFieldDirective } from "./form-field.directive";

@Component({
  selector: "mfb-test-component",
  template: ` <mfb-form-field [field]="field"></mfb-form-field> `,
})
class TestComponent {}

describe("FormFieldDirective", () => {
  let fixture;
  let des;

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      providers: [MaterialFormBuilderService],
      declarations: [FormFieldDirective],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).createComponent(TestComponent);
    fixture.detectChanges(); // initial binding

    // all elements with an attached HighlightDirective
    des = fixture.debugElement.queryAll(By.directive(FormFieldDirective));
  });

  it("should create an instance", () => {
    expect(des).toBeTruthy();
  });
});
