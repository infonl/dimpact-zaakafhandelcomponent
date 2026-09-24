/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { KeyValuePipe, NgFor } from "@angular/common";
import {
  Component,
  computed,
  EventEmitter,
  inject,
  input,
  Output,
} from "@angular/core";
import { MatIconButton } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import {
  MatListOption,
  MatSelectionList,
  MatSelectionListChange,
} from "@angular/material/list";
import { MatMenu, MatMenuTrigger } from "@angular/material/menu";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { ZoekenColumn } from "../model/zoeken-column";
import { SortPipe } from "../pipes/sort.pipe";
import { ColumnPickerValue } from "./column-picker-value";

@Component({
  selector: "zac-column-picker",
  templateUrl: "./column-picker.component.html",
  styleUrls: ["./column-picker.component.less"],
  standalone: true,
  imports: [
    NgFor,
    KeyValuePipe,
    MatIconButton,
    MatMenuTrigger,
    MatMenu,
    MatIcon,
    MatSelectionList,
    MatListOption,
    TranslateModule,
    SortPipe,
  ],
})
export class ColumnPickerComponent {
  private readonly translate = inject(TranslateService);

  readonly columnSrc = input(new Map<ZoekenColumn, ColumnPickerValue>());

  @Output() columnsChanged = new EventEmitter<
    Map<ZoekenColumn, ColumnPickerValue>
  >();

  protected readonly columns = computed(() => {
    const columns = this.columnSrc();
    return new Map(
      [...columns.keys()]
        .filter((key) => columns.get(key) !== ColumnPickerValue.STICKY)
        .map((key): [ZoekenColumn, string] => [
          key,
          this.translate.instant(key),
        ]),
    );
  });
  private readonly selection = computed(() => {
    const columns = this.columnSrc();
    return [...columns.keys()].filter(
      (key) => columns.get(key) === ColumnPickerValue.VISIBLE,
    );
  });
  private changed = false;

  protected menuOpened() {
    this.changed = false;
  }

  protected selectionChanged($event: MatSelectionListChange) {
    this.changed = true;
    const columnSrc = this.columnSrc();
    $event.options.forEach((option) =>
      columnSrc.set(
        option.value,
        columnSrc.get(option.value) === ColumnPickerValue.VISIBLE
          ? ColumnPickerValue.HIDDEN
          : ColumnPickerValue.VISIBLE,
      ),
    );
  }

  protected updateColumns() {
    if (this.changed) {
      this.columnsChanged.emit(this.columnSrc());
    }
  }

  protected isSelected(column: ZoekenColumn) {
    return this.selection().includes(column);
  }
}
