/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { AbstractFormField } from "../material-form-builder/model/abstract-form-field";

export class DialogData<Value = unknown, Result = unknown> {
  public confirmButtonActionKey = "actie.ja";
  public cancelButtonActionKey: string | null = "actie.annuleren";
  public value: Value = null as Value;
  public icon: string | null = null;

  constructor(
    public formFields: AbstractFormField[],
    public fn?: (result: Result) => Observable<unknown>,
    public melding?: string,
    public uitleg?: string,
  ) {}

  formFieldsInvalid(): boolean {
    return this.formFields.some((field) => field.formControl.invalid);
  }
}
