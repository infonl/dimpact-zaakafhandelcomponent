/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import {
  AbstractControl,
  FormControl,
  ValidatorFn,
  Validators,
} from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { tap } from "rxjs/operators";
import { IdentityService } from "../../../../identity/identity.service";
import { OrderUtil } from "../../../order/order-util";
import { GeneratedType } from "../../../utils/generated-types";
import { FormComponent } from "../../model/form-component";
import { AutocompleteValidators } from "../autocomplete/autocomplete-validators";
import { MedewerkerGroepFormField } from "./medewerker-groep-form-field";

@Component({
  templateUrl: "./medewerker-groep.component.html",
  styleUrls: ["./medewerker-groep.component.less"],
})
export class MedewerkerGroepComponent extends FormComponent implements OnInit {
  private groups: GeneratedType<"RestGroup">[] = [];
  private users: GeneratedType<"RestUser">[] = [];

  public data: MedewerkerGroepFormField;
  protected filteredGroups: GeneratedType<"RestGroup">[] = [];
  protected filteredUsers: GeneratedType<"RestUser">[] = [];

  constructor(
    public translate: TranslateService,
    public identityService: IdentityService,
  ) {
    super();
  }

  ngOnInit(): void {
    this.data.medewerker.disable();

    this.data.groep.valueChanges.subscribe((value) => {
      const filterValue = typeof value === "string" ? value : value?.naam;
      this.filteredGroups = this.groups.filter(
        ({ naam }) =>
          !value || naam.toLowerCase().includes(filterValue.toLowerCase()),
      );

      this.data.medewerker.reset();
      // The `MedewerkerGroepFormField` has the wrong type so we overwrite it here
      // This will get replaced when moving over to the Angular form builder for all forms
      this.data.medewerker.setValue(
        null as unknown as GeneratedType<"RestUser">,
      );

      if (!value) {
        this.data.medewerker.disable();
        return;
      }

      const group = this.groups.find(({ id }) => id === value.id);

      if (!group) return;

      this.data.medewerker.enable();
      this.setUsers(this.data.medewerker.defaultValue?.id);
    });

    this.data.medewerker.valueChanges.subscribe((value) => {
      const filterValue = typeof value === "string" ? value : value?.naam;
      this.filteredUsers = value
        ? this.users.filter(({ naam }) =>
            naam.toLowerCase().includes(filterValue.toLowerCase()),
          )
        : this.users;
    });

    this.setGroups();
  }

  private setGroups(): void {
    this.identityService
      .listGroups(this.data.zaaktypeUuid)
      .pipe(tap((value) => value.sort(OrderUtil.orderBy("naam"))))
      .subscribe((groups) => {
        this.groups = this.filteredGroups = groups;

        const validators: ValidatorFn[] = [];
        validators.push(AutocompleteValidators.optionInList(groups));

        validators.push((control: AbstractControl) => {
          if (!control.value || typeof control.value !== "object") return null;

          const naamToolong =
            control.value.naam?.length > this.data.maxGroupNameLength;

          return naamToolong ? { naamToolong: true } : null;
        });
        validators.push((control: AbstractControl) => {
          if (!control.value || typeof control.value !== "object") return null;

          const idTooLong =
            control.value.id?.length > this.data.maxGroupIdLength;

          return idTooLong ? { idTooLong: true } : null;
        });

        if (this.data.groep.hasValidator(Validators.required)) {
          validators.push(Validators.required);
        }

        this.data.groep.setValidators(validators);
        this.data.groep.updateValueAndValidity();

        const group = groups.find(
          ({ id }) => id === this.data.groep.defaultValue.id,
        );

        this.data.groep.setValue(
          group ?? (null as unknown as GeneratedType<"RestGroup">),
        );
      });
  }

  private setUsers(defaultUserId?: string) {
    this.identityService
      .listUsersInGroup(this.data.groep.value.id)
      .pipe(tap((value) => value.sort(OrderUtil.orderBy("naam"))))
      .subscribe((users) => {
        this.users = this.filteredUsers = users;

        const validators: ValidatorFn[] = [];
        validators.push(AutocompleteValidators.optionInList(users));
        if (this.data.medewerker.hasValidator(Validators.required)) {
          validators.push(Validators.required);
        }

        this.data.medewerker.setValidators(validators);

        const user = users.find(({ id }) => id === defaultUserId);

        this.data.medewerker.setValue(
          user ?? (null as unknown as GeneratedType<"RestUser">),
        );
      });
  }

  displayFn(obj: GeneratedType<"RestUser"> | GeneratedType<"RestGroup">) {
    return obj?.naam ?? "";
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

  clearField(formControl?: FormControl) {
    formControl?.setValue(null);
  }
}
