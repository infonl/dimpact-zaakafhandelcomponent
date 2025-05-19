/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl, 2025 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Observable, Subscription } from "rxjs";
import { map, startWith } from "rxjs/operators";
import { FormComponent } from "../../model/form-component";
import { AutocompleteFormField } from "./autocomplete-form-field";
import { AutocompleteValidators } from "./autocomplete-validators";

@Component({
  selector: "zac-autocomplete",
  templateUrl: "./autocomplete.component.html",
  styleUrls: ["./autocomplete.component.less"],
})
export class AutocompleteComponent
  extends FormComponent
  implements OnInit, OnDestroy
{
  data: AutocompleteFormField;

  options: Record<string, unknown>[] = [];
  filteredOptions = new Observable<unknown[]>();
  optionsChanged$: Subscription;

  constructor(public translate: TranslateService) {
    super();
  }

  ngOnInit() {
    this.initOptions();
    this.optionsChanged$ = this.data.optionsChanged$.subscribe(() => {
      this.data.formControl.clearAsyncValidators();
      this.initOptions();
      this.data.formControl.setValue(this.data.formControl.value); // force validation on new options
    });
  }

  initOptions() {
    this.data.formControl.setAsyncValidators(
      AutocompleteValidators.asyncOptionInList(this.data.options),
    );
    this.data.formControl.updateValueAndValidity();

    this.data.options.subscribe((options) => {
      this.options = options;

      this.filteredOptions = this.data.formControl.valueChanges.pipe(
        startWith(""),
        map((value) => {
          if (value === null) {
            return null;
          }

          switch (typeof value) {
            case "string":
              return value;
            case "object":
              return value[String(this.data.optionLabel)];
            default:
              return null;
          }
        }),
        map((name) =>
          name ? this._filter(String(name)) : this.options?.slice(),
        ),
      );
    });
  }

  displayFn = (obj: unknown): string => {
    return obj?.[String(this.data.optionLabel)] ?? obj;
  };

  private _filter(filter: string): unknown[] {
    const filterValue = filter.toLowerCase();

    return this.options.filter((option) => {
      return String(option[String(this.data.optionLabel)])
        .toLowerCase()
        .includes(filterValue);
    });
  }

  isEditing() {
    return Boolean(this.data.formControl.value);
  }

  clear() {
    this.data.formControl.setValue(null);
  }

  ngOnDestroy(): void {
    this.optionsChanged$.unsubscribe();
  }
}
