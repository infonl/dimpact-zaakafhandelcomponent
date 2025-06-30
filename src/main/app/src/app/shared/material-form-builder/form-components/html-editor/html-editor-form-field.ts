/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { ActionIcon } from "../../../edit/action-icon";
import { GeneratedType } from "../../../utils/generated-types";
import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class HtmlEditorFormField<
  T extends string = string,
> extends AbstractFormControlField<T> {
  fieldType: FieldType = FieldType.HTML_EDITOR;
  icons: ActionIcon[] = [];
  mailtemplateBody$: Observable<GeneratedType<"RESTMailtemplate">>;
  mailtemplateOnderwerp$: Observable<GeneratedType<"RESTMailtemplate">>;
  variabelen: string[] = [];
  emptyToolbar: boolean;
  maxlength: number;

  constructor() {
    super();
  }
}
