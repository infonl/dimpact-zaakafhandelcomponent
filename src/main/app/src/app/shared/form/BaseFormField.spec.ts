/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Type } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { MutationObserver } from "@tanstack/angular-query-experimental";
import { render, screen, waitFor } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
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
      imports: [NoopAnimationsModule],
      providers: [provideTranslateService()],
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

@Component({
  template: `
    <zac-input [form]="form" key="naam" />
    <button type="button" (click)="clearSilently()">wis stil</button>
  `,
  imports: [ZacInput],
})
class SilentChangeHostComponent {
  readonly form = new FormGroup({
    naam: new FormControl<string | null>("fakeNaam"),
  });

  clearSilently() {
    this.form.controls.naam.setValue(null, { emitEvent: false });
  }
}

describe("a form field whose value code changes without events", () => {
  it("shows the new value once its parent is checked again", async () => {
    await render(SilentChangeHostComponent, {
      imports: [NoopAnimationsModule],
      providers: [provideTranslateService()],
    });
    const user = userEvent.setup();
    expect(await screen.findByText("clear")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "wis stil" }));

    await waitFor(() =>
      expect(screen.queryByText("clear")).not.toBeInTheDocument(),
    );
  });
});
