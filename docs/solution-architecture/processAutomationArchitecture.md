# ZAC Process Automation Architecture

The process automation architecture of ZAC is based on the [CMMN](https://www.omg.org/spec/CMMN/1.1/) and 
[BPMN](https://www.omg.org/spec/BPMN/2.0/) standards.

ZAC supports a generic CMMN model, which is typically used for zaaktypes that can be 
handled by a generic process flow. 
Per zaaktype this generic CMMN model can be configured in ZAC using 'zaakafhandelparameters'.
ZAC also supports BPMN processes for custom process flows, typically used for more complex zaaktypes.

## Generic ZAC CMMN model

The generic ZAC CMMN model can be found in [Generiek_zaakafhandelmodel.cmmn.xml](../../src/main/resources/cmmn/Generiek_zaakafhandelmodel.cmmn.xml).
ZAC uses this model to handle the zaak states and related functionality and user interface of ZAC is based on this model.
It is not possible to change this model without changing the related ZAC source code.

The model consists of two main zaak states: `Intake` and `In behandeling` ('in progress') and looks as follows:
![image](images/zac-generiek-cmmn-proces.png)

Editing of the ZAC CMMN model is not supported for end-users because it is tightly integrated with the ZAC application code.
When a ZAC developers needs to edit the CMMN model, they can use the online Flowable Designer (or edit the model file manually).

## BPMN process flows

:warning: BPMN functionality is still in active development and currently needs to be enabled using a 'feature flag' environment variable.

To have a flexible and user customizable process flow ZAC supports the BPMN standard. 

BPMN process flows make use of the open source [Flowable](https://www.flowable.com/open-source)
process automation engine which is embedded within the ZAC application.

BPMN models can be generated and edited with the online Flowable Designer, and can be imported into ZAC
using the ZAC admin interface. 

BPMN support in ZAC uses the Open Source [form.io](https://form.io) web form framework to model process task forms.
Form.io forms can be created and edited using for example the form.io online form designer, and can be imported into ZAC using the ZAC admin interface.

