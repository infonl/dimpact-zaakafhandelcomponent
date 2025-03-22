/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Platform } from "@angular/cdk/platform";
import { AutofillMonitor } from "@angular/cdk/text-field";
import {
  Component,
  ElementRef,
  Input,
  NgZone,
  OnInit,
  Optional,
} from "@angular/core";
import {
  AbstractControl,
  FormGroup,
  FormGroupDirective,
  NgControl,
  NgForm,
} from "@angular/forms";
import { ErrorStateMatcher } from "@angular/material/core";
import { MatFormField } from "@angular/material/form-field";
import { MatInput } from "@angular/material/input";
import { TranslateService } from "@ngx-translate/core";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-input",
  templateUrl: "./input.html",
})
export class ZacInput<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
  >
  extends MatInput
  implements OnInit
{
  @Input({ required: true }) key!: Key;
  @Input({ required: true }) form!: FormGroup<Form>;

  protected control?: AbstractControl<string>;
  protected maxlength?: number | null;

  constructor(
    _elementRef: ElementRef,
    _platform: Platform,
    @Optional() ngControl: NgControl,
    @Optional() _parentForm: NgForm,
    @Optional() _parentFormGroup: FormGroupDirective,
    _defaultErrorStateMatcher: ErrorStateMatcher,
    _autoFillMonitor: AutofillMonitor,
    _ngZone: NgZone,
    @Optional() _formField: MatFormField,
    private readonly translateService: TranslateService,
  ) {
    super(
      _elementRef,
      _platform,
      ngControl,
      _parentForm,
      _parentFormGroup,
      _defaultErrorStateMatcher,
      null,
      _autoFillMonitor,
      _ngZone,
      _formField,
    );
  }

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
    this.maxlength = FormHelper.getValidatorValue("maxLength", this.control);
  }

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);
}
