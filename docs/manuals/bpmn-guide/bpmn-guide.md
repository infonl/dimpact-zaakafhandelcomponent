# BPMN guide

## ZAC and BPMN
ZAC uses [Flowable](https://www.flowable.com/) to support BPMN processes. Forms that provide input for the BPMN processes are implemented using the [Forms.io](https://forms.io/) framework.  

## BPMN process
To create a BPMN process definition, you can:
* use Flowable [web editor](https://trial.flowable.com/design)
* upload our integration tests [process](../../../src/itest/resources/bpmn/itProcessDefinition.bpmn)

## Process definition

### Download from Flowable
If you are ready to try the new process definition, export/save it as BPMN file. 

:warning: The "User tasks" should have the candidate group or user set. For example, the XML attribute in the exported BPMN file might look like this: `flowable:candidateGroups="group"`

### Upload
1. Open ZAC
2. Go to the "Beheer-instellingen"
3. Open "BPMN Process definities"
4. Click on the plus sign to open File selection dialog
5. Select the BPM proceess file

![image](./images/1036ca6b-d39e-429e-9356-80005807fc9c.png)

## Form.io form
To create a Form.io form, please use the Form.io [Builder](https://formio.github.io/formio.js/app/builder).

### Upload
1. Open ZAC
2. Go to the "Beheer-instellingen"
3. Open "Form.io formulieren"
4. Click on the plus sign to open File selection dialog
5. Select the Form.io form

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
:warning: Notice the `validate` and `type` keys.
