/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  numberAttribute,
  OnChanges,
  OnInit,
  Output,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { TextIcon } from "../edit/text-icon";

@Component({
  selector: "zac-static-text",
  templateUrl: "./static-text.component.html",
  styleUrls: ["./static-text.component.less"],
})
export class StaticTextComponent<
    T extends string | number | null | undefined = string,
  >
  implements OnInit, OnChanges
{
  /**
   * Will get translated automatically
   */
  @Input() label?: string;
  @Input() value: T = "" as T;
  @Input() icon?: TextIcon | null;
  @Input({ transform: numberAttribute }) maxLength?: number;
  @Output() iconClicked = new EventEmitter<void>();

  showIcon = false;

  ngOnInit() {
    this.setIcon();
  }

  ngOnChanges() {
    this.setIcon();
  }

  setIcon() {
    this.showIcon = Boolean(this.icon?.showIcon?.(new FormControl(this.value)));
  }
}
