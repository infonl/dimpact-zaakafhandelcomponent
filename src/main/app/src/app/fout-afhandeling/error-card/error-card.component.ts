/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";

@Component({
  selector: "zac-error-card",
  templateUrl: "./error-card.component.html",
  standalone: false,
})
export class ErrorCardComponent {
  @Input() title?: string = "error-card.title.default";
  @Input() text?: string = "";
  @Input() iconName?: string = "indeterminate_question_box";
}
