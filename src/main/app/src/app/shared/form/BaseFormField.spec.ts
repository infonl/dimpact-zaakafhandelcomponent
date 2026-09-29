/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Type } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { MutationObserver } from "@tanstack/angular-query-experimental";
import { render, waitFor } from "@testing-library/angular";
import { testQueryClient } from "../../../../setupJest";
import { ZacInput } from "./input/input";
import { ZacRadio } from "./radio/radio";

const formFields: [string, Type<unknown>, Record<string, unknown>][] = [
  ["a single input field", ZacInput, {}],
  ["a multi input field", ZacRadio, { options: [] }],
];

describe.each(formFields)("%s", (_, component, extraInputs) => {
  let resolveSubmit: () => void;

  async function setup() {
    const form = new FormGroup({
      naam: new FormControl<string | null>("fakeNaam"),
      readonly: new FormControl<string | null>({
        value: "fakeReadonly",
        disabled: true,
      }),
    });

    const { fixture } = await render(component, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      inputs: { form, key: "naam", label: "fakeLabel", ...extraInputs },
    });

    return { form, field: fixture.nativeElement };
  }

  function startSubmit() {
    void new MutationObserver(testQueryClient, {
      mutationFn: () =>
        new Promise<void>((resolve) => {
          resolveSubmit = resolve;
        }),
    }).mutate();
  }

  it("is interactive when nothing is being submitted", async () => {
    const { field } = await setup();

    expect(field).not.toHaveAttribute("inert");
  });

  it("is inert while a submit is pending", async () => {
    const { field } = await setup();

    startSubmit();

    await waitFor(() => expect(field).toHaveAttribute("inert"));
  });

  it("is interactive again once the submit settles, without touching the form's values or disabled controls", async () => {
    const { form, field } = await setup();
    startSubmit();
    await waitFor(() => expect(field).toHaveAttribute("inert"));

    resolveSubmit();

    await waitFor(() => expect(field).not.toHaveAttribute("inert"));
    expect(form.getRawValue()).toEqual({
      naam: "fakeNaam",
      readonly: "fakeReadonly",
    });
    expect(form.controls.naam.enabled).toBe(true);
    expect(form.controls.readonly.disabled).toBe(true);
  });
});
