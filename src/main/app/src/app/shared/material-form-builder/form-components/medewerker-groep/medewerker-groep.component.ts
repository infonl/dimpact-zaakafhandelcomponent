/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnDestroy, OnInit } from "@angular/core";
import {
  AbstractControl,
  FormControl,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Observable, Subscription } from "rxjs";
import { map, startWith, tap } from "rxjs/operators";
import { IdentityService } from "../../../../identity/identity.service";
import { OrderUtil } from "../../../order/order-util";
import { FormComponent } from "../../model/form-component";
import { AutocompleteValidators } from "../autocomplete/autocomplete-validators";
import { MedewerkerGroepFormField } from "./medewerker-groep-form-field";
import {GeneratedType} from "../../../utils/generated-types";

@Component({
  templateUrl: "./medewerker-groep.component.html",
  styleUrls: ["./medewerker-groep.component.less"],
})
export class MedewerkerGroepComponent
  extends FormComponent
  implements OnInit, OnDestroy
{
  data: MedewerkerGroepFormField;
  groepen: GeneratedType<'RestGroup'>[];
  filteredGroepen: Observable<GeneratedType<'RestGroup'>[]>;
  medewerkers: GeneratedType<'RestUser'>[];
  filteredMedewerkers: Observable<GeneratedType<'RestUser'>[]>;
  subscriptions$: Subscription[] = [];

  hasGroep: boolean = false;
  hasMedewerker: boolean = false;

  constructor(
    public translate: TranslateService,
    public identityService: IdentityService,
  ) {
    super();
  }

  ngOnInit(): void {
    this.initGroepen();

    this.subscriptions$.push(
      this.data.groep.valueChanges.subscribe((value) => {
        if (!this.data.groep.dirty) {
          return;
        }

        if (this.data.groep.valid) {
          this.data.medewerker.enable();
          this.getMedewerkers();
        } else if (!this.data.groep.value) {
          this.data.medewerker.disable();
        }
        this.data.medewerker.setValue(null);
      }),
    );
    if (!this.data.groep.value) {
      this.data.medewerker.disable();
    } else {
      this.getMedewerkers();
    }

    this.data.groep.valueChanges.subscribe((value) => {
      this.hasGroep = !!value;
    });

    this.data.medewerker.valueChanges.subscribe((value) => {
      this.hasMedewerker = !!value;
    });
  }

  ngOnDestroy() {
    this.subscriptions$.forEach((s) => s.unsubscribe());
  }

  initGroepen(): void {
    this.identityService
      .listGroups()
      .pipe(tap((value) => value.sort(OrderUtil.orderBy("naam"))))
      .subscribe((groepen) => {
        this.groepen = groepen;
        const validators: ValidatorFn[] = [];
        validators.push(AutocompleteValidators.optionInList(groepen));
        if (this.data.groep.hasValidator(Validators.required)) {
          validators.push(Validators.required);
        }
        validators.push((control: AbstractControl): ValidationErrors | null => {
          if (!control.value || typeof control.value !== "object") {
            return null; // or return an error if this is unexpected
          }

          const naamToolong =
            control.value.naam &&
            control.value.naam.length > this.data.maxGroupNameLength;

          return naamToolong ? { naamToolong: true } : null;
        });
        validators.push((control: AbstractControl): ValidationErrors | null => {
          if (!control.value || typeof control.value !== "object") {
            return null; // or return an error if this is unexpected
          }

          const idTooLong =
            control.value.id &&
            control.value.id.length > this.data.maxGroupIdLength;

          return idTooLong ? { idTooLong: true } : null;
        });

        this.data.groep.setValidators(validators);
        this.data.groep.updateValueAndValidity();

        this.filteredGroepen = this.data.groep.valueChanges.pipe(
          startWith(""),
          map((groep) =>
            groep ? this._filterGroepen(groep) : this.groepen.slice(),
          ),
        );
        if (this.data.groep.defaultValue) {
          this.data.groep.setValue(
            groepen.find(
              (groep) => groep.id === this.data.groep.defaultValue.id,
            ),
          );
        }
      });
  }

  private getMedewerkers(defaultMedewerkerId?: string) {
    this.medewerkers = [];
    this.identityService.listUsersInGroup(this.data.groep.value.id)
      .pipe(tap((value) => value.sort(OrderUtil.orderBy("naam"))))
      .subscribe((medewerkers) => {
        this.medewerkers = medewerkers;
        const validators: ValidatorFn[] = [];
        validators.push(AutocompleteValidators.optionInList(medewerkers));
        if (this.data.medewerker.hasValidator(Validators.required)) {
          validators.push(Validators.required);
        }
        this.data.medewerker.setValidators(validators);
        this.filteredMedewerkers = this.data.medewerker.valueChanges.pipe(
          startWith(""),
          map((medewerker) =>
            medewerker
              ? this._filterMedewerkers(medewerker)
              : this.medewerkers.slice(),
          ),
        );
        if (defaultMedewerkerId) {
          this.data.medewerker.setValue(
            medewerkers.find(
              (medewerker) => medewerker.id === defaultMedewerkerId,
            ),
          );
        }
      });
  }

  displayFn(obj: GeneratedType<'RestUser'> | GeneratedType<'RestGroup'>): string {
    return obj?.naam ?? "";
  }

  private _filterGroepen(value: string | GeneratedType<'RestGroup'>): GeneratedType<'RestGroup'>[] {
    if (typeof value === "object") {
      return [value];
    }
    const filterValue = value.toLowerCase();
    return this.groepen.filter((groep) =>
      groep.naam.toLowerCase().includes(filterValue),
    );
  }

  private _filterMedewerkers(value: string | GeneratedType<'RestUser'>): GeneratedType<'RestUser'>[] {
    if (typeof value === "object") {
      return [value];
    }
    const filterValue = value.toLowerCase();
    return this.medewerkers.filter((medewerker) =>
      medewerker.naam.toLowerCase().includes(filterValue),
    );
  }

  getMessage(formControl: FormControl, label: string): string {
    if (formControl.hasError("required")) {
      return this.translate.instant("msg.error.required", { label: label });
    }
    if (formControl.hasError("match")) {
      return this.translate.instant("msg.error.invalid.match");
    }
    if (formControl.hasError("idTooLong")) {
      return this.translate.instant("msg.error.group.invalid.id", {
        max: this.data.maxGroupIdLength,
      });
    }
    if (formControl.hasError("naamToolong")) {
      return this.translate.instant("msg.error.group.invalid.name", {
        max: this.data.maxGroupNameLength,
      });
    }
    return this.translate.instant("msg.error.field.generic");
  }

  clearField(formControl: FormControl) {
    if (formControl) {
      formControl.setValue(null);
    }
  }
}
