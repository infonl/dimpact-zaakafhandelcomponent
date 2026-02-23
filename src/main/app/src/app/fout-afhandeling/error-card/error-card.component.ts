/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";
import { MatCard, MatCardContent } from "@angular/material/card";
import { MatIcon } from "@angular/material/icon";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";

@Component({
  selector: "zac-error-card",
  templateUrl: "./error-card.component.html",
  styleUrl: "./error-card.component.less",
  host: { class: "error-card-host" },
  imports: [MatCard, MatCardContent, MatIcon, TranslateModule],
})
export class ErrorCardComponent {
  @Input() title?: string = "error-card.title.default";
  @Input() text?: string = "";
  @Input() iconName?: string = "indeterminate_question_box";

  constructor(private readonly route: ActivatedRoute) {
    this.route.data.subscribe((data) => {
      if (data.title) this.title = data.title;
      if (data.text) this.text = data.text;
      if (data.iconName) this.iconName = data.iconName;
    });
  }
}
