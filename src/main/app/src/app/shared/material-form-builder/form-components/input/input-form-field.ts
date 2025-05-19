/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { EventEmitter } from "@angular/core";
import { Subject } from "rxjs";
import { ActionIcon } from "../../../edit/action-icon";
import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class InputFormField<T extends string | number = string> extends AbstractFormControlField<T> {
  fieldType: FieldType = FieldType.INPUT;
  icons: ActionIcon[] = [];
  clicked = new Subject<unknown>();
  onClear = new EventEmitter<void>();
  maxlength: number;
  externalInput = false;

  constructor() {
    super();
  }
}
