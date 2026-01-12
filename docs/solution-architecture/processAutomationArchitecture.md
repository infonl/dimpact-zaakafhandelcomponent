# ZAC Process Automation Architecture

The process automation architecture of ZAC is implemented using the embedded [Flowable](https://www.flowable.com/open-source) process automation engine, 
and supports both the [CMMN](https://www.omg.org/spec/CMMN/1.1/) and [BPMN](https://www.omg.org/spec/BPMN/2.0/) standards. 

ZAC supports one generic CMMN model to handle zaken. 
This CMMN model can be used for zaaktypes that can be handled by a generic process flow.
These are typically the more 'simple' zaaktypes.

Besides CMMN, ZAC also supports BPMN processes to handle zaken. 
BPMN is used for custom process flows, typically used for more complex zaaktypes.

Every zaaktype which is to be handled in ZAC, needs to be configured to either use the generic CMMN model or a custom BPMN process definition.
This is done using so called `zaakafhandelparameters` (also known as `CMMN` or `BPMN` zaaktype configurations).

## Generic ZAC CMMN model

The generic ZAC CMMN model can be found in [Generiek_zaakafhandelmodel.cmmn.xml](../../src/main/resources/cmmn/Generiek_zaakafhandelmodel.cmmn.xml).
ZAC uses this model to handle the zaak states and related functionality and user interface of ZAC is based on this model.
It is not possible to change this model without changing the related ZAC source code.

The model consists of two main zaak states: `Intake` and `In behandeling` ('in progress') and looks as follows:
![image](images/zac-generiek-cmmn-proces.png)

Editing of the ZAC CMMN model is not supported for end-users because it is tightly integrated with the ZAC application code.
When a ZAC developer needs to edit the CMMN model, they can use the online Flowable Designer (or edit the model file manually).

## BPMN process flows

ZAC provides BPMN for more complex zaaktypes that cannot be handled by the generic CMMN model.

BPMN support in ZAC uses the open source [Form.io](https://form.io) web form framework to model user task forms.

BPMN process definitions, as well as the corresponding Form.io forms, need to be created outside of ZAC before they can be used.
They can then be imported into ZAC using the ZAC admin interface.
See: [BPMN guide](../manuals/bpmn-guide/README.md) for details.
