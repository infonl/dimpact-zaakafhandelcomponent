 # ZAC access control policies

This document describes the [Open Policy Agent (OPA)](https://www.openpolicyagent.org/) access control policies that are used in ZAC.
These policies are used to enforce access control rules (=authorisation) for the different resources (e.g. zaken, taken, documents) in the ZAC application per user role.
For example these policies may define that role X is allowed to create a new zaak, but role Y is not.
Since every user in ZAC has a role, these policies are used to enforce the access control rules for every user.

## ZAC roles

### Functional roles

As also documented in the [ZAC gebruikershandleiding](../manuals) ZAC supports the following functional user roles (in Dutch):

| Role          | Description                                                                                                                                |
|:--------------|:-------------------------------------------------------------------------------------------------------------------------------------------|
| Behandelaar   | Een zaakbehandelaar. Heeft alle rechten om met de werklijsten, zaken, taken en documenten te werken.                                       |
| Coördinator   | Een zaakcoördinator of ook wel werkverdeler genoemd. Heeft rechten om vanuit werklijsten werk te verdelen en zaken en taken te raadplegen. |
| Recordmanager | Mag zaken en taken raadplegen en heeft aanvullende rechten op het gebied van documenten en beëindigde zaken.                               |
| Beheerder     | De functioneel beheerder. Heeft toegang tot de beheerschermen van ZAC en kan daar diverse instellingen aanmaken en wijzingen.              |

Typically, these roles are assigned to individual users through groups.

Note that the ZAC authorisation architecture has been set up in such that a user also needs all 'lower-level' roles 
next to their 'main' role in order to be able to work in ZAC.
This means the following in practise:

- A user with the 'Coordinator' role also needs to have the 'Behandelaar' role
- A user with the 'Recordmanager' role also needs to have the 'Behandelaar' and 'Coordinator'  roles
- A user with the 'Beheerder' role also needs to have the 'Behandelaar', 'Coordinator' and 'Recordmanager' roles

### System roles

Besides the above functional roles, ZAC requires every user that logs in to ZAC to also have the mandatory `zaakafhandelcomponent_user` system role.
Normally every ZAC user will automatically obtain this system role from the [ZAC IAM](iamArchitecture.md) configuration in Keycloak.

### Domain roles

ZAC also supports the concept of 'domain' (domein) roles.
These roles normally correspond to user groups in the [ZAC IAM architecture](iamArchitecture.md) and typically to departments
in a muncipality. E.g. 'sociaal domein'.
These roles are used to grant access to a certain zaaktype (or set of zaaktypes) in ZAC.
This is done by configuring the 'zaakafhandelparameters' for a zaaktype in ZAC for a certain domain
and by giving a certain group of users the corresponding domain role in Keycloak.

Finally, there is a special `domein_elk_zaaktype` system role which will grant the user access to all zaaktypes in ZAC.

## ZAC policies per resource and role

The following OPA access control policies are enforced in the ZAC backend for the roles listed above for the
various resources on which a user can perform actions:

| Rechten                                              |                                                        Behandelaar                                                        | Coördinator |                           Recordmanager                            | Beheerder |
|:-----------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------:|:-----------:|:------------------------------------------------------------------:|:---------:|
|                                                      |                                                                                                                           |             |                                                                    |           |
| **_Zaak rechten_** <br/>_(zie ook: Overige rechten)_ |                                                                                                                           |             |                                                                    |           |
| lezen                                                |                                                             ✅                                                             |             |                                                                    |           |
| wijzigen                                             |                                                    ✅<br/>_(zaak open)_                                                    |             |                 ✅<br/>_(zaak open en afgehandeld)_                 |           |
| toekennen                                            |                                                             ✅                                                             |             |                                                                    |           |
| behandelen                                           |                                                             ✅                                                             |             |                                                                    |           |
| afbreken                                             |                                                             ✅                                                             |             |                                                                    |           |
| heropenen                                            |                                                                                                                           |             |                                 ✅                                  |           |
| bekijkenZaakdata                                     |                                                                                                                           |             |                                                                    |     ✅     |
| wijzigenDoorlooptijd                                 |                                                             ✅                                                             |             |                                                                    |           |
| verlengen                                            |                       ✅<br/>_(zaak open, niet heropend, niet opgeschort, en niet al keer verlengd)_                       |             |                                                                    |           |
| opschorten                                           |                                    ✅<br/>_(zaak open, niet heropend, niet opgeschort)_                                    |             |                                                                    |           |
| hervatten                                            |                                                             ✅                                                             |             |                                                                    |           |
| creeeren_document                                    |                                                    ✅<br/>_(zaak open)_                                                    |             |                                                                    |           |
| toevoegen_document                                   |                                                    ✅<br/>_(zaak open)_                                                    |             |                                 ✅                                  |           |
| koppelen                                             |                                                ✅<br/>_(beide zaken open)_                                                 |             |       ✅<br/>_(beide zaken open of beide zaken afgehandeld)_        |           |
| versturen_email                                      |                                                    ✅<br/>_(zaak open)_                                                    |             |                                                                    |           |
| versturen_ontvangstbevestiging                       |                                                    ✅<br/>_(zaak open)_                                                    |             |                                                                    |           |
| toevoegen_initiator_persoon                          |                                                    ✅<br/>_(zaak open)_                                                    |             |                                 ✅                                  |           |
| toevoegen_initiator_bedrijf                          |                                                    ✅<br/>_(zaak open)_                                                    |             |                                 ✅                                  |           |
| verwijderen_initiator                                |                                                    ✅<br/>_(zaak open)_                                                    |             |                                 ✅                                  |           |
| toevoegen_betrokkene_persoon                         |                                                    ✅<br/>_(zaak open)_                                                    |             |                                 ✅                                  |           |
| toevoegen_betrokkene_bedrijf                         |                                                    ✅<br/>_(zaak open)_                                                    |             |                                 ✅                                  |           |
| verwijderen_betrokkene                               |                                                    ✅<br/>_(zaak open)_                                                    |             |                                 ✅                                  |           |
| toevoegen_bag_object                                 |                                                    ✅<br/>_(zaak open)_                                                    |             |                                 ✅                                  |           |
| starten_taak                                         |                                        ✅<br/>_(afhankelijk van CMMN status zaak)_                                         |             |                                                                    |           |
| vastleggen_besluit                                   |                             ✅<br/>_(zaak open, niet in intake, zaaktype heeft besluittypen)_                              |             |                                                                    |           |
| verlengen_doorlooptijd                               |                                                    ✅<br/>_(zaak open)_                                                    |             |                                                                    |           |
|                                                      |                                                                                                                           |             |                                                                    |           |
| **_Taak rechten_**                                   |                                                                                                                           |             |                                                                    |           |
| lezen                                                |                                                             ✅                                                             |             |                                                                    |           |
| wijzigen                                             |                                                             ✅                                                             |             |                                                                    |           |
| toekennen                                            |                                                             ✅                                                             |             |                                                                    |           |
| creeeren_document                                    |                                              ✅<br/>_(zaak open, taak open)_                                               |             |                                                                    |           |
| toevoegen_document                                   |                                              ✅<br/>_(zaak open, taak open)_                                               |             |                                                                    |           |
|                                                      |                                                                                                                           |             |                                                                    |           |
| **_Document rechten_**                               |                                                                                                                           |             |                                                                    |           |
| lezen                                                |                                                             ✅                                                             |             |                                                                    |           |
| wijzigen                                             | ✅<br/>_(zaak open/heropend, document onvergrendeld of vergrendeld door gebruiker, document status anders dan definitief)_ |             | ✅<br/>_(document ontgrendelen + documenten met status definitief)_ |           |
| verwijderen                                          |                     ✅<br/>(zaak open, document status anders dan definitief, document onvergrendeld)_                     |             |                  ✅<br/>_(document onvergrendeld)_                  |           |
| vergrendelen                                         |                                               ✅<br/>_(zaak open/heropend)_                                                |             |                                                                    |           |
| ontgrendelen                                         |                             ✅<br/>_(zaak open/heropend, document vergrendeld door gebruiker)_                             |             |                          ✅<br/>_(altijd)_                          |           |
| ondertekenen                                         |                    ✅<br/>_(zaak open/heropend, document onvergrendeld of vergrendeld door gebruiker)_                     |             |                                                                    |           |
| toevoegen_nieuwe_versie                              |     ✅<br/>_(zaak open, document onvergrendeld of vergrendeld door gebruiker, document status anders dan definitief)_      |             |       ✅<br/>_(altijd, behalve bij ondertekende documenten)_        |           |
| verplaatsen (koppelen)                               |     ✅<br/>_(zaak open, document onvergrendeld of vergrendeld door gebruiker, document status anders dan definitief)_      |             |           ✅<br/>_(afgehandelde zaak, status definitief)_           |           |
| ontkoppelen                                          |      ✅<br/>_(zaak open, document onvergrendeld of vergrendeld door gebruiker, document satus anders dan definitief)_      |             |           ✅<br/>_(afgehandelde zaak, status definitief)_           |           |
| downloaden                                           |                                                             ✅                                                             |             |                                                                    |           |
|                                                      |                                                                                                                           |             |                                                                    |           |
| **_Werklijst rechten_**                              |                                                                                                                           |             |                                                                    |           |
| inbox                                                |                                                             ✅                                                             |             |                                                                    |           |
| ontkoppelde_documenten_verwijderen                   |                                                                                                                           |             |                                 ✅                                  |           |
| inbox_productaanvragen_verwijderen                   |                                                                                                                           |             |                                 ✅                                  |           |
| zaken_taken                                          |                                                             ✅                                                             |             |                                                                    |           |
| zaken_taken_verdelen                                 |                                                                                                                           |      ✅      |                                                                    |           |
| zaken_taken_exporteren                               |                                                                                                                           |             |                                                                    |     ✅     |
|                                                      |                                                                                                                           |             |                                                                    |           |
| **_Overige rechten_**                                |                                                                                                                           |             |                                                                    |           |
| starten_zaak                                         |                                                             ✅                                                             |             |                                                                    |           |
| beheren                                              |                                                                                                                           |             |                                                                    |     ✅     |
| zoeken                                               |                                                             ✅                                                             |             |                                                                    |           |

Notes:
- **✅ (zaak open)**: the zaak status must be 'Open' for the user role to have this right.
The state 'zaak open' effectively means that the zaak is in the state 'Intake' or 'In behandeling' (and
not in the state 'Afgerond' or 'Heropend').
- **✅ (afhankelijk van CMMN status zaak)**: the action is dependent on the state of the CMMN zaak process.
E.g. in case of a closed zaak, the CMMN state of the zaak is such that no active task 'plan items' exist for the zaak
and therefore no task can be started.
- The policies listed above are backend policies. Whether the related functionality is available to the user in the
frontend (browser) is for a large part also determined by these policies but differences may apply.

## Technical implementation

The OPA policies are implemented using OPA policy files in OPA's native [Rego policy language](https://www.openpolicyagent.org/docs/latest/policy-language/) and can be found 
in the [ZAC OPA policies folder](../../src/main/resources/policies).

These policies are deployed by ZAC on startup to the OPA server, which is part of the ZAC ecosystem and needs to be 
available for ZAC. The OPA server provides a REST API with which ZAC integrates.

Then during normal operation the following happens:

1. When a user with a certain role X requests to perform a certain action on ZAC (e.g. create a new zaak), the ZAC 
backend will send a request to the OPA server to obtain the current access control rights ('rechten') for the resource 
in question (zaken in this case).
2. With these access control rights the ZAC backend then checks if role X is allowed to perform the requested action
(create a new zaak in this case). In some cases (see above) additional logic is also performed to check access control.
All this is done in the ZAC Java backend code, mostly in the REST Java classes.
3. If the requested action is allowed the user may perform the action. Should this however not be allowed an error 
message will be logged and returned to the user.

The ZAC frontend however will normally prevent the user from even trying to perform this requested action
(the frontend also uses these access control rights) so the scenario above can normally only happen if a user bypasses
the ZAC frontend and tries to perform the action directly on the ZAC backend.

## Updating OPA policies

When updating OPA policies in a Rego file please make sure to also update the corresponding policy matrix in 
this document.
