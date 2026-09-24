/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input, linkedSignal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
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
  readonly title = input<string | undefined>("error-card.title.default");
  readonly text = input<string | undefined>("");
  readonly iconName = input<string | undefined>("indeterminate_question_box");

  protected readonly displayedTitle = linkedSignal(() => this.title());
  protected readonly displayedText = linkedSignal(() => this.text());
  protected readonly displayedIconName = linkedSignal(() => this.iconName());

  constructor() {
    inject(ActivatedRoute)
      .data.pipe(takeUntilDestroyed())
      .subscribe((data) => {
        if (data.title) this.displayedTitle.set(data.title);
        if (data.text) this.displayedText.set(data.text);
        if (data.iconName) this.displayedIconName.set(data.iconName);
      });
  }
}
