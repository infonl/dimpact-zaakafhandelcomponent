# BPMN guide

## ZAC and BPMN

ZAC uses [Flowable](https://www.flowable.com/) as embedded process automation engine to support BPMN processes.
Forms that provide input for user tasks in BPMN processes are implemented using the open source [Form.io](https://form.io/) web form library.

## BPMN process definition

To create a BPMN process definition, you can:

- use Flowable [web editor](https://trial.flowable.com/design)
- start with our integration tests [process](../../../src/itest/resources/bpmn/itProcessDefinition.bpmn)

## Use of quotes

Please make sure that you use straight quotes (') in expressions. Sometimes your system will use
[smart (curly) quotes](https://practicaltypography.com/straight-and-curly-quotes.html) which will
not parse correctly.

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

- use the Form.io [Builder](https://formio.github.io/formio.js/app/builder)
- upload our integration tests [form](../../../src/itest/resources/bpmn/testForm.json)

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

ZAC extension fields are added to the Form.io form as a `ZAC_TYPE` `attribute` on the field component.

Note where things go: `ZAC_TYPE` is an **attribute**, while the settings a field takes
(`ReferenceTable_Code`) are **properties**. Form.io keeps these in two different objects and putting
one in the other silently does nothing.

The `ZAC_` prefix means "ZAC resolves this field", not "the data belongs to ZAC" — most of it comes
from other systems. And the names do not say what kind of field it is: `ZAC_groep` gives you a
*list of groups to pick from*, not the group this task already has. They are grouped below by what
they do.

Reading a value of the zaak or the taak needs no `ZAC_TYPE` at all — see
[Reading zaak and taak data](#reading-zaak-and-taak-data).

#### Fields that fill a list of options

Each of these turns a `select` into a dropdown that ZAC fills. The stored value is the first column
of "value / label".

| `ZAC_TYPE` | Offers | Value / label | Data comes from | Also needs |
|---|---|---|---|---|
| `ZAC_groep` | groups authorised for the behandelaar role on the zaak's zaaktype | `id` / `naam` | PABC (authorisation mappings); inactive groups are left out | — |
| `ZAC_medewerker` | members of the chosen group | `id` / `naam` | user identity management | `refreshOn`: key of the `ZAC_groep` field |
| `ZAC_status` | statustypen of the zaaktype | `naam` / `naam` | Open Zaak | — |
| `ZAC_resultaat` | resultaattypen of the zaaktype | `naam` / `naam` | Open Zaak | — |
| `ZAC_documenten` | documents on the zaak | `uuid` / `titel` | Open Zaak | — |
| `ZAC_referentie_tabel` | values of one reference table | `id` / `name` | ZAC's own database | property `ReferenceTable_Code` |
| `ZAC_smart_documents_template_groups` | template groups for the zaaktype | `id` / `naam` | SmartDocuments | — |
| `ZAC_smart_documents_template_group_templates` | templates in the chosen group | `id` / `naam` | SmartDocuments | `refreshOn`: key of the template-groups field |

Two are datagrids rather than dropdowns:

| `ZAC_TYPE` | Rows | Notes |
|---|---|---|
| `ZAC_documenten_niet_ondertekend` | the zaak's documents that are not signed yet | ZAC adds a validation requiring at least one ticked row |
| `ZAC_gekozen_documenten_niet_ondertekend` | the rows ticked in another grid | `refreshOn`: key of that grid. Titles and signing state are re-read, so anything signed meanwhile drops out |

#### Fields that read a single value

| `ZAC_TYPE` | Reads | Which ZAC assembles from |
|---|---|---|
| `ZAC_process_data` | one process variable, named by the field's `key` | the task data, into which ZAC merges the process variables each time the task is opened. On a completed task this is what was stored then, not the variable's current value |

Use it for a variable the process itself sets. For anything about the zaak or the taak, read the
zaak or the taak — see [Reading zaak and taak data](#reading-zaak-and-taak-data) — and see
[Process variables or the zaak?](#process-variables-or-the-zaak) for the eight values that exist
both ways.

#### Fields that only present something

| `ZAC_TYPE` | Renders |
|---|---|
| `ZAC_regel_link_tekstueel` | a text link in a column of a document datagrid |
| `ZAC_regel_link_oog_icoon` | the same as an eye icon |

Both take their destination from the datagrid they sit in, so they only work inside one of the
document grids above.

#### Undefined ZAC_TYPE

A `ZAC_TYPE` that is not in the list above (for example a typo such as `ZAC_documentn`) cannot be
recognised by ZAC. Form.io itself does not flag this, so instead of a silently empty field ZAC
replaces the component with a red-bordered message in the rendered task form:

> <span style="color: red; border: 1px solid red; padding: 0.25rem;">Undefined ZAC_TYPE: 'ZAC_documentn'</span>

Seeing this message means the form needs to be corrected: check the spelling of the `ZAC_TYPE`
attribute on the affected component and upload the form again.

## JavaScript in Form.io forms

Form.io forms support several places where script-like logic can be configured: conditional display, custom validation, calculated values, and `custom` component logic (e.g. a button's custom action). Before adding any JavaScript to a form, beheerders should understand what it can do and prefer a safer alternative where one exists.

### JavaScript executes as trusted code, not data

Any JavaScript added to a Form.io component runs unrestricted in the browser of every user who opens that task — it can read and change the entire submission, manipulate the page, and make network calls. Adding JavaScript to a form is effectively adding code to the application itself.

:warning: Only add JavaScript to a Form.io form if you understand exactly what the code does. If you are unsure, have a developer review it before uploading the form.

:warning: ZAC may in the future prevent uploading Form.io forms that contain JavaScript logic. Forms using JSON Logic will always remain allowed. New forms should therefore prefer JSON Logic over JavaScript wherever possible, to avoid needing rework later.

### Prefer JSON Logic over JavaScript where available

Form.io offers [JSON Logic](https://jsonlogic.com/) as a safer, declarative alternative to JavaScript for:

- conditional (advanced) logic — showing/hiding a component based on other field values
- custom validation — rejecting a submission unless a rule holds

A JSON Logic rule can only compute a value from the submitted form data. Unlike JavaScript, it cannot access the page (DOM), make network calls, or have other side effects. See:

- https://jsonlogic.com/ — the JSON Logic specification, with an interactive playground to try out rules
- https://jsonlogic.com/operations.html — a reference of all supported JSON Logic operators
- https://help.form.io/form-building/logic-and-conditions — Form.io's documentation on Advanced Conditions, Logic and Custom Validation, including where JSON Logic can be used instead of JavaScript

For simple show/hide behavior, also consider Form.io's "Simple Conditions", which need no code at all.

#### Example: conditional display

Not recommended (JavaScript, in "Advanced Conditions"):

```js
show = data.aanvraagType === "spoed";
```

Recommended (JSON Logic, in "Advanced Conditions"):

```json
{
  "==": [{ "var": "aanvraagType" }, "spoed"]
}
```

#### Example: custom validation

Not recommended (JavaScript):

```js
valid =
  data.eindDatum > data.startDatum
    ? true
    : "Einddatum moet na startdatum liggen";
```

Recommended (JSON Logic):

```json
{
  ">": [{ "var": "eindDatum" }, { "var": "startDatum" }]
}
```

### Calculated values: no JSON Logic alternative

Form.io does not offer a JSON Logic option for calculated values — only JavaScript is supported there. If you need a calculated value, keep it a pure expression: only read `data`/`row` and return a value. Do not use a calculated value to manipulate the DOM, call `submit()`, or perform network calls.

### `custom` component logic: highest risk, last resort

Some behavior cannot be expressed with JSON Logic or a calculated value, for example manipulating the page or submitting the form programmatically. As an illustration, imagine a `custom` component that looks up an address for a postcode via an external API directly from the browser, then auto-submits the form once the result comes back:

```json
{
  "custom": "instance.loading = true; var root = instance.root; root.shouldValidate = function(){return false;}; fetch('https://example-postcode-api.nl/lookup?postcode=' + data.postcode + '&apiKey=hardcoded-secret-key').then(function(response){ return response.json(); }).then(function(result){ root.getComponent('adres').setValue(result.adres); root.submission.state = 'draft'; return root.submit(); }).then(function(){ instance.loading = false; });"
}
```

This is a **not recommended** pattern:

- it reaches into `instance.root` and mutates the renderer's internal state (`root.shouldValidate`)
- it disables form validation for the entire form (`root.shouldValidate = function(){return false;}`)
- it calls an external API directly from the browser with an API key embedded in the form definition, which every user who opens the task can read
- it force-submits the form once the lookup completes, without the user confirming the result

If similar behavior is genuinely needed, and JSON Logic or a calculated value cannot express it:

- reach out to the ZAC development team to discuss the use case; possibly the behavior can be implemented differently, or possibly it warrants a feature request
- keep the JavaScript as small and narrowly scoped as possible
- avoid disabling validation, embedding credentials, or calling external APIs directly from the browser unless there is no other way to achieve the required behavior
- have the code reviewed by someone who understands JavaScript before uploading the form

## Supported functionality

The following functionality is supported by the BPMN process definition:

- Zaak
  - listing status and result types
  - changing status and result
  - suspending
  - resuming
  - extending
- Send email
- Send automatische ontvangstbevestiging
- User/group
  - listing groups/users
  - assigning a group/user to a zaak
  - assigning zaak's default group/user to a task
  - assigning the group/user of another task
- Documents
  - listing attached documents
  - listing available SmartDocuments templates
  - creating documents
  - signing documents
- Listing reference table data
- Process data

### Zaak

#### Listing statustypes

The available status types for a zaak can be displayed with:

- A `select` component, with the attribute `ZAC_TYPE` of `ZAC_status`

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

- A `select` component, with the attribute `ZAC_TYPE` of `ZAC_resultaat`

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

- create a service task
- set class `net.atos.zac.flowable.delegate.UpdateZaakJavaDelegate`
- add fields
  - `statustypeOmschrijving` to `stringvalue` or `expression` representing your desired zaak statustype omschrijving
  - `resultaattypeOmschrijving` to a valid `stringvalue` or `expression`, required by your zaak statustype

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

- create a service task
- set class `net.atos.zac.flowable.delegate.SuspendZaakDelegate`
- add fields:
  - `aantalDagen` - number of days to suspend the zaak for. Added to the current date.
  - `opschortingReden` - reason for suspension

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

- create a service task
- set class `net.atos.zac.flowable.delegate.ResumeZaakDelegate`
- add fields:
  - `hervattenReden` - reason for resuming
  - `hervattenDatum` - resume date (optional). If not set, the current date is used.

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

- create a service task
- set class `net.atos.zac.flowable.delegate.ExtendZaakDelegate`
- add fields:
  - `aantalDagen` - number of days to extend the zaak for
  - `verlengingReden` - reason for extending
  - `takenVerlengen` - whether to extend all tasks in the zaak (optional, default `false`)

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

- create a service task
- set class `net.atos.zac.flowable.delegate.SendEmailDelegate`
- add fields:
  - `to` - equal to the receiver's email address
  - `from` - the sender's email address
  - `replyTo` - the replyTo's email address
  - `template` - the name of the email template you want to use

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

#### Using zaakdata in email templates

Email templates support dynamic substitution of zaakdata values. This allows you to include
data that was collected during the BPMN process (e.g. from Form.io form fields) directly in the email subject and body.

Use the following syntax in the email template subject or body:

```
{ZAAKDATA:<key>}
```

Where `<key>` is the name of the zaakdata variable — this corresponds to the `key` of the Form.io form field
or any other variable stored in the zaakdata during the process.

:warning: If the key does not exist or has no value, it will be replaced with `Onbekend`.

Example: if your Form.io form has a field with key `customerPhone`, add the following to your email template subject or body:

```
Het telefoonnummer van de klant is: {ZAAKDATA:customerPhone}
```

To store a Form.io field value as zaakdata, use the `ZAC_process_data` type (see [Process data](#process-data)):

```json
{
  "label": "Telefoonnummer",
  "type": "input",
  "key": "customerPhone",
  "input": true,
  "attributes": {
    "ZAC_TYPE": "ZAC_process_data"
  }
}
```

### Send confirmation email (automatische ontvangstbevestiging)

To send a confirmation email to the zaak initiator or zaak-specific contact email address from a BPMN process:

- create a service task
- set class `nl.info.zac.flowable.bpmn.delegate.SendConfirmationEmailDelegate`
- add fields:
  - `from` - the sender's email address
  - `replyTo` - the reply-to email address (optional)
  - `template` - the name of the email template to use

Unlike `SendEmailDelegate`, the recipient address is resolved automatically from the zaak:

1. The email address from the zaak-specific contact details is used if available.
2. Otherwise, the default email address of the initiator of zaak is used. Or if the initiator does not have a default email address, the first email address of the initiator is used3. If no address can be found, no email is sent and the process continues.
3. If no email address could be found, no email is sent and the process continues.

The email is stored as a document attached to the zaak.

For example:

```xml
    <serviceTask id="ServiceTask_359" name="Send confirmation email" flowable:class="nl.info.zac.flowable.bpmn.delegate.SendConfirmationEmailDelegate">
      <extensionElements>
        <flowable:field name="from">
          <flowable:string><![CDATA[noreply@example.nl]]></flowable:string>
        </flowable:field>
        <flowable:field name="replyTo">
          <flowable:string><![CDATA[contact@example.nl]]></flowable:string>
        </flowable:field>
        <flowable:field name="template">
          <flowable:string><![CDATA[Ontvangstbevestiging]]></flowable:string>
        </flowable:field>
        <design:stencilid><![CDATA[ServiceTask]]></design:stencilid>
        <design:stencilsuperid><![CDATA[Task]]></design:stencilsuperid>
      </extensionElements>
    </serviceTask>
```

### User/group

#### Listing groups

- A `select` component, with the attribute `ZAC_TYPE` of `ZAC_groep`

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

- A `select` component, with the attribute `ZAC_TYPE` of `ZAC_medewerker`
- An optional attribute `refreshOn` to refresh the user list when the group changes. The value of this attribute should be the key of the group component.

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

- create a service task
- set class `net.atos.zac.flowable.delegate.UpdateZaakAssignmentDelegate`
- add fields:
  - `groepId` - group to use for the assignment
  - `behandelaarGebruikersnaam` - user to use for the assignment (optional)
  - `reden` - the reason for the assignment

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

- `zaakGroep` - group assigned to the zaak
- `zaakBehandelaar` (optional) - user assigned to the zaak

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

We are using `var:get` [function](https://documentation.flowable.com/latest/develop/be/be-expressions#variable-functions) here which tries to get a `zaakBehandelaar` variable value, so that it will not throw an exception when the variable does not exist.
As the group should always be provided when creating a zaak we set the candidate group directly to the value of `zaakGroep` variable.

#### Assigning the group/user of another task

To set the assignee and candidate group to the user/group used in another user task, you can use the `taken:behandelaar` and `taken:groep` functions:

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

- `select` type component with:
  - custom data source
  - attributes containing `ZAC_TYPE` of `ZAC_documenten`
  - multi select attribute (`type=select` with `multiple=true`)

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

This requires these three components:

##### Listing SmartDocuments template groups linked to the current zaaktype

- A `select` component, with the attribute `ZAC_TYPE` of `ZAC_smart_documents_template_groups`

```json
{
  "label": "Template Group",
  "type": "select",
  "key": "SmartDocuments_Group",
  "input": true,
  "dataSrc": "custom",
  "clearOnRefresh": true,
  "attributes": {
    "ZAC_TYPE": "ZAC_smart_documents_template_groups"
  }
}
```

##### Listing SmartDocuments templates linked to a template group and the current zaaktype

- A `select` component, with the attribute `ZAC_TYPE` of `ZAC_smart_documents_template_group_templates`
- An optional attribute `refreshOn` to refresh the template list when the template group changes. The value of this attribute should be the key of the template group component.

```json
{
  "label": "Template",
  "type": "select",
  "key": "SD_SmartDocuments_Template",
  "dataSrc": "custom",
  "clearOnRefresh": true,
  "input": true,
  "refreshOn": "SmartDocuments_Group",
  "attributes": {
    "ZAC_TYPE": "ZAC_smart_documents_template_group_templates"
  }
}
```

##### Create document button

- A `button` with:
- custom event: `"event": "createDocument"`

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
  "input": true
}
```

#### Listing attached documents

- A `choicesjs` widget `select` component, with the attribute `ZAC_TYPE` of `ZAC_documenten`

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

#### Signing documents

To automatically sign one or more documents as part of a process:

- create a user task with a form that lets the user select documents to sign (see form field below)
- create a service task after the user task
- set class `net.atos.zac.flowable.delegate.SignDocumentDelegate`
- optionally add a field:
  - `documentenKey` - the key of the form field that contains the selected documents (defaults to `ZAAK_Documenten_Te_Ondertekenen` if not set)

The delegate signs every row of that field whose `selected` checkbox is ticked. Documents that are already signed will be skipped automatically.

The form field for selecting documents to sign is a `datagrid` with the attribute `ZAC_TYPE` of
`ZAC_documenten_niet_ondertekend`. It is filled with all documents of the zaak that are not yet
signed, each with a `selected` checkbox:

```json
{
  "label": "Documenten",
  "type": "datagrid",
  "key": "ZAAK_Documenten_Ondertekenen_Selectie",
  "input": true,
  "disableAddingRemovingRows": true,
  "validate": { "required": true },
  "attributes": { "ZAC_TYPE": "ZAC_documenten_niet_ondertekend" },
  "components": [
    { "label": "", "key": "selected", "type": "checkbox", "input": true },
    {
      "label": "Titel",
      "key": "titel",
      "type": "textfield",
      "input": true,
      "disabled": true
    },
    {
      "key": "openen",
      "type": "htmlelement",
      "input": false,
      "attributes": { "ZAC_TYPE": "ZAC_regel_link_oog_icoon" }
    }
  ]
}
```

The optional `ZAC_regel_link_oog_icoon` column renders an icon linking to the document of that row in
a new tab; `ZAC_regel_link_tekstueel` renders the same link as text. Both take their route from the datagrid
they sit in, so no URL has to be configured in the form.

To confirm that selection in a following user task, use a `datagrid` with the attribute `ZAC_TYPE` of
`ZAC_gekozen_documenten_niet_ondertekend` and a `refreshOn` pointing at the key of the selection field
above. Only the documents selected there are shown. Because a task can stay open for days, their
titles and signing state are re-read when the task is opened, and any document that has been signed
in the meantime is left out:

```json
{
  "label": "Documenten",
  "type": "datagrid",
  "key": "ZAAK_Documenten_Te_Ondertekenen",
  "input": true,
  "disableAddingRemovingRows": true,
  "refreshOn": "ZAAK_Documenten_Ondertekenen_Selectie",
  "attributes": { "ZAC_TYPE": "ZAC_gekozen_documenten_niet_ondertekend" },
  "components": [
    { "label": "", "key": "selected", "type": "checkbox", "input": true },
    {
      "label": "Titel",
      "key": "titel",
      "type": "textfield",
      "input": true,
      "disabled": true
    },
    {
      "key": "openen",
      "type": "htmlelement",
      "input": false,
      "attributes": { "ZAC_TYPE": "ZAC_regel_link_oog_icoon" }
    }
  ]
}
```

Leave the `selected` checkbox of that confirmation grid editable: unticking a row there is how a user
drops a document again, and the delegate will then not sign it. Disabling the checkbox makes the grid
view-only and every listed document will be signed.

### Reference Table values

To display and use values from a reference table you can use:

- a fieldset with type `referenceTableFieldset`
- `select` type component with:
  - custom data source
  - attribute `ZAC_TYPE` of `ZAC_referentie_tabel`
  - properties containing `ReferenceTable_Code`

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

:warning: prefixing the reference table code with 'BPMN\_' is recommended to avoid conflicts with other ZAAK types and reference tables.

### Process data

- A `input` component, with the attribute `ZAC_TYPE` of `ZAC_process_data`, where the `key` is the name of the process data variable

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

### Reading zaak and taak data

ZAC puts the whole zaak and the whole taak into the form's template context, so reading a value
needs no `ZAC_TYPE` and no ZAC property at all — it is Form.io's own `{{ }}` interpolation:

```json
{
  "label": "",
  "type": "content",
  "key": "ZO_zaaknummer",
  "input": false,
  "html": "<strong>Zaaknummer:</strong> {{ zaak.identificatie }}"
}
```

Two objects are available, `zaak` and `taak`, and any property of either is reachable by its path:

```
{{ zaak.identificatie }}                  {{ taak.naam }}
{{ zaak.zaaktype.omschrijving }}          {{ taak.groep.naam }}
{{ zaak.resultaat.resultaattype.naam }}   {{ taak.fataledatum }}
```

Because it is a template and not a field, several values can go in one sentence — which is the main
thing this can do that a dedicated field type could not:

```html
Zaak {{ zaak.identificatie }} ({{ zaak.zaaktype.omschrijving }}) is behandeld door
{{ zaak.behandelaar.naam }}.
```

`zaak` is the zaak as read from Open Zaak, with its group and behandelaar resolved from the zaak
rollen via user identity management, and it is re-read every time the task is opened. `taak` is the
task in the process engine, with its candidate group and assignee resolved the same way. Both have
markup removed from their string values before the form sees them, so a value a user typed into the
zaak cannot inject anything into the page.

#### Helper functions

Five functions are in the context alongside the two objects.

| Function | Turns | Into |
|---|---|---|
| `ZAC_formatter_datum(value)` | `2026-08-24` | `24-08-2026` |
| `ZAC_formatter_leeg(value)` | an empty value | `-` |
| `ZAC_formatter_jaNee(value)` | `true` / `false` | `Ja` / `Nee` |
| `ZAC_formatter_lijst(list, "property")` | a list of objects | one property of every element, comma-separated |
| `ZAC_formatter_sleutels(object, "caption")` | an object | a boxed table of all its keys and values |

```
{{ ZAC_formatter_datum(zaak.startdatum) }}                        24-08-2026
{{ ZAC_formatter_leeg(zaak.toelichting) }}                        -
{{ ZAC_formatter_jaNee(zaak.isOpen) }}                            Ja
{{ ZAC_formatter_lijst(zaak.kenmerken, "kenmerk") }}              fakeKenmerk1, fakeKenmerk2
{{ ZAC_formatter_lijst(zaak.indicaties) }}                        OPSCHORTING, VERLENGD
{{ ZAC_formatter_sleutels(zaak.zaakdata, "process variables") }}  a table of every key
```

They all carry the `ZAC_formatter_` prefix so it is clear they come from ZAC and not from Form.io
itself — nothing in a plain Form.io installation is called any of these.

Two things about `ZAC_formatter_leeg`, both inherited from the Angular pipe:

- **Never use it on a boolean.** It treats anything falsy as empty, so `false` renders as `-` rather
  than as `false`. Use `ZAC_formatter_jaNee` for a boolean, or read it bare.
- An **empty date renders as nothing**, not as a dash, because `ZAC_formatter_datum` passes an empty
  value straight through and the two cannot be combined.

`ZAC_formatter_datum` and `ZAC_formatter_leeg` are the Angular `datum` and `empty` pipes the rest of
ZAC renders with, so a date and a dash mean the same in a task form as they do in the zaak screen.
Without `ZAC_formatter_leeg` an empty property renders as nothing at all, which is indistinguishable
from a wrong path — see [What a mistake looks like](#what-a-mistake-looks-like).

`ZAC_formatter_lijst` is how you read into a list; there is no `[]` path syntax.
`ZAC_formatter_sleutels` is for objects whose keys you do not know in advance, such as the process
variables — it renders whatever keys are there, so nothing has to be authored by hand. In its table a
nested object shows as `{n}` and a list as `[n]`, n being the number of entries; address those with
their own expression to look inside. Its second argument is an optional caption, and it is the only
place a reader learns which system the keys came from, so it is worth filling in.

`ZAC_getDocumentTitles` lives in the same context — see [Custom functions](#custom-functions).

#### Never write an expression you do not mean to run

Anything between `{{ }}` in a form is **executed**, including in a heading or a paragraph. So a form
cannot explain its own syntax by printing it:

```json
"html": "<p>Read a value with <code>{{ zaak.identificatie }}</code></p>"
```

That renders the zaak's identificatie instead of the example, and if the braces contain something
that is not valid JavaScript — a `…` placeholder, a description in words — the **whole component
fails to render** with `SyntaxError: Invalid or unexpected token` in the console. Not just that line:
the entire `html`.

HTML-escaping the braces (`&#123;&#123;`) does not help. Form.io sanitises the html by parsing it
into a DOM and serialising it back, which turns `&#123;` into a literal `{` before the expression is
evaluated.

So in a form, name an expression without its delimiters — `<code>zaak.identificatie</code>`,
`<code>ZAC_formatter_leeg</code>` — and leave the braces to the places where you actually want a
value.

#### What a mistake looks like

**Almost nothing reports itself.** Form.io evaluates `{{ }}` as JavaScript and renders the result;
there is no field-level error message.

| Written | Renders |
|---|---|
| a misspelled property | empty, plus one line in the browser console naming the full path |
| a property the zaak genuinely has no value for | empty |
| a property read through an object the zaak does not have (`zaak.behandelaar.naam` on an unassigned zaak) | empty — a correct expression, not a mistake |
| an object instead of a value (`{{ zaak.zaaktype }}`) | `[object Object]` |
| an unknown function (`{{ dutchDate(...) }}`) | nothing, and a `ReferenceError` in the console |

An empty result therefore means either "no value" or "wrong path", and the two look identical on
screen. The console line is what tells them apart, so keep devtools open while building a form.

#### Filling an editable field

Interpolation only renders text. To put a value **into** an input, use Form.io's own
`customDefaultValue`, which is JavaScript with the same context available:

```json
{
  "label": "Toelichting",
  "type": "textarea",
  "key": "IN_toelichting",
  "input": true,
  "customDefaultValue": "value = zaak.toelichting"
}
```

The helper functions work here too — `value = ZAC_formatter_datum(zaak.startdatum)` — and so does anything else
JavaScript can express.

Three rules for filled fields:

1. **A saved answer is never overwritten.** Form.io skips a default value once the submission
   already carries the key, so what the user typed survives a reopen. This is Form.io's own
   behaviour, not something ZAC adds.
2. **Do not use a format on a field that parses its own value.** A date picker reads the raw value
   and formats it for display itself; giving it `24-08-2026` breaks it. Use
   `value = zaak.startdatum`, not `value = ZAC_formatter_datum(zaak.startdatum)`.
3. **Do not use a process variable name as the `key`.** Completing a task writes every submitted key
   back as a process variable, so a field keyed `zaakGroep` overwrites that variable. Prefix the keys
   of your own fields.

That third rule is the difference that matters between the two mechanisms: an interpolated value is
only rendered and can never be written back, while a filled field becomes part of the submission.

#### Process variables or the zaak?

Seven zaak values also exist as process variables — `zaakUUID`, `zaakIdentificatie`, `zaaktypeUUID`,
`zaaktypeOmschrijving`, `zaakGroep`, `zaakBehandelaar`, `zaakCommunicatiekanaal` — plus `initiator`.
They are reachable three ways: bare as `{{ zaakGroep }}`, through `{{ zaak.zaakdata.zaakGroep }}`,
and as the `key` of a `ZAC_process_data` field.

**Read the zaak instead.** Not because the copies are out of date — ZAC does keep them in step — but
because those eight are all there will ever be. `startdatum`, `status`, `resultaat`, `omschrijving`,
`toelichting`, `kenmerken`, `besluiten` and the rest are not process variables and each one would
need a code change to become one. `{{ zaak.… }}` gives you all of them today and nothing to request
tomorrow.

Use `ZAC_process_data` for what it is for: a variable the **process itself** sets, such as a value an
earlier task submitted.

#### Trying it out

`src/itest/resources/bpmn/data-diagnostic` holds a process with four task forms: every addressable
property of the zaak and of the taak with the expression that produced it, the process variables with
a panel comparing them against the zaak, filled input fields, and panels of deliberate mistakes
showing what each failure actually looks like. Upload the process and its forms to see all of it
against a real zaak.

### Custom functions

ZAC supports custom functions in Form.io `content` components via the `{{ }}` template syntax.
These functions are evaluated client-side and can be used to display dynamic data in read-only content blocks.

#### ZAC_getDocumentTitles

Resolves a list of document UUIDs stored in a taakdata field to their human-readable document titles.
The titles are formatted as a Dutch conjunction list (e.g. `Document A, Document B en Document C`).

- Use a `content` component with an `html` property containing `{{ ZAC_getDocumentTitles(<fieldKey>) }}`
- `<fieldKey>` must match the name of a taakdata field containing documents, in any of the shapes below (it does not need to be a component in the form)

The argument is the bare field name, without quotes. Taakdata is available as variables in the template, so
what the function receives is the value of that field. Several `content` components may call it with different
fields; the titles of all of them are fetched when the form is opened.

Any field holding documents works, whichever shape it stores them in:

- a single UUID
- a list of UUIDs, the way a `ZAC_documenten` select stores them: `["06a47923-…", "44e891f7-…"]`
- a list of datagrid rows, the way the signing grids store them: `[{ "selected": true, "titel": "…", "uuid": "…" }]`

Rows whose `selected` checkbox was unticked are left out, so the list matches what a signing task will
actually sign. Entries without a UUID are ignored. Titles are always read from the document itself, so a
`titel` stored in a row earlier can never show up out of date.

```json
{
  "label": "Selected documents",
  "type": "content",
  "key": "selectedDocuments",
  "html": "<p>Gekozen documenten:</p><p>{{ ZAC_getDocumentTitles(ZAAK_Documents_Select) }}</p>",
  "input": false
}
```

If a document cannot be fetched or has no title, the UUID is used as a fallback.

#### Zaak variables in the process

When a BPMN zaak starts, ZAC copies a few zaak fields into the process variables, because the
process engine cannot read the zaak itself. They are what makes `${zaakGroep}` and
`${var:get(zaakBehandelaar)}` work in the process definition.

| Variable | Reachable with `ZAC_process_data` |
|---|---|
| `zaakIdentificatie` | yes |
| `zaakCommunicatiekanaal` | yes |
| `zaakGroep` | yes |
| `zaakBehandelaar` | yes, once the zaak has a behandelaar |
| `zaaktypeOmschrijving` | yes |
| `zaakUUID` | **no** — left out of the task data |
| `zaaktypeUUID` | **no** — left out of the task data |

`zaakUUID` and `zaaktypeUUID` exist as process variables, and the process definition can use them,
but ZAC does not pass them into the task data — so a `ZAC_process_data` field with either of those
keys stays empty. This is a deliberate exclusion, not a bug.

**Read the zaak instead of these variables.** `{{ zaak.identificatie }}` is not limited to these
seven fields: `startdatum`, `status`, `resultaat`, `omschrijving`, the initiator and the deadlines are
all reachable, and `zaak.uuid` and `zaak.zaaktype.uuid` work normally where the two variables above
do not. Reserve `ZAC_process_data` for variables the process itself sets. See
[Process variables or the zaak?](#process-variables-or-the-zaak).
