/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, Type } from "@angular/core";
import { DocumentenLijstComponent } from "./form-components/documenten-lijst/documenten-lijst.component";
import { InputComponent } from "./form-components/input/input.component";
import { ReadonlyComponent } from "./form-components/readonly/readonly.component";
import { SelectComponent } from "./form-components/select/select.component";
import { TextareaComponent } from "./form-components/textarea/textarea.component";
import { AbstractFormField } from "./model/abstract-form-field";
import { FieldType } from "./model/field-type.enum";
import { FormComponent } from "./model/form-component";
import { FormItem } from "./model/form-item";

@Injectable({
  providedIn: "root",
})
export class MaterialFormBuilderService {
  constructor() {}

  public getFormItem(formField: AbstractFormField): FormItem {
    return new FormItem(
      MaterialFormBuilderService.getType(formField.fieldType),
      formField,
    );
  }

  private static getType(type: FieldType): Type<FormComponent> {
    switch (type) {
      case FieldType.READONLY:
        return ReadonlyComponent;
      case FieldType.INPUT:
        return InputComponent;
      case FieldType.TEXTAREA:
        return TextareaComponent;
      case FieldType.SELECT:
        return SelectComponent;
      case FieldType.DOCUMENTEN_LIJST:
        return DocumentenLijstComponent;
      default:
        throw new Error(`Unknown type: '${type}'`);
    }
  }
}
