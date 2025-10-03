/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  booleanAttribute,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { Observable } from "rxjs";
import { GeneratedType } from "../utils/generated-types";

type _Form = Record<string, AbstractControl<unknown, unknown>>;

type SingleInputFormField<
  Form extends _Form,
  Key extends keyof Form = keyof Form,
> = {
  key: Key;
  label?: string;
  readonly?: boolean;
};

type MultipleInputFormField<
  Form extends _Form,
  Key extends keyof Form = keyof Form,
  Option extends string | Record<string, unknown> = Record<string, unknown>,
> = SingleInputFormField<Form, Key> & {
  options: Option[] | Observable<Option[]>;
  optionDisplayValue?: keyof Option | ((option: Option) => string);
};

type SelectFormField<
  Form extends _Form,
  Key extends keyof Form = keyof Form,
  Option extends string | Record<string, unknown> = Record<string, unknown>,
> = MultipleInputFormField<Form, Key, Option> & {
  type: "select";
};

type InputFormField<Form extends _Form> = SingleInputFormField<Form> & {
  type: "input";
};

type TextareaFormField<Form extends _Form> = SingleInputFormField<Form> & {
  type: "textarea";
};

type DateFormField<Form extends _Form> = SingleInputFormField<Form> & {
  type: "date";
};

type AutocompleteFormField<
  Form extends _Form,
  Key extends keyof Form = keyof Form,
  Option extends string | Record<string, unknown> = Record<string, unknown>,
> = MultipleInputFormField<Form, Key, Option> & {
  type: "auto-complete";
};

type DocumentFormField<
  Form extends _Form,
  Key extends keyof Form = keyof Form,
  Option extends
    GeneratedType<"RestEnkelvoudigInformatieobject"> = GeneratedType<"RestEnkelvoudigInformatieobject">,
> = MultipleInputFormField<Form, Key, Option> & {
  type: "documents";
};

type PlainTextField<Form extends _Form> = Omit<
  SingleInputFormField<Form>,
  "key"
> & {
  type: "plain-text";
  text: string;
  header?: string;
};

type RadioFormField<
  Form extends _Form,
  Key extends keyof Form = keyof Form,
  Option extends string | Record<string, unknown> = string,
> = MultipleInputFormField<Form, Key, Option> & {
  type: "radio";
};

/**
 * This type is meant to be used **only** in the `ZacForm` component.
 */
type _FormConfig = {
  submitLabel?: string;
};
type FormConfigWithPartialSubmit = _FormConfig & {
  partialSubmitLabel: string;
  hideCancelButton: true;
};

type CancelableFormConfig = _FormConfig & {
  cancelLabel?: string;
  hideCancelButton?: false;
};

export type FormConfig = FormConfigWithPartialSubmit | CancelableFormConfig;

export type FormField<Form extends _Form = _Form> =
  | SelectFormField<Form>
  | InputFormField<Form>
  | DateFormField<Form>
  | TextareaFormField<Form>
  | AutocompleteFormField<Form>
  | DocumentFormField<Form>
  | PlainTextField<Form>
  | RadioFormField<Form>;

@Component({
  selector: "zac-form",
  templateUrl: "./form.html",
})
export class ZacForm<Form extends _Form> implements OnChanges {
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ required: true }) fields: FormField[] = [];
  @Input() config: FormConfig = {
    hideCancelButton: false,
  };
  @Input({ transform: booleanAttribute }) readonly = false;

  @Output() formSubmitted = new EventEmitter<FormGroup<Form>>();
  @Output() formPartiallySubmitted = new EventEmitter<FormGroup<Form>>();
  @Output() formCancelled = new EventEmitter<void>();

  ngOnChanges(changes: SimpleChanges) {
    if ("readonly" in changes) {
      if (changes.readonly.currentValue) {
        this.form.disable();
      } else {
        this.form.enable();
      }
    }
  }

  protected submitForm() {
    this.formSubmitted.emit(this.form);
  }

  protected partiallySubmitForm() {
    this.formPartiallySubmitted.emit(this.form);
  }

  protected cancelForm() {
    this.form.reset();
    this.formCancelled.emit();
  }
}
