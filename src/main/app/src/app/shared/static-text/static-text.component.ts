/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
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
export class StaticTextComponent<T = unknown> implements OnInit, OnChanges {
  @Input() label?: string;
  @Input() value?: T;
  @Input() icon?: TextIcon | null;
  @Input() maxLength?: number;
  @Output() iconClicked = new EventEmitter<void>();

  showIcon = false;

  constructor() {}

  ngOnInit(): void {
    this.setIcon();
  }

  ngOnChanges(): void {
    this.setIcon();
  }

  get hasIcon(): boolean {
    return this.showIcon;
  }

  setIcon(): void {
    this.showIcon = Boolean(this.icon?.showIcon?.(new FormControl(this.value)));
  }
}
