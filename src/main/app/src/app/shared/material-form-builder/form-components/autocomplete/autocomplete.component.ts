/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AfterViewInit, Component, OnDestroy } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Observable, Subject, Subscription } from "rxjs";
import { map, startWith, takeUntil } from "rxjs/operators";
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
  implements AfterViewInit, OnDestroy
{
  data: AutocompleteFormField;

  options: any[];
  filteredOptions: Observable<any[]>;
  optionsChanged$: Subscription;
  destroy$ = new Subject<void>();

  constructor(public translate: TranslateService) {
    super();
  }

  ngAfterViewInit() {
    this.initOptions();
    this.data.optionsChanged$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.data.formControl.clearAsyncValidators();
      this.initOptions();
      this.data.formControl.setValue(this.data.formControl.value); // force validation on new options
    });
  }

  initOptions() {
    this.data.formControl.setAsyncValidators(
      AutocompleteValidators.asyncOptionInList(this.data.options),
    );
    this.data.options.pipe(takeUntil(this.destroy$)).subscribe((options) => {
      this.options = options;

      this.filteredOptions = this.data.formControl.valueChanges.pipe(
        startWith(""),
        map((value) =>
          typeof value === "string"
            ? value
            : value
              ? value[this.data.optionLabel]
              : null,
        ),
        map((name) => (name ? this._filter(name) : this.options.slice())),
      );
    });
  }

  isSearching(): boolean {
    return typeof this.data.formControl.value === "string";
  }

  displayFn = (obj: any): string => {
    return obj && obj[this.data.optionLabel] ? obj[this.data.optionLabel] : obj;
  };

  private _filter(filter: string): any[] {
    const filterValue = filter.toLowerCase();

    return this.options.filter((option) =>
      option[this.data.optionLabel].toLowerCase().includes(filterValue),
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
