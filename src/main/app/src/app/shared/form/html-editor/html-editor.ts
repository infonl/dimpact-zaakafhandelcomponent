/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  booleanAttribute,
  Component,
  Input,
  OnDestroy,
  OnInit,
} from "@angular/core";
import { AbstractControl, FormGroup, Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Editor, Toolbar } from "ngx-editor";
import { Schema } from "prosemirror-model";
import { FormHelper } from "../helpers";

const plainTextSchema = new Schema({
  nodes: {
    doc: { content: "text*" },
    paragraph: {
      content: "text*",
      group: "block",
      parseDOM: [{ tag: "p" }],
      toDOM: () => ["p", 0],
    },
    text: {},
  },
  marks: {},
});

@Component({
  selector: "zac-html-editor",
  templateUrl: "./html-editor.html",
  styleUrls: ["./html-editor.less"],
})
export class ZacHtmlEditor<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
  >
  implements OnInit, OnDestroy
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ transform: booleanAttribute }) readonly = false;
  @Input({ transform: booleanAttribute }) isPlainText = false;
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

  protected editor = new Editor();

  protected maxlength: number | null = null;
  protected control?: AbstractControl<string | null>;

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
    this.maxlength = FormHelper.getValidatorValue("maxLength", this.control);

    if (this.isPlainText) this.setupPlainTextEditor();
  }

  private setupPlainTextEditor() {
    this.toolbar = [];
    this.editor.destroy();

    const plainText = this.control?.value?.replace(/<[^>]*>/g, "") ?? null;
    this.control?.setValue(plainText);

    this.editor = new Editor({
      keyboardShortcuts: false,
      schema: plainTextSchema,
    });
  }

  protected get required() {
    return this.control?.hasValidator(Validators.required) ?? false;
  }

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);

  ngOnDestroy() {
    this.editor.destroy();
  }
}
