/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
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
export class StaticTextComponent implements OnInit, OnChanges {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) value: any;
  @Input({ required: true }) icon!: TextIcon;
  @Input({ required: true }) maxLength!: number;
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
    this.showIcon = this.icon?.showIcon(new FormControl(this.value));
  }
}
