/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, Input, numberAttribute, OnChanges } from "@angular/core";
import { MatTooltipModule } from "@angular/material/tooltip";

@Component({
  selector: "read-more",
  template: ` <div
      *ngIf="showTooltip"
      matTooltip="{{ text }}"
      [innerHTML]="subText"
    ></div>
    <div *ngIf="!showTooltip" [innerHTML]="text"></div>`,
  standalone: true,
  imports: [NgIf, MatTooltipModule],
})
export class ReadMoreComponent implements OnChanges {
  @Input() text?: string;
  @Input({ transform: numberAttribute }) maxLength = 100;
  protected subText: string | null = null;
  protected showTooltip = false;

  ngOnChanges() {
    this.showTooltip =
      typeof this.text === "string" ? this.text.length > this.maxLength : false;
    this.subText = this.showTooltip
      ? this.text?.substring(0, this.maxLength - 3) + "..."
      : null;
  }
}
