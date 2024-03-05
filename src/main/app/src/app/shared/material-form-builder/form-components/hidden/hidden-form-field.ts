import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class HiddenFormField extends AbstractFormControlField {
  fieldType: FieldType = FieldType.HIDDEN;

  constructor() {
    super();
  }

  initControl(value?: any) {
    super.initControl(value);
    this.label = "hidden";
  }
}
