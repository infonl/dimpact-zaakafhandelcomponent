/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely, 2025 Dimpact
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

  options: Record<string, string>[];
  filteredOptions: Observable<any[]>;
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
          return typeof value === "string"
            ? value
            : typeof value === "object" && value !== null
              ? value[this.data.optionLabel]
              : null;
        }),
        map((name) => (name ? this._filter(name) : this.options?.slice())),
      );
    });
  }

  displayFn = (obj: any): string => {
    return obj?.[this.data.optionLabel] ?? obj;
  };

  private _filter(filter: string): any[] {
    const filterValue = filter.toLowerCase();

    return this.options.filter((option) => {
      return option[this.data.optionLabel].toLowerCase().includes(filterValue);
    });
  }

  isEditing(): boolean {
    return Boolean(this.data.formControl.value);
  }

  clear() {
    this.data.formControl.setValue(null);
  }

  ngOnDestroy(): void {
    this.optionsChanged$.unsubscribe();
  }
}
