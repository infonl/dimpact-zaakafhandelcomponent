/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, input, numberAttribute } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";

@Component({
  selector: "read-more",
  template: ` @if (showTooltip()) {
      <div matTooltip="{{ text() }}" [innerHTML]="subText()"></div>
    }
    @if (!showTooltip()) {
      <div [innerHTML]="text()"></div>
    }`,
  standalone: true,
  imports: [MatTooltipModule],
})
export class ReadMoreComponent {
  readonly text = input<string>();
  readonly maxLength = input(100, { transform: numberAttribute });

  protected readonly showTooltip = computed(() => {
    const text = this.text();
    return typeof text === "string" ? text.length > this.maxLength() : false;
  });

  protected readonly subText = computed(() =>
    this.showTooltip()
      ? this.text()?.substring(0, this.maxLength() - 3) + "..."
      : null,
  );
}
