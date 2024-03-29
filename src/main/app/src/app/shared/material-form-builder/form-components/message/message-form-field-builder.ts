/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { TextIcon } from "../../../edit/text-icon";
import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { MessageFormField } from "./message-form-field";
import { MessageLevel } from "./message-level.enum";

export class MessageFormFieldBuilder extends AbstractFormFieldBuilder {
  public readonly formField: MessageFormField;

  private messageLevel: MessageLevel = MessageLevel.INFO;

  constructor(value = true) {
    super();
    this.formField = new MessageFormField();
    this.formField.initControl(value);
    this.level(MessageLevel.INFO);
  }

  level(level: MessageLevel): this {
    this.messageLevel = level;
    return this;
  }

  text(text: string): this {
    this.formField.label = text;
    return this;
  }

  build() {
    this.validate();
    switch (this.messageLevel) {
      case MessageLevel.INFO:
        this.formField.icon = this.icon("info");
        break;
      case MessageLevel.WARNING:
        this.formField.icon = this.icon("warning");
        break;
      case MessageLevel.ERROR:
        this.formField.icon = this.icon("error");
        break;
    }
    return this.formField;
  }

  private icon(level: string) {
    return new TextIcon(
      DateConditionals.provideFormControlValue(DateConditionals.always),
      level,
      this.formField.id + "_icon",
      "msg.message." + level,
      level,
    );
  }
}
