/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { booleanAttribute, Component, Input, OnInit } from "@angular/core";
import { AbstractControl, FormGroup, Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Editor, Toolbar } from "ngx-editor";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-html-editor",
  templateUrl: "./html-editor.html",
  styleUrls: ['./html-editor.less'],
})
export class ZacHtmlEditor<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
> implements OnInit
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ transform: booleanAttribute }) readonly = false;
  @Input({ transform: booleanAttribute }) noToolbar = false;
  @Input() toolbar: Toolbar = [
    ["bold", "italic", "underline"],
    ["blockquote"],
    ["ordered_list", "bullet_list"],
    [{ heading: ["h1", "h2", "h3", "h4", "h5", "h6"] }],
    ["link", "image"],
    ["text_color", "background_color"],
    ["align_left", "align_center", "align_right", "align_justify"],
    [],
  ];
  @Input() variables: string[] = [];
  @Input() label?: string;

  protected readonly editor = new Editor();

  protected maxlength: number | null = null;
  protected control?: AbstractControl<string>;

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
    this.maxlength = FormHelper.getValidatorValue("maxLength", this.control);
    if (this.noToolbar) this.toolbar = [];
  }

  protected get required() {
    return this.control?.hasValidator(Validators.required) ?? false;
  }

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);
}
