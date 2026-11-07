/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Validators } from "@angular/forms";
import { GeneratedType } from "../../../utils/generated-types";
import { AbstractFormField } from "../../model/abstract-form-field";
import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { MedewerkerGroepFormField } from "./medewerker-groep-form-field";

export class MedewerkerGroepFieldBuilder extends AbstractFormFieldBuilder {
  readonly formField: MedewerkerGroepFormField;

  constructor(
    groep?: GeneratedType<"RestGroup">,
    medewerker?: GeneratedType<"RestUser">,
  ) {
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
