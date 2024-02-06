 # ZAC access control policies

This document describes the [Open Policy Agent (OPA)](https://www.openpolicyagent.org/) access control
policies that are used in ZAC.
These policies are used to enforce access control rules (=authorisation) for the different
resources (e.g. zaken, taken, documents) in the ZAC application per user role.
For example these policies may define that role X is allowed to create a new zaak, but role Y is not.
Since every user in ZAC has a role, these policies are used to enforce the access control rules for every user.

## ZAC roles

ZAC supports the following user roles:

| Role          | Description |
|:-------------:|:-----------:|
| Behandelaar   | A user who is responsible for handling zaken and taken. |
| Coordinator   | A user who is responsible for coordinating zaken and taken. |
| Recordmanager | A user who is responsible for managing records. |
| Beheerder     | A user who is responsible for managing the ZAC application. |


## ZAC policies per resource and role

The following OPA access control policies are enforced in ZAC for the roles listed above for the
various resources on which a user can perform actions:

| Rechten                           |                      behandelaar                       | coordinator | recordmanager | beheerder |
|:----------------------------------|:------------------------------------------------------:|:-----------:|:-------------:|:---------:|
| **_Zaak rechten_**                |                                                        |             |               |           |
| lezen                             |                           X                            |      X      |       X       |           |
| wijzigen                          |                          Open                          |             |       X       |           |
| wijzigen_toekenning               |                           X                            |      X      |       X       |           |
| verlengen                         |                           X                            |             |               |           |
| opschorten                        |                           X                            |             |               |           |
| hervatten                         |                           X                            |             |               |           |
| afbreken                          |                           X                            |             |       X       |           |
| voortzetten                       |                           X                            |             |       X       |           |
| heropenen                         |                                                        |             |       X       |           |
| creeeren_document                 |                          Open                          |             |       X       |           |
| toevoegen_document                |                          Open                          |             |       X       |           |
| koppelen                          |                          Open                          |             |       X       |           |
| versturen_email                   |                          Open                          |             |               |           |
| versturen_ontvangstbevestiging    |                          Open                          |             |               |           |
| toevoegen_initiator_persoon       |                          Open                          |             |               |           |
| toevoegen_initiator_bedrijf       |                          Open                          |             |               |           |
| verwijderen_initiator             |                          Open                          |             |               |           |
| toevoegen_betrokkene_persoon      |                          Open                          |             |               |           |
| toevoegen_betrokkene_bedrijf      |                          Open                          |             |               |           |
| verwijderen_betrokkene            |                          Open                          |             |               |           |
| toevoegen_bag_object              |                          Open                          |             |               |           |
| aanmaken_taak                     |                           X                            |             |               |           |
| vastleggen_besluit                |                          Open                          |             |               |           |
| verlengen_doorlooptijd            |                           X                            |             |       X       |           |
|                                   |                                                        |             |               |           |
| **_Taak rechten_**                |                                                        |             |               |           |
| lezen                             |                           X                            |             |       X       |           |
| wijzigen                          |                           X                            |             |       X       |           |
| wijzigen_toekenning               |                           X                            |             |               |           |
| wijzigen_formulier                |                           X                            |             |       X       |           |
| creeeren_document                 |                           X                            |             |       X       |           |
| toevoegen_document                |                           X                            |             |       X       |           |
| **_Document rechten_**            |                                                        |             |               |           |
| lezen                             |                           X                            |      X      |       X       |           |
| wijzigen                          | zaak open, onvergrendeld of vergrendeld door gebruiker |             |       X       |           |
| toevoegen_nieuwe_versie           | zaak open, onvergrendeld of vergrendeld door gebruiker |             |       X       |           |
| koppelen                          |                zaak open, onvergrendeld                |             |       X       |           |
| verwijderen                       |                           X                            |             |       X       |           |
| downloaden                        |                           X                            |      X      |       X       |           |
| vergrendelen                      |                       zaak open                        |             |       X       |           |
| ontgrendelen                      |               vergrendeld door gebruiker               |             |       X       |           |
| ondertekenen                      | zaak open, onvergrendeld of vergrendeld door gebruiker |             |               |           |
| **_Werklijst rechten_**           |                                                        |             |               |           |
| documenten_inbox                  |                                                        |             |       X       |           |
| documenten_ontkoppeld             |                           X                            |             |       X       |           |
| documenten_ontkoppeld_verwijderen |                                                        |             |       X       |           |
| zaken_taken                       |                           X                            |      X      |       X       |           |
| zaken_taken_verdelen              |                                                        |      X      |               |           |
| **_Overige rechten_**             |                                                        |             |               |           |
| starten_zaak                      |                           X                            |             |               |           |
| beheren                           |                                                        |             |               |     X     |
| zoeken                            |                           X                            |      X      |       X       |           |

## Technical implementation

The OPA policies are implemented using OPA policy files in OPA's native
[Rego policy language](https://www.openpolicyagent.org/docs/latest/policy-language/) and can be found
in the [ZAC OPA policies folder](../../src/main/resources/policies).

These policies are deployed by ZAC on startup to the OPA server, which is part of the ZAC ecosystem
and needs to be available for ZAC. The OPA server provides a REST API with which ZAC integrates.

Then during normal operation the following happens:

1. When a user with a certain role X requests to perform a certain action
on ZAC (e.g. create a new zaak), the ZAC backend will send a request to the OPA server to obtain the current
access control rights ('rechten') for the resource in question (zaken in this case).
2. With these access control rights ZAC then checks if role X is allowed to perform the requested action
(create a new zaak in this case).
3. If this is allowed the user may perform the action.
Should this however not be allowed an error message will be logged and returned to the user.

The ZAC frontend however will normally prevent the user from even trying to perform this requested action
(the frontend also has access to these access control rights) so the scenario above can normally
only happen if a user bypasses the ZAC frontend and tries to perform the action directly on the ZAC backend.

## Updating OPA policies

When updating OPA policies in a Rego file please make sure to also update the corresponding policy
matrix in this document.
