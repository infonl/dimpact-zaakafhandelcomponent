/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TemplateRef } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { Observable } from "rxjs";

export type GenericDialogData<
  Form extends FormGroup = FormGroup,
  Result = unknown,
> = {
  form: Form;
  contentTemplate: TemplateRef<{ $implicit: Form }>;
  callback: (form: Form) => Observable<Result>;
  melding?: string;
  uitleg?: string;
  icon?: string;
  confirmButtonActionKey?: string;
  cancelButtonActionKey?: string;
};
