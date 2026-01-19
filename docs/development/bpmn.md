# BPMN processes

Besides [CMMN](cmmn.md), ZAC also supports [BPMN (Business Process Model and Notation)](https://www.bpmn.org/) processes to handle zaken.

Also see:
* [Process automation architecture](../solution-architecture/processAutomationArchitecture.md)
* [BPMN guide](../manuals/bpmn-guide/README.md)

## BPMN process automation engine 

BPMN process flows make use of the open source [Flowable](https://www.flowable.com/open-source)
process automation engine which is embedded within the ZAC application.
Business process data is stored in the ZAC Flowable database. 

## BPMN form.io forms

The [Form.io](https://github.com/formio/angular) Angular components are used to:
* visualize the progress
* provide input forms

At the moment (2024-09-30) input forms in the Flowable Angular use Bootstrap 4 with several [security vulnerabilities](https://security.snyk.io/package/npm/bootstrap/4.0.0). The upcoming release of v8 of the renderer comes with Bootstrap 5 is used.   
