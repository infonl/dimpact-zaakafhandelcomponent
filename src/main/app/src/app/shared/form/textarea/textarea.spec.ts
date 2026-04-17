/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatFormFieldModule } from "@angular/material/form-field";
import {
  MatErrorHarness,
  MatFormFieldHarness,
} from "@angular/material/form-field/testing";
import { MatInputModule } from "@angular/material/input";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { ZacTextarea } from "./textarea";

type SimpleForm = { notes: FormControl<string | null> };

const makeForm = (
  control: FormControl<string | null> = new FormControl(""),
): FormGroup<SimpleForm> => new FormGroup<SimpleForm>({ notes: control });

@Component({
  template: `<zac-textarea
    [form]="form"
    key="notes"
    [label]="label"
    [readonly]="readonly"
  />`,
  standalone: false,
})
class HostComponent {
  form = makeForm();
  label: string | undefined = undefined;
  readonly = false;
}

describe(ZacTextarea.name, () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HostComponent],
      imports: [
        ZacTextarea,
        ReactiveFormsModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        MatFormFieldModule,
        MatInputModule,
        CapitalizeFirstLetterPipe,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it("reflects form control value in the textarea", async () => {
    host.form.controls.notes.setValue("hello world");
    fixture.detectChanges();
    const input = await loader.getHarness(MatInputHarness);
    expect(await input.getValue()).toBe("hello world");
  });

  it("updates the form control when the user types", async () => {
    const input = await loader.getHarness(MatInputHarness);
    await input.setValue("new text");
    expect(host.form.controls.notes.value).toBe("new text");
  });

  it("is readonly when the readonly input is set", () => {
    host.readonly = true;
    fixture.detectChanges();
    const textarea: HTMLTextAreaElement =
      fixture.nativeElement.querySelector("textarea");
    expect(textarea.readOnly).toBe(true);
  });

  it("is disabled when the form control is disabled", async () => {
    host.form.controls.notes.disable();
    fixture.detectChanges();
    const input = await loader.getHarness(MatInputHarness);
    expect(await input.isDisabled()).toBe(true);
  });

  it("shows mat-error when the control has a validation error", async () => {
    const control = new FormControl<string | null>("", Validators.required);
    host.form = new FormGroup<SimpleForm>({ notes: control });
    fixture.detectChanges();
    control.markAsTouched();
    control.setErrors({ required: true });
    fixture.detectChanges();
    const errors = await loader.getAllHarnesses(MatErrorHarness);
    expect(errors.length).toBeGreaterThan(0);
  });

  it("hides mat-error when the control has no errors", async () => {
    fixture.detectChanges();
    const errors = await loader.getAllHarnesses(MatErrorHarness);
    expect(errors.length).toBe(0);
  });

  it("shows character counter hint when maxlength validator is present", async () => {
    const control = new FormControl<string | null>(
      "abc",
      Validators.maxLength(100),
    );
    host.form = new FormGroup<SimpleForm>({ notes: control });
    fixture.detectChanges();
    const formField = await loader.getHarness(MatFormFieldHarness);
    const hints = await formField.getTextHints();
    const hasCounter = hints.some((h) => h.includes("100"));
    expect(hasCounter).toBe(true);
  });

  it("hides character counter when no maxlength validator is present", async () => {
    fixture.detectChanges();
    const formField = await loader.getHarness(MatFormFieldHarness);
    const hints = await formField.getTextHints();
    const hasCounter = hints.some((h) => /\d+ \/ \d+/.test(h));
    expect(hasCounter).toBe(false);
  });

  it("shows clear button when the control has a non-empty value", async () => {
    host.form.controls.notes.setValue("some text");
    fixture.detectChanges();
    const buttons = await loader.getAllHarnesses(MatButtonHarness);
    expect(buttons.length).toBeGreaterThan(0);
  });

  it("hides clear button when the control is empty", async () => {
    host.form.controls.notes.setValue("");
    fixture.detectChanges();
    const buttons = await loader.getAllHarnesses(MatButtonHarness);
    expect(buttons.length).toBe(0);
  });
});
