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

type BaseFormField<Form extends _Form, Key extends keyof Form = keyof Form> = {
  key: Key;
  label?: string;
};

type SelectFormField<
  Form extends _Form,
  Option extends string | Record<string, unknown> = Record<string, unknown>,
> = BaseFormField<Form> & {
  type: "select";
  options: Option[];
  optionDisplayValue?: keyof Option | ((option: Option) => string);
};

type InputFormField<Form extends _Form> = BaseFormField<Form> & {
  type: "input";
};

type TextareaFormField<Form extends _Form> = BaseFormField<Form> & {
  type: "textarea";
};

type DateFormField<Form extends _Form> = BaseFormField<Form> & {
  type: "date";
};

type AutocompleteFormField<
  Form extends _Form,
  Option extends string | Record<string, unknown> = Record<string, unknown>,
> = BaseFormField<Form> & {
  type: "auto-complete";
  options: Option[];
  optionDisplayValue?: keyof Option | ((option: Option) => string);
};

type DocumentFormField<
  Form extends _Form,
  Option extends
    GeneratedType<"RestEnkelvoudigInformatieobject"> = GeneratedType<"RestEnkelvoudigInformatieobject">,
> = BaseFormField<Form> & {
  type: "documents";
  options: Option[] | Observable<Option[]>;
  readonly?: boolean;
};

type PlainTextField<Form extends _Form> = Omit<BaseFormField<Form>, "key"> & {
  type: "plain-text";
  text: string;
  header?: string;
};

type RadioFormField<
  Form extends _Form,
  Option extends string | Record<string, unknown> = string,
> = BaseFormField<Form> & {
  type: "radio";
  options: Option[];
};

type BaseFormConfig = {
  submitLabel?: string;
};
type FormConfigWithPartialSubmit = BaseFormConfig & {
  partialSubmitLabel: string;
  hideCancelButton: true;
};

type CancelableFormConfig = BaseFormConfig & {
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
