/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema } from "@formio/angular";
import { escapeHtml } from "../../../shared/utils/escape-html";

export function renderFieldError(
  component: ExtendedComponentSchema,
  message: string,
) {
  component.type = "content";
  component.label = "";
  component.input = false;
  component.html = `<div class="zac-unknown-zac-type">${escapeHtml(message)}</div>`;
}
