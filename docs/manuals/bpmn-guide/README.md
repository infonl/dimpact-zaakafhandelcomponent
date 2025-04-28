# BPMN guide (BETA)

:fire: The functionality described below is still "Beta". Beta software may contain errors or inaccuracies and may not function as well as regular releases. :fire:

## ZAC and BPMN
ZAC uses [Flowable](https://www.flowable.com/) to support BPMN processes. Forms that provide input for the BPMN processes are implemented using the [Forms.io](https://forms.io/) framework.

### Feature flag
By default, BPMN support in ZAC is disabled in K8s and enabled in local Docker Compose environment. This is controlled by the `FEATURE_FLAG_BPMN_SUPPORT` environment variable, that accepts `true` or `false` values.

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
4. Click on the plus sign to open a file selection dialog
5. Select the BPM process file

![image](./images/1036ca6b-d39e-429e-9356-80005807fc9c.png)

## Form.io form
To create a Form.io form:
* use the Form.io [Builder](https://formio.github.io/formio.js/app/builder)
* upload our integration tests [form](../../../src/itest/resources/bpmn/testForm.json)

### Upload
1. Open ZAC
2. Go to the "Beheer-instellingen"
3. Open "Form.io formulieren"
4. Click on the plus sign to open a file selection dialog
5. Select the Form.io form

### Validation
Form.io offers validation of the data entered in the form. 

For example, the emails can be validated by specifying `validate` and `type` keys:
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
