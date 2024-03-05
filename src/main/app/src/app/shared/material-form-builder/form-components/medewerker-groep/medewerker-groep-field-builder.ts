/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ValidatorFn, Validators } from "@angular/forms";
import { Group } from "../../../../identity/model/group";
import { User } from "../../../../identity/model/user";
import { AbstractFormField } from "../../model/abstract-form-field";
import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { MedewerkerGroepFormField } from "./medewerker-groep-form-field";

export class MedewerkerGroepFieldBuilder extends AbstractFormFieldBuilder {
  readonly formField: MedewerkerGroepFormField;

  constructor(groep?: Group, medewerker?: User) {
    super();
    this.formField = new MedewerkerGroepFormField();
    this.formField.initControl({
      groep: AbstractFormField.formControlInstance(groep),
      medewerker: AbstractFormField.formControlInstance(medewerker),
    });
    this.maxGroupIdlength(24);
    this.maxGroupNamelength(50);
  }

  groepLabel(groepLabel: string): this {
    this.formField.groepLabel = groepLabel;
    return this;
  }

  medewerkerLabel(medewerkerLabel: string): this {
    this.formField.medewerkerLabel = medewerkerLabel;
    return this;
  }

  validators(...validators: ValidatorFn[]): this {
    throw new Error("Not implemented");
  }

  groepRequired(): this {
    this.formField.groep.setValidators(Validators.required);
    this.formField.required = true;
    return this;
  }

  medewerkerRequired(): this {
    this.formField.medewerker.setValidators(Validators.required);
    this.formField.required = true;
    return this;
  }

  maxGroupIdlength(maxlength: number): this {
    this.formField.maxGroupIdLength = maxlength;
    return this;
  }

  maxGroupNamelength(maxlength: number): this {
    this.formField.maxGroupNameLength = maxlength;
    return this;
  }

  validate() {
    if (!this.formField.id) {
      throw new Error("id is required");
    }
  }
}
