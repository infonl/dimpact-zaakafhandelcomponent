/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Directive } from "@angular/core";
import { MatFormFieldControl } from "@angular/material/form-field";
import {
  MatInput,
  getMatInputUnsupportedTypeError,
} from "@angular/material/input";

// copied from https://github.com/angular/components/blob/main/src/material/input/input.ts
@Directive({
  selector: `input[matFileInput]`,
  exportAs: "matFileInput",
  host: {
    class: "mat-mdc-input-element",
    // The BaseMatInput parent class adds `mat-input-element`, `mat-form-field-control` and
    // `mat-form-field-autofill-control` to the CSS class list, but this should not be added for
    // this MDC equivalent input.
    "[class.mat-input-server]": "_isServer",
    "[class.mat-mdc-form-field-textarea-control]":
      "_isInFormField && _isTextarea",
    "[class.mat-mdc-form-field-input-control]": "_isInFormField",
    "[class.mdc-text-field__input]": "_isInFormField",
    "[class.mat-mdc-native-select-inline]": "_isInlineSelect()",
    // Native input properties that are overwritten by Angular inputs need to be synced with
    // the native input element. Otherwise property bindings for those don't work.
    "[id]": "id",
    "[disabled]": "disabled",
    "[required]": "required",
    "[attr.name]": "name || null",
    "[attr.readonly]": "readonly && !_isNativeSelect || null",
    // Only mark the input as invalid for assistive technology if it has a value since the
    // state usually overlaps with `aria-required` when the input is empty and can be redundant.
    "[attr.aria-invalid]": "(empty && required) ? null : errorState",
    "[attr.aria-required]": "required",
    // Native input properties that are overwritten by Angular inputs need to be synced with
    // the native input element. Otherwise property bindings for those don't work.
    "[attr.id]": "id",
    "(focus)": "_focusChanged(true)",
    "(blur)": "_focusChanged(false)",
    "(input)": "_onInput()",
  },
  providers: [{ provide: MatFormFieldControl, useExisting: MatFileInput }],
  standalone: true,
})
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class MatFileInput extends MatInput {
  /** Make sure the input is a supported type. */
  protected _validateType() {
    if (this._type !== "file") {
      throw getMatInputUnsupportedTypeError(this._type);
    }
  }
}
