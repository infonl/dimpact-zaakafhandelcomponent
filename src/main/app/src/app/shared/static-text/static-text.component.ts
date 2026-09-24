/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass, NgIf } from "@angular/common";
import {
  Component,
  computed,
  EventEmitter,
  input,
  numberAttribute,
  Output,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { TextIcon } from "../edit/text-icon";
import { EmptyPipe } from "../pipes/empty.pipe";
import { ReadMoreComponent } from "../read-more/read-more.component";

@Component({
  selector: "zac-static-text",
  templateUrl: "./static-text.component.html",
  styleUrls: ["./static-text.component.less"],
  imports: [
    NgIf,
    NgClass,
    MatIconModule,
    TranslateModule,
    ReadMoreComponent,
    EmptyPipe,
  ],
})
export class StaticTextComponent<
  T extends string | number | null | undefined = string,
> {
  /**
   * Will get translated automatically
   */
  readonly label = input<string>();
  readonly value = input<T>();
  readonly icon = input<TextIcon | null>();
  readonly maxLength = input<number | undefined, unknown>(undefined, {
    transform: numberAttribute,
  });
  @Output() iconClicked = new EventEmitter<void>();

  protected readonly showIcon = computed(() =>
    Boolean(this.icon()?.showIcon?.(new FormControl(this.value()))),
  );
}
