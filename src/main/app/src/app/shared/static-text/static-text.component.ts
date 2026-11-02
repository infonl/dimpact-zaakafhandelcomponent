/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass } from "@angular/common";
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  numberAttribute,
  output,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatIconModule } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import { TextIcon } from "../edit/text-icon";
import { EmptyPipe } from "../pipes/empty.pipe";
import { ReadMoreComponent } from "../read-more/read-more.component";

@Component({
  selector: "zac-static-text",
  templateUrl: "./static-text.component.html",
  styleUrls: ["./static-text.component.less"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    NgClass,
    MatIconModule,
    TranslatePipe,
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
  readonly iconClicked = output<void>();

  protected readonly showIcon = computed(() =>
    Boolean(this.icon()?.showIcon?.(new FormControl(this.value()))),
  );
}
