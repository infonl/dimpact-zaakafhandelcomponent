/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  booleanAttribute,
  Component,
  effect,
  input,
  output,
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
  hidden?: boolean;
  control?: AbstractControl<unknown, unknown>;
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

type HtmlEditorField<Form extends _Form> = SingleInputFormField<Form> & {
  type: "html-editor";
};

type CheckboxField<Form extends _Form> = SingleInputFormField<Form> & {
  type: "checkbox";
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

type PlainTextField<Form extends _Form> = SingleInputFormField<Form> & {
  type: "plain-text";
};

type RadioFormField<
  Form extends _Form,
  Key extends keyof Form = keyof Form,
  Option extends string | Record<string, unknown> =
    | string
    | Record<string, unknown>,
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
  cancelLabel?: null;
};

type CancelableFormConfig = _FormConfig & {
  partialSubmitLabel?: null;
  cancelLabel?: string;
  hideCancelButton?: false;
};

export type FormConfig = FormConfigWithPartialSubmit | CancelableFormConfig;

export type FormField<Form extends _Form = _Form> =
  | SelectFormField<Form>
  | InputFormField<Form>
  | DateFormField<Form>
  | HtmlEditorField<Form>
  | CheckboxField<Form>
  | TextareaFormField<Form>
  | AutocompleteFormField<Form>
  | DocumentFormField<Form>
  | PlainTextField<Form>
  | RadioFormField<Form>;

@Component({
  selector: "zac-form",
  templateUrl: "./form.html",
})
export class ZacForm<Form extends _Form> {
  protected readonly form = input.required<FormGroup<Form>>();
  protected readonly fields = input.required<FormField[]>();
  protected readonly config = input<FormConfig>({ hideCancelButton: false });
  protected readonly readonly = input(false, { transform: booleanAttribute });

  protected readonly formSubmitted = output<FormGroup<Form>>();
  protected readonly formPartiallySubmitted = output<FormGroup<Form>>();
  protected readonly formCancelled = output<void>();

  constructor() {
    effect(() => {
      if (!this.readonly()) return;
      this.form().disable({ onlySelf: true });
    });
  }

  protected submitForm() {
    this.formSubmitted.emit(this.form());
  }

  protected partiallySubmitForm() {
    this.formPartiallySubmitted.emit(this.form());
  }

  protected cancelForm() {
    this.form().reset();
    this.formCancelled.emit();
  }
}
