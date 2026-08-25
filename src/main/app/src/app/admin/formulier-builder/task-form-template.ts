/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormioForm } from "@formio/angular";

/** Without the submit button a task cannot be completed, so it is there before any field is. */
export function createTaskFormTemplate(
  formKey: string,
  title: string,
): FormioForm {
  return {
    display: "form",
    type: "form",
    name: formKey,
    title,
    components: [
      {
        title,
        label: title,
        type: "panel",
        key: "form",
        input: false,
        components: [],
      },
      {
        type: "button",
        label: "Afronden",
        key: "submit",
        disableOnInvalid: true,
        input: true,
        tableView: false,
      },
    ],
  };
}
