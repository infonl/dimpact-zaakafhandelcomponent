# BPMN guide (BETA)

:fire: The functionality described below is still "Beta". Beta software may contain errors or inaccuracies and may not function as well as regular releases. :fire:

## ZAC and BPMN
ZAC uses [Flowable](https://www.flowable.com/) to support BPMN processes. Forms that provide input for the BPMN processes are implemented using the [Forms.io](https://forms.io/) framework.

### Feature flag
By default, BPMN support in ZAC is disabled in K8s and enabled in local Docker Compose environment. This is controlled by the `FEATURE_FLAG_BPMN_SUPPORT` environment variable, that accepts `true` or `false` values.

## BPMN process definition
To create a BPMN process definition, you can:
* use Flowable [web editor](https://trial.flowable.com/design)
* start with our integration tests [process](../../../src/itest/resources/bpmn/itProcessDefinition.bpmn)

### Requirements

#### Candidate group/user
The "User tasks" should have the candidate group or user set. For example, the XML attributes in the `.bpmn` file might look like this: `flowable:candidateUsers="${var:get(zaakBehandelaar)}" flowable:candidateGroups="${zaakGroep}"`

We're using `var:get` [function](https://documentation.flowable.com/latest/develop/be/be-expressions#variable-functions) here which tries to get a `zaakBehandelaar` variable value, but it won’t throw an exception when the variable doesn’t exist.
As the group should always be provided when creating a zaak we set the candidate group directly to the value of `zaakGroep` variable.

For example:
```xml
    <userTask id="userTask" name="User details" flowable:candidateUsers="${var:get(zaakBehandelaar)}" flowable:candidateGroups="${zaakGroep}" flowable:formKey="testForm" flowable:formFieldValidation="false">
      ... the rest of userTask tags ...
    </userTask>
```

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

## ZAC extensions

ZAC extension fields are added to the Form.io form as an `ZAC_TYPE` `attribute` to the field component.

Available ZAC types are:
* `ZAC_groep`
* `ZAC_medewerker`
* `ZAC_smart_documents_template`
* `ZAC_referentie_tabel`
* `ZAC_documenten`
* `ZAC_resultaat`
* `ZAC_status`
* `ZAC_process_data`

### Zaak

#### Listing statustypes
The available status types for a zaak can be displayed with:
* A `select` component, with the attribute `ZAC_TYPE` of `ZAC_status`

Example:
```json
{
  "label": "Select status",
  "optionsLabelPosition": "right",
  "key": "ZK_Status",
  "widget": "html5",
  "validate": {
    "required": true,
    "onlyAvailableItems": true
  },
  "attributes": {
    "ZAC_TYPE": "ZAC_status"
  },
  "type": "select",
  "input": true,
  "dataSrc": "custom"
}
```

#### Listing resultaattypes
The available result types for a zaak can be displayed with:
* A `select` component, with the attribute `ZAC_TYPE` of `ZAC_resultaat`

Example:
```json
{
  "label": "Select result",
  "optionsLabelPosition": "right",
  "key": "ZK_Result",
  "widget": "html5",
  "validate": {
    "required": true,
    "onlyAvailableItems": true
  },
  "attributes": {
    "ZAC_TYPE": "ZAC_resultaat"
  },
  "type": "select",
  "input": true,
  "dataSrc": "custom"
}
```

#### Changing status and result
To change zaak status, you have to:
* create a service task
* set class `net.atos.zac.flowable.delegate.UpdateZaakJavaDelegate`
* add fields
   * `statustypeOmschrijving` to `stringvalue` or `expression` representing your desired zaak statustype omschrijving
   * `resultaattypeOmschrijving` to a valid `stringvalue` or `expression`, required by your zaak statustype

For example:
```xml
    <serviceTask id="ServiceTask_357" name="Status to &quot;Verleend&quot;" flowable:class="net.atos.zac.flowable.delegate.UpdateZaakJavaDelegate">
      <extensionElements>
        <flowable:field name="statustypeOmschrijving">
          <flowable:string><![CDATA[Afgerond]]></flowable:string>
        </flowable:field>
        <flowable:field name="resultaattypeOmschrijving">
          <flowable:string><![CDATA[Verleend]]></flowable:string>
        </flowable:field>
        <design:stencilid><![CDATA[ServiceTask]]></design:stencilid>
        <design:stencilsuperid><![CDATA[Task]]></design:stencilsuperid>
      </extensionElements>
    </serviceTask>
```

#### Suspending
To suspend a zaak:
* create a service task
* set class `net.atos.zac.flowable.delegate.SuspendZaakDelegate`
* add fields:
  * `aantalDagen` - number of days to suspend the zaak for. Added to the current date.
  * `opschortingReden` - reason for suspension

For example:
```xml
    <serviceTask id="ServiceTask_360" name="Suspend" flowable:class="net.atos.zac.flowable.delegate.SuspendZaakDelegate">
      <extensionElements>
        <flowable:field name="aantalDagen">
          <flowable:expression><![CDATA[10]]></flowable:expression>
        </flowable:field>
        <flowable:field name="opschortingReden">
          <flowable:expression><![CDATA[suspend test]]></flowable:expression>
        </flowable:field>
        <design:stencilid><![CDATA[ServiceTask]]></design:stencilid>
        <design:stencilsuperid><![CDATA[Task]]></design:stencilsuperid>
      </extensionElements>
    </serviceTask>
```

#### Resuming
To resume a zaak:
* create a service task
* set class `net.atos.zac.flowable.delegate.ResumeZaakDelegate`
* add fields:
  * `hervattenReden` - reason for resuming

For example:
```xml
    <serviceTask id="ServiceTask_361" name="Resume" flowable:class="net.atos.zac.flowable.delegate.ResumeZaakDelegate">
      <extensionElements>
        <flowable:field name="hervattenReden">
          <flowable:expression><![CDATA[resume test]]></flowable:expression>
        </flowable:field>
        <design:stencilid><![CDATA[ServiceTask]]></design:stencilid>
        <design:stencilsuperid><![CDATA[Task]]></design:stencilsuperid>
      </extensionElements>
    </serviceTask>
```


### Send email
To send email:
* create a service task
* set class `net.atos.zac.flowable.delegate.SendEmailDelegate`
* add fields:
  * `to` - equal to the receiver's email address
  * `from` - the sender's email address
  * `replyTo` - the replyTo's email address
  * `template` - the name of the email template you want to use

For example:
```xml
    <serviceTask id="ServiceTask_358" name="Send email" flowable:class="net.atos.zac.flowable.delegate.SendEmailDelegate">
      <extensionElements>
        <flowable:field name="from">
          <flowable:string><![CDATA[team-dimpact@info.nl]]></flowable:string>
        </flowable:field>
        <flowable:field name="to">
          <flowable:string><![CDATA[shared-team-dimpact@info.nl]]></flowable:string>
        </flowable:field>
        <flowable:field name="replyTo">
          <flowable:string><![CDATA[shared-team-dimpact@info.nl]]></flowable:string>
        </flowable:field>
        <flowable:field name="template">
          <flowable:string><![CDATA[Algemene e-mail]]></flowable:string>
        </flowable:field>
        <design:stencilid><![CDATA[ServiceTask]]></design:stencilid>
        <design:stencilsuperid><![CDATA[Task]]></design:stencilsuperid>
      </extensionElements>
    </serviceTask>
```

### User/group

#### Group
* A `select` component, with the attribute `ZAC_TYPE` of `ZAC_groep`

```json
{
  "label": "Group",
  "type": "select",
  "key": "AM_TeamBehandelaar_Groep",
  "input": true,
  "dataSrc": "custom",
  "clearOnRefresh": true,
  "attributes": {
    "ZAC_TYPE": "ZAC_groep"
  }
}
```

#### User
* A `select` component, with the attribute `ZAC_TYPE` of `ZAC_medewerker`
* An optional attribute `refreshOn` to refresh the user list when the group changes. The value of this attribute should be the key of the group component.

```json
{
  "label": "User",
  "type": "select",
  "key": "AM_TeamBehandelaar_Groep",
  "dataSrc": "custom",
  "clearOnRefresh": true,
  "input": true,
  "refreshOn": "AM_TeamBehandelaar_Groep",
  "attributes": {
    "ZAC_TYPE": "ZAC_medewerker"
  }
}
```

#### Setting task group and user
The following BPMN-specific variables can be used in expressions in the BPMN process:
* `zaakGroep` - group assigned to the zaak
* `zaakBehandelaar` (optional) - user assigned to the zaak

The above variables can be used in `assignee` and `candidateGroups` attributes for example:
```xml
<userTask id="summary" name="Summary" flowable:assignee="${var:get(zaakBehandelaar)}" flowable:candidateGroups="${zaakGroep}" flowable:formKey="summaryForm" flowable:formFieldValidation="false">
```

### SmartDocuments

#### Listing available documents
To display linked documents of a zaak you can use:
* a fieldset with type `documentsFieldset`
* `select` type component with:
    * custom data source
    * multi select attribute (`type=select` with `multiple=true`)

Example:
```json
    {
      "legend": "Available Documents",
      "type": "documentsFieldset",
      "key": "ZAAK_Documents",
      "input": false,
      "components": [
        {
          "label": "Documents",
          "type": "select",
          "key": "ZAAK_Documents_Select",
          "input": true,
          "widget": "choicesjs",
          "multiple": true,
          "defaultValue": [],
          "clearOnRefresh": true,
          "dataSrc": "custom",
          "placeholder": "Select one or more documents",
          "customOptions": {
            "choicesOptions": {
              "removeItemButton": true,
              "placeholder": false,
              "searchEnabled": true,
              "shouldSort": false
            }
          },
          "validate": {
            "required": true
          }
        }
      ]
    }
```

#### Creating documents
To create and attach a file generated by SmartDocuments to the current task, you should use a `fieldset` layout component with:
* `"type": "smartDocumentsFieldset"`
* a `select` component with:
   * the same key as the `fieldset` and suffix `_Template`
   * custom data source: `"dataSrc": "custom"`
* a `button` with:
   * SmartDocument properties
   * custom event: `"event": "createDocument"`

The following properties can be used to configure the integration: 
* `SmartDocuments_Group` - path to the SmartDocuments group
* `SmartDocuments_InformatieobjecttypeUuid` - default informatieobjecttype UUID
* `SmartDocuments_<template name>_InformatieobjecttypeUuid` - UUID of the Informatieobjecttype for a specific template

The path to the SmartDocuments group specifies which group of templates to list. For example: `root/nested group/group with more nesting`.  

First, a lookup for the template-specific information object type (informatieobjecttype) UUID is attempted. If a template-specific UUID is not found, the default is used.
The template name should be snake-case (`Data Test` becomes `Data_Test`).

Example:
```json
    {
      "legend": "SmartDocuments",
      "type": "smartDocumentsFieldset",
      "key": "SD_SmartDocuments",
      "input": false,
      "components": [
        {
          "label": "Template",
          "type": "select",
          "key": "SD_SmartDocuments_Template",
          "input": true,
          "widget": "html5",
          "validate": {
            "required": true
          },
          "dataSrc": "custom",
          "clearOnRefresh": true
        },
        {
          "label": "Create",
          "action": "event",
          "showValidations": false,
          "block": true,
          "tableView": false,
          "key": "SD_SmartDocuments_Create",
          "type": "button",
          "event": "createDocument",
          "input": true,
          "properties": {
            "SmartDocuments_Group": "Dimpact/OpenZaak",
            "SmartDocuments_Data_Test_InformatieobjecttypeUuid": "efc332f2-be3b-4bad-9e3c-49a6219c92ad",
            "SmartDocuments_OpenZaakTest_InformatieobjecttypeUuid": "efc332f2-be3b-4bad-9e3c-49a6219c92ad"
          }
        }
      ]
    }
```

### Reference Table values
To display and use values from a reference table you can use:
* a fieldset with type `referenceTableFieldset`
* `select` type component with:
   * custom data source
   * properties containing `ReferenceTable_Code`

Example:
```json
{
  "label": "Communication channel",
  "type": "select",
  "key": "RT_ReferenceTable_Values",
  "input": true,
  "widget": "html5",
  "validate": {
    "required": true
  },
  "dataSrc": "custom",
  "attributes": {
    "ZAC_TYPE": "ZAC_referentie_tabel"
  },
  "properties": {
    "ReferenceTable_Code": "COMMUNICATIEKANAAL"
  }
}
```
:warning: prefixing the reference table code with 'BPMN_' is recommended to avoid conflicts with other ZAAK types and reference tables.

### Process data
* A `input` component, with the attribute `ZAC_TYPE` of `ZAC_process_data`, where the `key` is the name of the process data variable

```json
{
  "label": "Process data",
  "type": "input",
  "key": "<PROCESS_DATA_VARIABLE_NAME>",
  "input": true,
  "dataSrc": "custom",
  "attributes": {
    "ZAC_TYPE": "ZAC_process_data"
  }
}
```

### Documenten
* A `choicesjs` widget `select` component, with the attribute `ZAC_TYPE` of `ZAC_documenten`

```json
{
  "label": "Documents",
  "type": "select",
  "key": "ZAAK_Documents_Select",
  "input": true,
  "widget": "choicesjs",
  "multiple": true,
  "defaultValue": [],
  "clearOnRefresh": true,
  "dataSrc": "custom",
  "placeholder": "Select one or more documents",
  "customOptions": {
    "choicesOptions": {
      "removeItemButton": true,
      "placeholder": true,
      "searchEnabled": true,
      "shouldSort": false
    }
  },
  "validate": {
    "required": true
  },
  "attributes": {
    "ZAC_TYPE": "ZAC_documenten"
  }
}
```
