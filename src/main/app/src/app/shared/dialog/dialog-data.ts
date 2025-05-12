/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { AbstractFormField } from "../material-form-builder/model/abstract-form-field";

export class DialogData<Value = unknown, Result = unknown> {
  public confirmButtonActionKey: string | null = "actie.ja";
  public cancelButtonActionKey: string | null = "actie.annuleren";
  public value: Value = null as Value;
  public icon: string;

  constructor(
    public options: {
      formFields: AbstractFormField[];
      // TODO: the `result` type should be based on the form fields
      // right now this is impossible as `AbstractFormField` always return `any` as a field value
      callback?: (result: Result) => Observable<unknown>;
      melding?: string;
      uitleg?: string;
      confirmButtonActionKey?: string | null;
      cancelButtonActionKey?: string | null;
      icon: string;
    },
  ) {
    this.icon = options.icon;

    if (options.confirmButtonActionKey !== undefined) {
      this.confirmButtonActionKey = options.confirmButtonActionKey;
    }
    if (options.cancelButtonActionKey !== undefined) {
      this.cancelButtonActionKey = options.cancelButtonActionKey;
    }
  }

  formFieldsInvalid(): boolean {
    return this.options.formFields.some((field) => field.formControl.invalid);
  }
}
