import { AbstractFormControlField } from "./abstract-form-control-field";
import { FieldType } from "./field-type.enum";

class TestFormControlField<T = unknown> extends AbstractFormControlField<T> {
  fieldType = FieldType.INPUT;
  constructor() {
    super();
  }
}
describe(AbstractFormControlField.name, () => {
  test.each([
    [null, null],
    [undefined, null],
    ["null", null],
    ["undefined", null],
    ["some value", "some value"],
  ])("should transform value '%s' to '%s'", (input, expected) => {
    const field = new TestFormControlField<string>();
    field.initControl(input);
    expect(field.formControl.value).toBe(expected);
  });
});
