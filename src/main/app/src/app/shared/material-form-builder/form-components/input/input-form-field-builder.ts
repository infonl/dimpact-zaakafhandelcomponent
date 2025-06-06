/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ActionIcon } from "../../../edit/action-icon";
import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { InputFormField } from "./input-form-field";

export class InputFormFieldBuilder<
  T extends string | number | boolean = string,
> extends AbstractFormFieldBuilder<T> {
  readonly formField: InputFormField<T>;

  constructor(value?: T | null) {
    super();
    this.formField = new InputFormField();
    this.formField.initControl(value);
  }

  icon(icon: ActionIcon): this {
    this.formField.icons = [icon];
    return this;
  }

  icons(icons: ActionIcon[]): this {
    this.formField.icons = icons;
    return this;
  }

  // NOTE: intended for making a field disabled but not looking like it's disabled,
  // it should be used for fields that will only display values that will be filled by external components, like a modal
  externalInput() {
    this.formField.externalInput = true;
    return this;
  }

  maxlength(maxlength: number): this {
    this.formField.maxlength = maxlength;
    return this;
  }
}
