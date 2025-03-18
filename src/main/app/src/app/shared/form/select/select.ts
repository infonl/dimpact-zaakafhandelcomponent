/*
 * SPDX-FileCopyrightText: <YYYY> Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {Component, Input, OnInit} from "@angular/core";
import {AbstractControl, FormGroup, Validators} from "@angular/forms";
import {isObservable, Observable} from "rxjs";
import { getErrorMessage } from "../helpers";

@Component({
    selector: "zac-select",
    templateUrl: "./select.html",
})
export class ZacSelect<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
    Option extends Form[Key]["value"],
    OptionLabel extends keyof Option | ((option: Option) => string),
    Compare extends (a: Option, b: Option) => boolean
> implements OnInit
{
    @Input({ required: true }) key!: Key;
    @Input({required: true}) label!: string
    @Input({ required: true }) form!: FormGroup<Form>;
    @Input({ required: true }) options!:
        | Observable<Array<Option>>
        | Array<Option>;
    @Input({ required: true }) optionLabel!: OptionLabel;
    @Input() compare?: Compare;
    /**
     * The suffix to display after the input field.
     * It will get translated using the `translate` pipe.
     */
    @Input() suffix?: string;

    protected control?: AbstractControl;

    protected selectableOptions: Option[] = [];

    ngOnInit() {
        this.control = this.form.get(String(this.key))!;

        if (isObservable(this.options)) {
            this.options.subscribe((options) => {
                this.selectableOptions = options;
            });
        } else {
            this.selectableOptions = this.options;
        }
    }

    // Needs to be an arrow function in order to de-link the reference to `this`
    // when used in the template `[displayWith]="displayWith"`
    getOptionLabel = (option?: Option) => {
        if (!option) {
            return null;
        }

        if (typeof this.optionLabel === "function") {
            return this.optionLabel(option);
        }

        return String(option[this.optionLabel as keyof Option]);
    };

    protected isRequired() {
        return this.control?.hasValidator(Validators.required) ?? false;
    }

    // Needs to be an arrow function in order to de-link the reference to `this`
    // when used in the template `[compareWith]="compareWith"`
    protected compareWith = (a: Option, b: Option) => {
        return this.compare?.(a, b) ?? a === b;
    }

    protected getErrorMessage = () => getErrorMessage(this.control);
}
