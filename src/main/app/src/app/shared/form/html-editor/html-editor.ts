/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  booleanAttribute,
  Component,
  computed,
  input,
  OnDestroy,
  SecurityContext,
} from "@angular/core";
import { AbstractControl } from "@angular/forms";
import { DomSanitizer } from "@angular/platform-browser";
import { Editor, Toolbar } from "ngx-editor";
import { Schema } from "prosemirror-model";
import { SingleInputFormField } from "../BaseFormField";
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
    Option extends Form[Key]["value"] = string | null,
  >
  extends SingleInputFormField<Form, Key, Option>
  implements OnDestroy
{
  protected isPlainText = input(false, { transform: booleanAttribute });
  protected toolbar = input<Toolbar>([
    ["bold", "italic", "underline"],
    ["blockquote"],
    ["ordered_list", "bullet_list"],
    [{ heading: ["h1", "h2", "h3", "h4", "h5", "h6"] }],
    ["link", "image"],
    ["text_color", "background_color"],
    ["align_left", "align_center", "align_right", "align_justify"],
    [],
  ]);

  protected computedToolbar = computed(() =>
    this.isPlainText() ? [] : this.toolbar(),
  );
  protected variables = input<string[]>([]);

  protected editor = computed(() => {
    if (!this.isPlainText()) return new Editor();

    const sanitized = this.domSanitizer.sanitize(
      SecurityContext.HTML,
      this.control()?.value ?? null,
    ) as Option | null;

    this.control()?.setValue(sanitized);

    return new Editor({
      keyboardShortcuts: false,
      schema: plainTextSchema,
    });
  });

  protected maxlength = computed(() =>
    FormHelper.getValidatorValue("maxLength", this.control() ?? null),
  );

  constructor(private readonly domSanitizer: DomSanitizer) {
    super();
  }

  ngOnDestroy() {
    this.editor().destroy();
  }
}
