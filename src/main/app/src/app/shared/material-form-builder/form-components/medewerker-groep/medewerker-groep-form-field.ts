/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
import { AbstractFormGroupField } from "../../model/abstract-form-group-field";
import { FieldType } from "../../model/field-type.enum";
import {GeneratedType} from "../../../utils/generated-types";

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

  groepValue(groep: GeneratedType<'RestGroup'>): void {
    this.groep.setValue(groep);
    this.groep.markAsDirty();
  }

  medewerkerValue(medewerker: GeneratedType<'RestUser'>) {
    this.medewerker.setValue(medewerker);
    this.medewerker.markAsDirty();
  }

  get groep(): FormControl<GeneratedType<'RestGroup'>> {
    return this.formControl.get("groep") as FormControl;
  }

  get medewerker(): FormControl<GeneratedType<'RestUser'>> {
    return this.formControl.get("medewerker") as FormControl;
  }
}
