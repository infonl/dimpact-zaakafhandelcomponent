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
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatCheckboxHarness } from "@angular/material/checkbox/testing";
import { MatErrorHarness } from "@angular/material/form-field/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { ZacCheckbox } from "./checkbox";

type SimpleForm = { active: FormControl<boolean | null> };

const makeForm = (
  control: FormControl<boolean | null> = new FormControl(false),
): FormGroup<SimpleForm> => new FormGroup<SimpleForm>({ active: control });

@Component({
  template: `<zac-checkbox [form]="form" key="active" [label]="label" />`,
  standalone: false,
})
class HostComponent {
  form = makeForm();
  label: string | undefined = undefined;
}

describe(ZacCheckbox.name, () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HostComponent],
      imports: [
        ZacCheckbox,
        ReactiveFormsModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        MatCheckboxModule,
        CapitalizeFirstLetterPipe,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it("reflects form control value — unchecked when false", async () => {
    host.form.controls.active.setValue(false);
    fixture.detectChanges();
    const checkbox = await loader.getHarness(MatCheckboxHarness);
    expect(await checkbox.isChecked()).toBe(false);
  });

  it("reflects form control value — checked when true", async () => {
    host.form.controls.active.setValue(true);
    fixture.detectChanges();
    const checkbox = await loader.getHarness(MatCheckboxHarness);
    expect(await checkbox.isChecked()).toBe(true);
  });

  it("updates the form control when the user toggles the checkbox", async () => {
    const checkbox = await loader.getHarness(MatCheckboxHarness);
    await checkbox.check();
    expect(host.form.controls.active.value).toBe(true);
  });

  it("is disabled when the form control is disabled", async () => {
    host.form.controls.active.disable();
    fixture.detectChanges();
    const checkbox = await loader.getHarness(MatCheckboxHarness);
    expect(await checkbox.isDisabled()).toBe(true);
  });

  it("shows mat-error when the control has a validation error", async () => {
    const control = new FormControl<boolean | null>(null, Validators.required);
    host.form = new FormGroup<SimpleForm>({ active: control });
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

  it("marks the checkbox as required when the control has the required validator", async () => {
    const control = new FormControl<boolean | null>(null, Validators.required);
    host.form = new FormGroup<SimpleForm>({ active: control });
    fixture.detectChanges();
    const checkbox = await loader.getHarness(MatCheckboxHarness);
    expect(await checkbox.isRequired()).toBe(true);
  });
});
