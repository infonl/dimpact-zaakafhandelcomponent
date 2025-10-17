/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, Type } from "@angular/core";
import { AutocompleteComponent } from "./form-components/autocomplete/autocomplete.component";
import { CheckboxComponent } from "./form-components/checkbox/checkbox.component";
import { DateComponent } from "./form-components/date/date.component";
import { DividerComponent } from "./form-components/divider/divider.component";
import { DocumentenLijstComponent } from "./form-components/documenten-lijst/documenten-lijst.component";
import { DocumentenOndertekenenComponent } from "./form-components/documenten-ondertekenen/documenten-ondertekenen.component";
import { FileInputComponent } from "./form-components/file-input/file-input.component";
import { FileComponent } from "./form-components/file/file.component";
import { HeadingComponent } from "./form-components/heading/heading.component";
import { HiddenComponent } from "./form-components/hidden/hidden.component";
import { HtmlEditorComponent } from "./form-components/html-editor/html-editor.component";
import { InputComponent } from "./form-components/input/input.component";
import { MedewerkerGroepComponent } from "./form-components/medewerker-groep/medewerker-groep.component";
import { MessageComponent } from "./form-components/message/message.component";
import { ParagraphComponent } from "./form-components/paragraph/paragraph.component";
import { RadioComponent } from "./form-components/radio/radio.component";
import { ReadonlyComponent } from "./form-components/readonly/readonly.component";
import { SelectComponent } from "./form-components/select/select.component";
import { TaakDocumentUploadComponent } from "./form-components/taak-document-upload/taak-document-upload.component";
import { TextareaComponent } from "./form-components/textarea/textarea.component";
import { AbstractFormField } from "./model/abstract-form-field";
import { FieldType } from "./model/field-type.enum";
import { FormComponent } from "./model/form-component";
import { FormItem } from "./model/form-item";

@Injectable({
  providedIn: "root",
})
export class MaterialFormBuilderService {
  constructor() {}

  public getFormItem(formField: AbstractFormField): FormItem {
    return new FormItem(
      MaterialFormBuilderService.getType(formField.fieldType),
      formField,
    );
  }

  private static getType(type: FieldType): Type<FormComponent> {
    switch (type) {
      case FieldType.PARAGRAPH:
        return ParagraphComponent;
      case FieldType.RADIO:
        return RadioComponent;
      case FieldType.AUTOCOMPLETE:
        return AutocompleteComponent;
      case FieldType.READONLY:
        return ReadonlyComponent;
      case FieldType.DIVIDER:
        return DividerComponent;
      case FieldType.DATE:
        return DateComponent;
      case FieldType.INPUT:
        return InputComponent;
      case FieldType.FILE:
        return FileComponent;
      case FieldType.FILE_INPUT:
        return FileInputComponent;
      case FieldType.TEXTAREA:
        return TextareaComponent;
      case FieldType.HEADING:
        return HeadingComponent;
      case FieldType.HIDDEN:
        return HiddenComponent;
      case FieldType.HTML_EDITOR:
        return HtmlEditorComponent;
      case FieldType.SELECT:
        return SelectComponent;
      case FieldType.MEDEWERKER_GROEP:
        return MedewerkerGroepComponent;
      case FieldType.CHECKBOX:
        return CheckboxComponent;
      case FieldType.TAAK_DOCUMENT_UPLOAD:
        return TaakDocumentUploadComponent;
      case FieldType.DOCUMENTEN_LIJST:
        return DocumentenLijstComponent;
      case FieldType.DOCUMENTEN_ONDERTEKENEN:
        return DocumentenOndertekenenComponent;
      case FieldType.MESSAGE:
        return MessageComponent;
      default:
        throw new Error(`Unknown type: '${type}'`);
    }
  }
}
