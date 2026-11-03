/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injector, runInInjectionContext, signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { FormControl, FormGroup, Validators } from "@angular/forms";
import { trackControl } from "./control-state";

describe(trackControl.name, () => {
  function setup(control = new FormControl<string | null>("eerste")) {
    const controlSignal = signal<FormControl<string | null> | null>(control);
    const read = runInInjectionContext(TestBed.inject(Injector), () =>
      trackControl(controlSignal),
    );
    const value = read((tracked) => tracked.value);
    const disabled = read((tracked) => tracked.disabled);
    TestBed.tick();
    return { control, controlSignal, value, disabled };
  }

  it("reads the current state of the control straight away", () => {
    const { value, disabled } = setup();

    expect(value()).toBe("eerste");
    expect(disabled()).toBe(false);
  });

  it("follows a value that is set from code", () => {
    const { control, value } = setup();
    expect(value()).toBe("eerste");

    control.setValue("tweede");

    expect(value()).toBe("tweede");
  });

  it("follows the control being disabled from code", () => {
    const { control, disabled } = setup();
    expect(disabled()).toBe(false);

    control.disable();

    expect(disabled()).toBe(true);
  });

  it("follows the validity, dirtiness and errors of a form group", () => {
    const form = new FormGroup({
      naam: new FormControl<string | null>(null, Validators.required),
    });
    const read = runInInjectionContext(TestBed.inject(Injector), () =>
      trackControl(signal(form)),
    );
    const valid = read((tracked) => tracked.valid);
    const dirty = read((tracked) => tracked.dirty);
    TestBed.tick();
    expect(valid()).toBe(false);
    expect(dirty()).toBe(false);

    form.controls.naam.setValue("Jan");
    form.markAsDirty();

    expect(valid()).toBe(true);
    expect(dirty()).toBe(true);
  });

  it("follows another control once the tracked control is replaced", () => {
    const { controlSignal, value } = setup();
    const replacement = new FormControl<string | null>("ander");

    controlSignal.set(replacement);
    TestBed.tick();
    expect(value()).toBe("ander");
    replacement.setValue("ander gewijzigd");

    expect(value()).toBe("ander gewijzigd");
  });

  it("follows a value or disabled state that code changed without events, once it is checked", () => {
    const control = new FormControl<string | null>("eerste");
    const read = runInInjectionContext(TestBed.inject(Injector), () =>
      trackControl(signal(control)),
    );
    const value = read((tracked) => tracked.value);
    const disabled = read((tracked) => tracked.disabled);
    TestBed.tick();
    read.check();
    expect(value()).toBe("eerste");

    control.setValue("stil", { emitEvent: false });
    control.disable({ emitEvent: false });
    read.check();

    expect(value()).toBe("stil");
    expect(disabled()).toBe(true);
  });

  it("does not recompute when a check finds nothing changed", () => {
    const control = new FormControl<string | null>("eerste");
    const read = runInInjectionContext(TestBed.inject(Injector), () =>
      trackControl(signal(control)),
    );
    const reads = jest.fn(
      (tracked: FormControl<string | null>) => tracked.value,
    );
    const value = read(reads);
    TestBed.tick();
    value();

    read.check();
    value();

    expect(reads).toHaveBeenCalledTimes(1);
  });

  it("reads nothing while there is no control", () => {
    const { controlSignal, value } = setup();

    controlSignal.set(null);

    expect(value()).toBeUndefined();
  });
});
