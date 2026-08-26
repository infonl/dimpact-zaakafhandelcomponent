/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormioForm } from "@formio/angular";
import { flattenComponents } from "./form-components";

const HTML5_WIDGET = "html5";

export function applyDefaultSelectWidget(form: Pick<FormioForm, "components">) {
  flattenComponents(form.components)
    .filter((component) => component.type === "select" && !component.widget)
    .forEach((component) => {
      component.widget = HTML5_WIDGET;
    });
  return form;
}
