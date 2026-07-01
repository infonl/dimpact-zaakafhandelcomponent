/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TemplateRef } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { Observable } from "rxjs";

/**
 * Data for the {@link GenericDialogComponent}.
 *
 * The dialog provides a shared shell (toolbar, message, action buttons and inline error
 * handling) and projects the caller's own field markup through {@link contentTemplate}, so
 * every caller keeps full control over which `zac-input` / `zac-textarea` / `zac-select` it
 * renders while sharing the confirm/cancel and error-retry behaviour.
 */
export type GenericDialogData<
  Form extends FormGroup = FormGroup,
  Result = unknown,
> = {
  /** Reactive form backing the projected field(s). */
  form: Form;
  /**
   * Template with the specific field(s) for this dialog. The form is passed as the implicit
   * context, so callers can write `<ng-template let-form>`.
   */
  contentTemplate: TemplateRef<{ $implicit: Form }>;
  /**
   * Called when the user confirms. On error the dialog stays open, shows the error inline and
   * re-enables the confirm button so the action can be retried without losing any input.
   */
  callback: (form: Form) => Observable<Result>;
  /** Optional message or question shown above the field(s). May contain HTML. */
  melding?: string;
  /** Optional extra explanation shown below the message. */
  uitleg?: string;
  /** Material icon shown in the toolbar. */
  icon?: string;
  /** Translation key used for both the toolbar title and the confirm button. */
  confirmButtonActionKey?: string;
  /** Translation key for the cancel button. */
  cancelButtonActionKey?: string;
};
