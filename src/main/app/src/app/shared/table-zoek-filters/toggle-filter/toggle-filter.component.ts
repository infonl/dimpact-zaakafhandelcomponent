/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgSwitch, NgSwitchCase } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { ToggleSwitchOptions } from "./toggle-switch-options";

@Component({
  selector: "zac-toggle-filter",
  templateUrl: "./toggle-filter.component.html",
  styleUrls: ["./toggle-filter.component.less"],
  standalone: true,
  imports: [MatButtonModule, MatIconModule, NgSwitch, NgSwitchCase],
})
export class ToggleFilterComponent {
  @Input() protected selected: ToggleSwitchOptions =
    ToggleSwitchOptions.INDETERMINATE;
  @Input() protected checkedIcon = "check_circle";
  @Input() protected unCheckedIcon = "cancel";
  @Input() protected indeterminateIcon = "radio_button_unchecked";
  @Output() public changed = new EventEmitter<ToggleSwitchOptions>();

  protected readonly toggleSwitchOptions = ToggleSwitchOptions;

  protected toggle() {
    switch (this.selected) {
      case ToggleSwitchOptions.CHECKED:
        this.selected = ToggleSwitchOptions.UNCHECKED;
        break;
      case ToggleSwitchOptions.UNCHECKED:
        this.selected = ToggleSwitchOptions.INDETERMINATE;
        break;
      case ToggleSwitchOptions.INDETERMINATE:
        this.selected = ToggleSwitchOptions.CHECKED;
        break;
    }

    this.changed.emit(this.selected);
  }
}
