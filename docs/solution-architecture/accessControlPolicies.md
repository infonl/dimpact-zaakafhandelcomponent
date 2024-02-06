 # ZAC access control policies

This document describes the [Open Policy Agent (OPA)](https://www.openpolicyagent.org/) access control
policies that are used in ZAC.
These policies are used to enforce access control rules (=authorisation) for the different
resources (e.g. zaken, taken, documents) in the ZAC application per user role.
For example these policies may define that role X is allowed to create a new zaak, but role Y is not.
Since every user in ZAC has a role, these policies are used to enforce the access control rules for every user.

## ZAC roles

### Functional roles

As also documented in the [ZAC gebruikershandleiding](../manuals) ZAC supports the following functional user roles (in Dutch):

| Role                  | Description                                                                                                                                |
|:----------------------|:-------------------------------------------------------------------------------------------------------------------------------------------|
| Behandelaar           | Een zaakbehandelaar. Heeft alle rechten om met de werklijsten, zaken, taken en documenten te werken.                                       |
| Coördinator           | Een zaakcoördinator of ook wel werkverdeler genoemd. Heeft rechten om vanuit werklijsten werk te verdelen en zaken en taken te raadplegen. |
| Recordmanager         | Mag zaken en taken raadplegen en heeft aanvullende rechten op het gebied van documenten en beëindigde zaken.                               |
| Functioneel beheerder | Heeft toegang tot de beheerschermen van ZAC en kan daar diverse instellingen aanmaken en wijzingen.                                        |

Every ZAC user can have multiple of these roles.

### System roles

Besides the above functional roles, ZAC requires every user that logs in to ZAC to also have the mandatory
`zaakafhandelcomponent_user` system role.
Normally every ZAC user will automatically obtain this system role from the [ZAC IAM](iamArchitecture.md) configuration in
Keycloak.

### Domain roles

ZAC also supports the concept of 'domain' (domein) roles.
These roles normally correspond to user groups in the [ZAC IAM architecture](iamArchitecture.md) and typically to departments
in a muncipality. E.g. 'sociaal domein'.
These roles are used to grant access to a certain zaaktype (or set of zaaktypes) in ZAC.
This is done by configuring the 'zaakafhandelparameters' for a zaaktype in ZAC for a certain domain
and by giving a certain group of users the corresponding domain role in Keycloak.

Finally, there is a special `domein_elk_zaaktype` system role which will grant the user access to all zaaktypes in ZAC.

## ZAC policies per resource and role

The following OPA access control policies are enforced in ZAC for the roles listed above for the
various resources on which a user can perform actions:

| Rechten                                       |                                 Behandelaar                                 | Coördinator | Recordmanager | beheerder |
|:----------------------------------------------|:---------------------------------------------------------------------------:|:-----------:|:-------------:|:---------:|
|                                               |                                                                             |             |               |           |
| **_Zaak rechten (zie ook: Overige rechten)_** |                                                                             |             |               |           |
| lezen                                         |                                      X                                      |      X      |       X       |           |
| wijzigen                                      |                            X (status: zaak open)                            |             |       X       |           |
| wijzigen_toekenning                           |                                      X                                      |      X      |       X       |           |
| verlengen                                     |                                      X                                      |             |               |           |
| opschorten                                    |                                      X                                      |             |               |           |
| hervatten                                     |                                      X                                      |             |               |           |
| afbreken                                      |                                      X                                      |             |       X       |           |
| voortzetten                                   |                                      X                                      |             |       X       |           |
| heropenen                                     |                                                                             |             |       X       |           |
| creeeren_document                             |                            X (status: zaak open)                            |             |       X       |           |
| toevoegen_document                            |                            X (status: zaak open)                            |             |       X       |           |
| koppelen                                      |                            X (status: zaak open)                            |             |       X       |           |
| versturen_email                               |                            X (status: zaak open)                            |             |               |           |
| versturen_ontvangstbevestiging                |                            X (status: zaak open)                            |             |               |           |
| toevoegen_initiator_persoon                   |                            X (status: zaak open)                            |             |               |           |
| toevoegen_initiator_bedrijf                   |                            X (status: zaak open)                            |             |               |           |
| verwijderen_initiator                         |                            X (status: zaak open)                            |             |               |           |
| toevoegen_betrokkene_persoon                  |                            X (status: zaak open)                            |             |               |           |
| toevoegen_betrokkene_bedrijf                  |                            X (status: zaak open)                            |             |               |           |
| verwijderen_betrokkene                        |                            X (status: zaak open)                            |             |               |           |
| toevoegen_bag_object                          |                            X (status: zaak open)                            |             |               |           |
| aanmaken_taak                                 |                                      X                                      |             |               |           |
| vastleggen_besluit                            |                            X (status: zaak open)                            |             |               |           |
| verlengen_doorlooptijd                        |                                      X                                      |             |       X       |           |
|                                               |                                                                             |             |               |           |
| **_Taak rechten_**                            |                                                                             |             |               |           |
| lezen                                         |                                      X                                      |             |       X       |           |
| wijzigen (en aanmaken)                        |                                      X                                      |             |       X       |           |
| wijzigen_toekenning                           |                                      X                                      |             |               |           |
| wijzigen_formulier                            |                                      X                                      |             |       X       |           |
| creeeren_document                             |                                      X                                      |             |       X       |           |
| toevoegen_document                            |                                      X                                      |             |       X       |           |
|                                               |                                                                             |             |               |           |
| **_Document rechten_**                        |                                                                             |             |               |           |
| lezen                                         |                                      X                                      |      X      |       X       |           |
| wijzigen (en aanmaken)                        | X (status: zaak open, document onvergrendeld of vergrendeld door gebruiker) |             |       X       |           |
| toevoegen_nieuwe_versie                       | X (status: zaak open, document onvergrendeld of vergrendeld door gebruiker) |             |       X       |           |
| koppelen                                      |                X (status: zaak open, document onvergrendeld)                |             |       X       |           |
| verwijderen                                   |                                      X                                      |             |       X       |           |
| downloaden                                    |                                      X                                      |      X      |       X       |           |
| vergrendelen                                  |                            X (status: zaak open)                            |             |       X       |           |
| ontgrendelen                                  |                   X (document vergrendeld door gebruiker)                   |             |       X       |           |
| ondertekenen                                  | X (status: zaak open, document onvergrendeld of vergrendeld door gebruiker) |             |               |           |
|                                               |                                                                             |             |               |           |
| **_Werklijst rechten_**                       |                                                                             |             |               |           |
| documenten_inbox                              |                                                                             |             |       X       |           |
| documenten_ontkoppeld                         |                                      X                                      |             |       X       |           |
| documenten_ontkoppeld_verwijderen             |                                                                             |             |       X       |           |
| zaken_taken                                   |                                      X                                      |      X      |       X       |           |
| zaken_taken_verdelen                          |                                                                             |      X      |               |           |
|                                               |                                                                             |             |               |           |
| **_Overige rechten_**                         |                                                                             |             |               |           |
| starten_zaak                                  |                                      X                                      |             |               |           |
| beheren                                       |                                                                             |             |               |     X     |
| zoeken                                        |                                      X                                      |      X      |       X       |           |

Notes:
- `X (status: zaak open)` means that the zaak status must be 'Open' for the user role to have this right.
The state 'zaak open' effectively means that the zaak is in the state 'Intake' or 'In behandeling' (and
not in the state 'Afgerond' or 'Heropend')

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
