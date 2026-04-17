/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatErrorHarness } from "@angular/material/form-field/testing";
import { MatSlideToggleHarness } from "@angular/material/slide-toggle/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { ZacToggle } from "./toggle";

type SimpleForm = { enabled: FormControl<boolean | null> };

const makeForm = (control: FormControl<boolean | null> = new FormControl(false)): FormGroup<SimpleForm> =>
  new FormGroup<SimpleForm>({ enabled: control });

@Component({
  template: `<zac-toggle [form]="form" key="enabled" [label]="label" />`,
  standalone: false,
})
class HostComponent {
  form = makeForm();
  label: string | undefined = undefined;
}

describe(ZacToggle.name, () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HostComponent],
      imports: [
        ZacToggle,
        ReactiveFormsModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        MatSlideToggleModule,
        CapitalizeFirstLetterPipe,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it("reflects form control value — off when false", async () => {
    host.form.controls.enabled.setValue(false);
    fixture.detectChanges();
    const toggle = await loader.getHarness(MatSlideToggleHarness);
    expect(await toggle.isChecked()).toBe(false);
  });

  it("reflects form control value — on when true", async () => {
    host.form.controls.enabled.setValue(true);
    fixture.detectChanges();
    const toggle = await loader.getHarness(MatSlideToggleHarness);
    expect(await toggle.isChecked()).toBe(true);
  });

  it("updates the form control when the user toggles", async () => {
    const toggle = await loader.getHarness(MatSlideToggleHarness);
    await toggle.check();
    expect(host.form.controls.enabled.value).toBe(true);
  });

  it("is disabled when the form control is disabled", async () => {
    host.form.controls.enabled.disable();
    fixture.detectChanges();
    const toggle = await loader.getHarness(MatSlideToggleHarness);
    expect(await toggle.isDisabled()).toBe(true);
  });

  it("shows mat-error when the control has a validation error", async () => {
    const control = new FormControl<boolean | null>(null, Validators.required);
    host.form = new FormGroup<SimpleForm>({ enabled: control });
    fixture.detectChanges();
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

  it("is required when the control has the required validator", async () => {
    const control = new FormControl<boolean | null>(null, Validators.required);
    host.form = new FormGroup<SimpleForm>({ enabled: control });
    fixture.detectChanges();
    const toggle = await loader.getHarness(MatSlideToggleHarness);
    expect(await toggle.isRequired()).toBe(true);
  });
});
