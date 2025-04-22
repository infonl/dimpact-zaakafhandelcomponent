# BPMN guide

## ZAC and BPMN
ZAC uses [Flowable](https://www.flowable.com/) to support BPMN processes. Forms that provide input for the BPMN processes are implemented using the [Forms.io](https://forms.io/) framework.  

## BPMN process
To create a BPMN process definition, you can:
* use Flowable [web editor](https://flowable.com/design)
* upload our integration tests [process](../../../src/itest/resources/bpmn/itProcessDefinition.bpmn)

:warning: The "User tasks" should have the candidate group or user set. For example: `flowable:candidateGroups="group"`

## Form.io form
To create a Form.io form, please use the Form.io [Builder](https://formio.github.io/formio.js/app/builder).

### Validation
Form.io offers validation of the data entered in the form. 

For example, the emails can be validated by specifying:
```json
{
  "label": "E-mail sender",
  "type": "email",
  "key": "email",
  "input": true,
  "applyMaskOn": "change",
  "validate": {
    "required": true
  }
}
```
:warning:: Notice the `validate` and `type` keys.
