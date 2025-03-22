/*
 * SPDX-FileCopyrightText: <YYYY> Lifely
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
  selector: "zac-textarea",
  templateUrl: "./textarea.html",
})
export class ZacTextarea<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
  >
  extends MatInput
  implements OnInit
{
  @Input({ required: true }) key!: Key;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input() minRows?: number = 5;
  @Input() maxRows?: number = 15;

  protected control?: AbstractControl<string>;
  protected maxlength?: number | null;

  constructor(
    protected _elementRef: ElementRef,
    protected _platform: Platform,
    @Optional() public ngControl: NgControl,
    @Optional() protected _parentForm: NgForm,
    @Optional() protected _parentFormGroup: FormGroupDirective,
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
