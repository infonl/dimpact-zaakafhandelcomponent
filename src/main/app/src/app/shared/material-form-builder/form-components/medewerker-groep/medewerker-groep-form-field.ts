/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
import { Group } from "../../../../identity/model/group";
import { User } from "../../../../identity/model/user";
import { AbstractFormGroupField } from "../../model/abstract-form-group-field";
import { FieldType } from "../../model/field-type.enum";

export class MedewerkerGroepFormField extends AbstractFormGroupField {
  fieldType = FieldType.MEDEWERKER_GROEP;
  groepLabel: string;
  medewerkerLabel: string;
  maxlength: number;

  maxGroupNameLength: number;
  maxGroupIdLength: number;

  constructor() {
    super();
  }

  /**
   * implements own readonly view, dont use the default read-only-component
   */
  hasReadonlyView() {
    return true;
  }

  groepValue(groep: Group): void {
    this.groep.setValue(groep);
    this.groep.markAsDirty();
  }

  medewerkerValue(medewerker: User) {
    this.medewerker.setValue(medewerker);
    this.medewerker.markAsDirty();
  }

  get groep(): FormControl<Group> {
    return this.formControl.get("groep") as FormControl;
  }

  get medewerker(): FormControl<User> {
    return this.formControl.get("medewerker") as FormControl;
  }
}
