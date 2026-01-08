# BPMN guide (BETA)

:fire: The functionality described below is still "Beta". Beta software may contain errors or inaccuracies and may not function as well as regular releases. :fire:

## ZAC and BPMN
ZAC uses [Flowable](https://www.flowable.com/) to support BPMN processes. Forms that provide input for the BPMN processes are implemented using the [Forms.io](https://forms.io/) framework.

## BPMN process definition
To create a BPMN process definition, you can:
* use Flowable [web editor](https://trial.flowable.com/design)
* start with our integration tests [process](../../../src/itest/resources/bpmn/itProcessDefinition.bpmn)

### Requirements

#### Candidate group/user
The "User tasks" should have a candidate group or user set. 

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
4. Click on the plus sign to open a file selection dialogue
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

### ZAC extensions

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


## Supported functionality
The following functionality is supported by the BPMN process definition:
* Zaak
   * listing status and result types
   * changing status and result
   * suspending
   * resuming
   * extending
* Send email
* User/group
   * listing groups/users
   * assigning a group/user to a zaak
   * assigning zaak's default group/user to a task
   * assigning the group/user of another task
* Documents
  * listing attached documents
  * listing available SmartDocuments templates
  * creating documents
* Listing reference table data
* Process data

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
  * `hervattenDatum` - resume date (optional). If not set, the current date is used.

For example:
```xml
    <serviceTask id="ServiceTask_361" name="Resume" flowable:class="net.atos.zac.flowable.delegate.ResumeZaakDelegate">
      <extensionElements>
        <flowable:field name="hervattenReden">
          <flowable:expression><![CDATA[resume test]]></flowable:expression>
        </flowable:field>
        <flowable:field name="hervattenDatum">
          <flowable:expression><![CDATA[${ZK_Resume_Date}]]></flowable:expression>
        </flowable:field>
        <design:stencilid><![CDATA[ServiceTask]]></design:stencilid>
        <design:stencilsuperid><![CDATA[Task]]></design:stencilsuperid>
      </extensionElements>
    </serviceTask>
```

The `hervattenDatum` is a date-time string with a time-zone in the ISO-8601 calendar system: `2025-11-14T17:38:21.929149+01:00[Europe/Amsterdam]`. 

#### Extending
To extend a zaak:
* create a service task
* set class `net.atos.zac.flowable.delegate.ExtendZaakDelegate`
* add fields:
  * `aantalDagen` - number of days to extend the zaak for
  * `verlengingReden` - reason for extending
  * `takenVerlengen` - whether to extend all tasks in the zaak (optional, default `false`)

For example:
```xml
    <serviceTask id="ServiceTask_378" name="Extend" flowable:class="net.atos.zac.flowable.delegate.ExtendZaakDelegate">
      <extensionElements>
        <flowable:field name="aantalDagen">
          <flowable:expression><![CDATA[${extendDays}]]></flowable:expression>
        </flowable:field>
        <flowable:field name="verlengingReden">
          <flowable:string><![CDATA[Extend test]]></flowable:string>
        </flowable:field>
        <flowable:field name="takenVerlengen">
          <flowable:expression><![CDATA[${extendTasks}]]></flowable:expression>
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

#### Listing groups
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

#### Listing users in a group
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

#### Assigning a group/user to a zaak
To assign a group or user to a zaak:
* create a service task
* set class `net.atos.zac.flowable.delegate.UpdateZaakAssignmentDelegate`
* add fields:
    * `groepId` - group to use for the assignment
    * `behandelaarGebruikersnaam` - user to use for the assignment (optional)
    * `reden` - the reason for the assignment

For example:
```xml
    <serviceTask id="ServiceTask_362" name="Assign for approval" flowable:class="net.atos.zac.flowable.delegate.UpdateZaakAssignmentDelegate">
      <extensionElements>
        <flowable:field name="groepId">
          <flowable:expression><![CDATA[${AM_TeamBehandelaar_Groep}]]></flowable:expression>
        </flowable:field>
        <flowable:field name="behandelaarGebruikersnaam">
          <flowable:expression><![CDATA[${AM_TeamBehandelaar_Medewerker}]]></flowable:expression>
        </flowable:field>
        <flowable:field name="reden">
          <flowable:expression><![CDATA[Please check case ${zaakIdentificatie}]]></flowable:expression>
        </flowable:field>
        <design:stencilid><![CDATA[ServiceTask]]></design:stencilid>
        <design:stencilsuperid><![CDATA[Task]]></design:stencilsuperid>
      </extensionElements>
    </serviceTask>
```

#### Assigning zaak's default group/user to a task
The following BPMN-specific variables can be used in expressions in the BPMN process:
* `zaakGroep` - group assigned to the zaak
* `zaakBehandelaar` (optional) - user assigned to the zaak

The above variables can be used in `assignee` and `candidateGroups` attributes for example:
For example:
```xml
<userTask id="userTask"
          name="User details"
          flowable:assignee="${var:get(zaakBehandelaar)}"
          flowable:candidateGroups="${zaakGroep}"
          flowable:formKey="testForm"
          flowable:formFieldValidation="false">
  ... the rest of userTask tags ...
</userTask>
```

We're using `var:get` [function](https://documentation.flowable.com/latest/develop/be/be-expressions#variable-functions) here which tries to get a `zaakBehandelaar` variable value, so that it won’t throw an exception when the variable doesn’t exist.
As the group should always be provided when creating a zaak we set the candidate group directly to the value of `zaakGroep` variable.


#### Assigning the group/user of another task
To set the asignee and candidate group to the user/group used in another user task, you can use the `taken:behandelaar` and `taken:groep` functions:
```xml
<userTask id="userTask"
          name="User details"
          flowable:assignee="${taken:behandelaar('userTaskId')}"
          flowable:candidateGroups="${taken:groep('userTaskId')}"
          flowable:formKey="testForm"
          flowable:formFieldValidation="false">
  ... the rest of userTask tags ...
</userTask>
```

Note: the `userTaskId` should be replaced with the actual id of the user task in the BPMN process.

### Documents

#### Listing available documents
To display linked documents of a zaak you can use:
* `select` type component with:
  * custom data source
  * attributes containing `ZAC_TYPE` of `ZAC_documenten`
  * multi select attribute (`type=select` with `multiple=true`)

Example:
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
      "placeholder": false,
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

#### Creating documents
This requires two components:

##### Smartdocuments template
* A `select` component with:
  * the attribute `ZAC_TYPE` of `ZAC_smart_documents_template`
  * custom data source: `"dataSrc": "custom"`
  * properties containing `SmartDocuments_Group`

Example:
```json
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
  "attributes": {
    "ZAC_TYPE": "ZAC_smart_documents_template"
  },
  "properties": {
    "SmartDocuments_Group": "Dimpact/OpenZaak"
  },
  "clearOnRefresh": true
}
```

##### Create document button
* A `button` with:
* SmartDocument properties
  * `SmartDocuments_Group` needs to be set to the same value as in the template select component
* custom event: `"event": "createDocument"`

Example:
```json
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
```

The path to the SmartDocuments group specifies which group of templates to list. For example: `root/nested` `group/with/more/nesting`.

First, a lookup for the template-specific information object type (informatieobjecttype) UUID is attempted. If a template-specific UUID is not found, the default is used.
The template name should be snake-case (`Data Test` becomes `Data_Test`).

#### Listing attached documents
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

### Reference Table values
To display and use values from a reference table you can use:
* a fieldset with type `referenceTableFieldset`
* `select` type component with:
  * custom data source
  * attribute `ZAC_TYPE` of `ZAC_referentie_tabel`
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

#### Supported process data variables
* `zaakUUID` - zaak UUID
* `zaakIdentificatie` - zaak id
* `zaakCommunicatiekanaal` - zaak communication channel
* `zaakGroep` - zaak group
* `zaakBehandelaar` - zaak assigned user`
* `zaaktypeUUID` - zaaktype UUID
* `zaaktypeOmschrijving` - zaaktype description
