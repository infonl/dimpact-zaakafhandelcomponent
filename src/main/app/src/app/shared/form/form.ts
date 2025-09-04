import {Component, EventEmitter, Input, Output} from "@angular/core";
import {AbstractControl, FormGroup} from "@angular/forms";
import {FieldType} from "../material-form-builder/model/field-type.enum";

type _Form = Record<string, AbstractControl<unknown, unknown>>

type BaseFormField<Form extends _Form, Key extends keyof Form = keyof Form> = {
    key: Key,
    label?: string
}

type SelectFormField<Form extends _Form, Option extends string | Record<string, unknown> = Record<string, unknown>> = BaseFormField<Form> & {
    type: 'select',
    options: Option[],
    optionDisplayValue?: keyof Option | ((option: Option) => string),
}

type InputFormField<Form extends _Form> = BaseFormField<Form> & {
    type: 'input',
}

type TextareaFormField<Form extends _Form> = BaseFormField<Form> & {
    type: 'textarea',
}

type DateFormField<Form extends _Form> = BaseFormField<Form> & {
    type: 'date',
}

type AutocompleteFormField<Form extends _Form, Option extends string | Record<string, unknown> = Record<string, unknown>> = BaseFormField<Form> & {
    type: 'auto-complete',
    options: Option[],
    optionDisplayValue?: keyof Option | ((option: Option) => string),
}

export type FormConfig = {
    submitLabel?: string,
    cancelLabel?: string,
}

export type FormField<Form extends _Form = _Form> =
    SelectFormField<Form> |
    InputFormField<Form> |
    DateFormField<Form> |
    TextareaFormField<Form> |
    AutocompleteFormField<Form>

@Component({
    selector: "zac-form",
    templateUrl: "./form.html",
    // styleUrls: ["./form.less"],
})
export class ZacForm<Form extends _Form> {
    @Input({ required: true }) form!: FormGroup<Form>;
    @Input({ required: true }) fields: FormField[] = [];
    @Input() config: FormConfig = {};
    @Output() formSubmitted = new EventEmitter<FormGroup<Form>>();
    @Output() formCancelled = new EventEmitter<void>()

    protected submitForm() {
        this.formSubmitted.emit(this.form);
    }

    protected cancelForm() {
        this.form.reset()
        this.formCancelled.emit();
    }
}
