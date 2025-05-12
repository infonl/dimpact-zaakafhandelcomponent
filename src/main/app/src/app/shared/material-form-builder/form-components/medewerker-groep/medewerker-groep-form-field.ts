/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
import { GeneratedType } from "../../../utils/generated-types";
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

  groepValue(groep: GeneratedType<"RestGroup">): void {
    this.groep.setValue(groep);
    this.groep.markAsDirty();
  }

  medewerkerValue(medewerker: GeneratedType<"RestUser">) {
    this.medewerker.setValue(medewerker);
    this.medewerker.markAsDirty();
  }

  // This is actually a `FormControl<GeneratedType<"RestGroup"> | string>`
  // When the user searches for a group, the value is a string
  // This will get replaced when moving over to the Angular form builder for all forms
  get groep(): FormControl<GeneratedType<"RestGroup">> {
    return this.formControl.get("groep") as FormControl;
  }

  // This is actually a `FormControl<GeneratedType<"RestUser"> | string>`
  // When the user searches for a group, the value is a string
  // This will get replaced when moving over to the Angular form builder for all forms
  get medewerker(): FormControl<GeneratedType<"RestUser">> {
    return this.formControl.get("medewerker") as FormControl;
  }
}
