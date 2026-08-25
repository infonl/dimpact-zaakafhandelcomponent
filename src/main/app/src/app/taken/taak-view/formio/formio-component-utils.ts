/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema } from "@formio/angular";

export function escapeHtml(value: string) {
  return value.replace(
    /[&<>"']/g,
    (character) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
      })[character]!,
  );
}

export function renderFieldError(
  component: ExtendedComponentSchema,
  message: string,
) {
  component.type = "content";
  component.label = "";
  component.input = false;
  component.html = `<div class="zac-unknown-zac-type">${escapeHtml(message)}</div>`;
}
