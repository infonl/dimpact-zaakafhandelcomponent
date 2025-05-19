/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { EventEmitter } from "@angular/core";
import { Observable, tap } from "rxjs";
import { OrderUtil } from "../../order/order-util";
import { AbstractFormControlField } from "./abstract-form-control-field";

/**
 * Abstract class voor Form Fields die meerdere waardes tonen (checkbox, radiobutton, select)
 * Deze componenten hebben een compare methode nodig om te bepalen welke value geselecteerd moet worden in de lijst.
 */
export abstract class AbstractChoicesFormField<T extends Record<string, unknown> = Record<string, unknown>> extends AbstractFormControlField<T> {
  optionsChanged$ = new EventEmitter<void>();
  private options$ = new Observable<T[]>();
  private valueOptions: T[] = [];
  public optionLabel: string | null = null;
  public optionSuffix: string | null = null;
  public optionValue: string | null = null;
  public optionOrderFn?: (a: T, b: T) => number;
  public settings: {
    translateLabels?: boolean;
    capitalizeFirstLetter?: boolean;
  } = {};

  protected constructor() {
    super();
  }

  compareWithFn = (object1: T, object2: T): boolean => {
    if (object1 && object2) {
      return this.optionValue
        ? this.compare(object1, object2, this.optionValue)
        : this.optionLabel
          ? this.compare(object1, object2, this.optionLabel)
          : object1 === object2;
    }
    return false;
  };

  private compare(object1: T, object2: T, field: keyof T): boolean {
    return (
      object1 === object2[field] ||
      object1[field] === object2 ||
      object1[field] === object2[field]
    );
  }

  getOption(value: T) {
    for (const option of this.valueOptions) {
      if (this.compareWithFn(value, option)) {
        return option;
      }
    }
    return null;
  }

  get options() {
    return this.options$;
  }

  set options(options: Observable<T[]>) {
    this.valueOptions = [];
    this.options$ = options.pipe(
      tap((value) => {
        this.valueOptions = value;
        value?.sort(this.optionOrderFn || OrderUtil.orderBy(this.optionLabel as null)); // as null to make TS happy
      }),
    );
    this.optionsChanged$.next();
  }
}
