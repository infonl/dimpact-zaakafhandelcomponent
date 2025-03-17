/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { Mailtemplate } from "../../../../admin/model/mailtemplate";
import { MailtemplateVariabele } from "../../../../admin/model/mailtemplate-variabele";
import { ActionIcon } from "../../../edit/action-icon";
import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class HtmlEditorFormField extends AbstractFormControlField {
  fieldType: FieldType = FieldType.HTML_EDITOR;
  icons: ActionIcon[];
  mailtemplateBody$: Observable<Mailtemplate>;
  mailtemplateOnderwerp$: Observable<Mailtemplate>;
  variabelen: MailtemplateVariabele[];
  emptyToolbar: boolean;
  maxlength: number;

  constructor() {
    super();
  }
}
