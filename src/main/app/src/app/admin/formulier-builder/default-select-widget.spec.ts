/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormioForm } from "@formio/angular";
import { applyDefaultSelectWidget } from "./default-select-widget";

function formWith(
  ...components: NonNullable<FormioForm["components"]>
): FormioForm {
  return { display: "form", components };
}

describe(applyDefaultSelectWidget.name, () => {
  it("should give a dropdown without a widget the native one, which a task can open by mouse", () => {
    const form = formWith({ type: "select", key: "uitkomst" });

    applyDefaultSelectWidget(form);

    expect(form.components).toEqual([
      { type: "select", key: "uitkomst", widget: "html5" },
    ]);
  });

  it("should leave a widget the builder chose explicitly alone", () => {
    const form = formWith({
      type: "select",
      key: "uitkomst",
      widget: "choicesjs",
    });

    applyDefaultSelectWidget(form);

    expect(form.components).toEqual([
      { type: "select", key: "uitkomst", widget: "choicesjs" },
    ]);
  });

  it("should leave a field that is not a dropdown alone", () => {
    const form = formWith({ type: "textfield", key: "opmerking" });

    applyDefaultSelectWidget(form);

    expect(form.components).toEqual([{ type: "textfield", key: "opmerking" }]);
  });

  it("should reach a dropdown nested in a panel", () => {
    const form = formWith({
      type: "panel",
      key: "form",
      components: [{ type: "select", key: "uitkomst" }],
    });

    applyDefaultSelectWidget(form);

    expect(form.components).toEqual([
      {
        type: "panel",
        key: "form",
        components: [{ type: "select", key: "uitkomst", widget: "html5" }],
      },
    ]);
  });

  it("should reach a dropdown nested in a column", () => {
    const form = formWith({
      type: "columns",
      key: "kolommen",
      columns: [{ components: [{ type: "select", key: "uitkomst" }] }],
    });

    applyDefaultSelectWidget(form);

    expect(form.components).toEqual([
      {
        type: "columns",
        key: "kolommen",
        columns: [
          {
            components: [{ type: "select", key: "uitkomst", widget: "html5" }],
          },
        ],
      },
    ]);
  });

  it("should reach a dropdown nested in a table row", () => {
    const form = formWith({
      type: "table",
      key: "tabel",
      rows: [[{ components: [{ type: "select", key: "uitkomst" }] }]],
    });

    applyDefaultSelectWidget(form);

    expect(form.components).toEqual([
      {
        type: "table",
        key: "tabel",
        rows: [
          [
            {
              components: [
                { type: "select", key: "uitkomst", widget: "html5" },
              ],
            },
          ],
        ],
      },
    ]);
  });

  it("should handle a textarea, whose 'rows' is a line count rather than nested cells", () => {
    const form = formWith({ type: "textarea", key: "opmerking", rows: 3 });

    applyDefaultSelectWidget(form);

    expect(form.components).toEqual([
      { type: "textarea", key: "opmerking", rows: 3 },
    ]);
  });
});
