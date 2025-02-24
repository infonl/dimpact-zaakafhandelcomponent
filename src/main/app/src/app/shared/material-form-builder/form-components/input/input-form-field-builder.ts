/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ActionIcon } from "../../../edit/action-icon";
import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { InputFormField } from "./input-form-field";

export class InputFormFieldBuilder extends AbstractFormFieldBuilder {
  readonly formField: InputFormField;

  constructor(value?: any) {
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

  maxlength(maxlength: number, showCount = true): this {
    this.formField.maxlength = maxlength;
    this.formField.showCount = showCount;
    return this;
  }
}
