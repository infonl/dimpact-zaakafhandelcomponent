/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { ActionIcon } from "../../../edit/action-icon";
import { GeneratedType } from "../../../utils/generated-types";
import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { HtmlEditorFormField } from "./html-editor-form-field";

export class HtmlEditorFormFieldBuilder<
  T extends string = string,
> extends AbstractFormFieldBuilder<T> {
  readonly formField: HtmlEditorFormField<T>;

  constructor(value?: T | null) {
    super();
    this.formField = new HtmlEditorFormField();
    this.formField.initControl(value ? value : ("" as T));
  }

  mailtemplateBody(
    mailtemplate$: Observable<GeneratedType<"RESTMailtemplate">>,
  ): this {
    this.formField.mailtemplateBody$ = mailtemplate$;
    return this;
  }

  mailtemplateOnderwerp(
    mailtemplate$: Observable<GeneratedType<"RESTMailtemplate">>,
  ): this {
    this.formField.mailtemplateOnderwerp$ = mailtemplate$;
    return this;
  }

  variabelen(variabelen: string[]): this {
    this.formField.variabelen = variabelen;
    return this;
  }

  emptyToolbar(): this {
    this.formField.emptyToolbar = true;
    return this;
  }

  icon(icon: ActionIcon): this {
    this.formField.icons = [icon];
    return this;
  }

  icons(icons: ActionIcon[]): this {
    this.formField.icons = icons;
    return this;
  }

  maxlength(maxlength: number): this {
    this.formField.maxlength = maxlength;
    return this;
  }
}
