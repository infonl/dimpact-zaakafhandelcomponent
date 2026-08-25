/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema } from "@formio/angular";
import { renderFieldError } from "./formio-component-utils";

describe(renderFieldError.name, () => {
  function render(message: string, component: ExtendedComponentSchema = {}) {
    renderFieldError(component, message);
    return component;
  }

  it("should turn the field into content that cannot be submitted", () => {
    const component = render("Something went wrong");

    expect(component.type).toBe("content");
    expect(component.input).toBe(false);
  });

  it("should render the message in place of the field", () => {
    expect(render("Something went wrong").html).toBe(
      '<div class="zac-unknown-zac-type">Something went wrong</div>',
    );
  });

  it("should drop the label, because the message replaces it", () => {
    expect(render("Something went wrong", { label: "Zaaknummer" }).label).toBe(
      "",
    );
  });

  it("should escape the message so a form value in it cannot inject markup", () => {
    const component = render(
      'Zaak property "<img src=x onerror=alert(1)>" holds an object',
    );

    expect(component.html).not.toContain("<img");
    expect(component.html).toContain("&lt;img");
    expect(component.html).toContain("&quot;");
  });

  it("should replace an input the form already configured", () => {
    const component = render("Something went wrong", {
      type: "textfield",
      key: "ZO_Zaakveld",
      input: true,
      defaultValue: "ZAAK-2026-0000000835",
    });

    expect(component.type).toBe("content");
    expect(component.input).toBe(false);
    expect(component.key).toBe("ZO_Zaakveld");
  });
});
