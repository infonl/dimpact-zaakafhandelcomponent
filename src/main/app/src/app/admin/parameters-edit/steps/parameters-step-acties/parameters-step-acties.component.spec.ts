/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import {
  createActiesForm,
  createActiesFormForProcessDefinition,
  toUserEventListenerParameters,
} from "./acties-form";
import { ParametersStepActiesComponent } from "./parameters-step-acties.component";

describe(ParametersStepActiesComponent.name, () => {
  const user = userEvent.setup();

  async function setup() {
    const form = createActiesForm([
      { id: "event-1", naam: "Event 1", toelichting: "initial toelichting" },
    ]);
    await render(ParametersStepActiesComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      inputs: { form },
    });
    return form;
  }

  const toelichting = () =>
    within(screen.getByRole("region", { name: "Event 1" })).getByRole(
      "textbox",
      { name: /^toelichting$/i },
    );

  it("lists a panel per actie", async () => {
    await setup();

    expect(screen.getByRole("button", { name: "Event 1" })).toBeVisible();
  });

  it("shows the stored toelichting of an actie", async () => {
    await setup();

    await user.click(screen.getByRole("button", { name: "Event 1" }));

    expect(toelichting()).toHaveValue("initial toelichting");
  });

  it("stores the rewritten toelichting of an actie", async () => {
    const form = await setup();

    await user.click(screen.getByRole("button", { name: "Event 1" }));
    await user.clear(toelichting());
    await user.type(toelichting(), "updated toelichting");

    expect(toUserEventListenerParameters(form)).toEqual([
      { id: "event-1", naam: "Event 1", toelichting: "updated toelichting" },
    ]);
  });

  it("starts the acties of a newly chosen case definition without toelichting", () => {
    const form = createActiesFormForProcessDefinition(
      fromPartial<GeneratedType<"RestProcessDefinition">>({
        userEventListenerDefinitions: [{ id: "event-2", naam: "Event 2" }],
      }),
    );

    expect(toUserEventListenerParameters(form)).toEqual([
      { id: "event-2", naam: "Event 2", toelichting: undefined },
    ]);
  });
});
