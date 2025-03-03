/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { AbstractFormField } from "../material-form-builder/model/abstract-form-field";

export class DialogData<T extends unknown = unknown> {
  public confirmButtonActionKey = "actie.ja";
  public cancelButtonActionKey = "actie.annuleren";
  public value: T = null as T;

  constructor(
    public formFields: AbstractFormField[],
    public fn?: (results: any[]) => Observable<any>,
    public melding?: string,
    public uitleg?: string,
  ) {}

  formFieldsInvalid(): boolean {
    return this.formFields.some((field) => field.formControl.invalid);
  }
}
