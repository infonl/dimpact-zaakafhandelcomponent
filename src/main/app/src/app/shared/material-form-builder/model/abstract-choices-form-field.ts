/*
 * SPDX-FileCopyrightText: 2021 Atos
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
export abstract class AbstractChoicesFormField extends AbstractFormControlField {
  optionsChanged$ = new EventEmitter<void>();
  private options$: Observable<any[]> = new Observable();
  private valueOptions: any[] = [];
  public optionLabel: string | null = null;
  public optionSuffix: string | null = null;
  public optionValue: string | null = null;
  public optionOrderFn?: (a: any, b: any) => number;
  public settings: {
    translateLabels?: boolean;
    capitalizeFirstLetter?: boolean;
  } = {};

  protected constructor() {
    super();
  }

  compareWithFn = (object1: any, object2: any): boolean => {
    if (object1 && object2) {
      return this.optionValue
        ? this.compare(object1, object2, this.optionValue)
        : this.optionLabel
          ? this.compare(object1, object2, this.optionLabel)
          : object1 === object2;
    }
    return false;
  };

  private compare(object1: any, object2: any, field: string): boolean {
    return (
      object1 === object2[field] ||
      object1[field] === object2 ||
      object1[field] === object2[field]
    );
  }

  getOption(value: any) {
    for (const option of this.valueOptions) {
      if (this.compareWithFn(value, option)) {
        return option;
      }
    }
    return null;
  }

  get options(): Observable<any[]> {
    return this.options$;
  }

  set options(options: Observable<any[]>) {
    this.valueOptions = [];
    this.options$ = options.pipe(
      tap((value) => {
        this.valueOptions = value;
        value?.sort(this.optionOrderFn || OrderUtil.orderBy(this.optionLabel));
      }),
    );
    this.optionsChanged$.next();
  }
}
