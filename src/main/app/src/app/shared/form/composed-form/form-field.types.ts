/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractControl } from "@angular/forms";
import { Observable } from "rxjs";
import { GeneratedType } from "../../utils/generated-types";

export type Form = Record<string, AbstractControl<unknown, unknown>>;

type SingleInputFormField<F extends Form, Key extends keyof F = keyof F> = {
  key: Key;
  label?: string;
  readonly?: boolean;
  hidden?: boolean;
  control?: AbstractControl<unknown, unknown>;
};

type MultipleInputFormField<
  F extends Form,
  Key extends keyof F = keyof F,
  Option extends string | Record<string, unknown> = Record<string, unknown>,
> = SingleInputFormField<F, Key> & {
  options: Option[] | Observable<Option[]>;
  optionDisplayValue?: keyof Option | ((option: Option) => string);
};

type SelectFormField<
  F extends Form,
  Key extends keyof F = keyof F,
  Option extends string | Record<string, unknown> = Record<string, unknown>,
> = MultipleInputFormField<F, Key, Option> & {
  type: "select";
};

type InputFormField<F extends Form> = SingleInputFormField<F> & {
  type: "input";
};

type TextareaFormField<F extends Form> = SingleInputFormField<F> & {
  type: "textarea";
};

type DateFormField<F extends Form> = SingleInputFormField<F> & {
  type: "date";
};

type HtmlEditorField<F extends Form> = SingleInputFormField<F> & {
  type: "html-editor";
  variables?: string[];
};

type CheckboxField<F extends Form> = SingleInputFormField<F> & {
  type: "checkbox";
};

type AutocompleteFormField<
  F extends Form,
  Key extends keyof F = keyof F,
  Option extends string | Record<string, unknown> = Record<string, unknown>,
> = MultipleInputFormField<F, Key, Option> & {
  type: "auto-complete";
};

type DocumentFormField<
  F extends Form,
  Key extends keyof F = keyof F,
  Option extends
    GeneratedType<"RestEnkelvoudigInformatieobject"> = GeneratedType<"RestEnkelvoudigInformatieobject">,
> = MultipleInputFormField<F, Key, Option> & {
  type: "documents";
  viewDocumentInNewTab?: boolean;
};

type PlainTextField<F extends Form> = SingleInputFormField<F> & {
  type: "plain-text";
  icon?: string;
};

type RadioFormField<
  F extends Form,
  Key extends keyof F = keyof F,
  Option extends string | Record<string, unknown> =
    | string
    | Record<string, unknown>,
> = MultipleInputFormField<F, Key, Option> & {
  type: "radio";
};

export type FormField<F extends Form = Form> =
  | SelectFormField<F>
  | InputFormField<F>
  | DateFormField<F>
  | HtmlEditorField<F>
  | CheckboxField<F>
  | TextareaFormField<F>
  | AutocompleteFormField<F>
  | DocumentFormField<F>
  | PlainTextField<F>
  | RadioFormField<F>;

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
