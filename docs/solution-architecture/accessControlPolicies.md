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

The following OPA access control policies are enforced in the ZAC backend for the roles listed above for the
various resources on which a user can perform actions:

| Rechten                                                                          |                                 Behandelaar                                 | Coördinator | Recordmanager | Beheerder | Technische implementatie                                                           |
|:---------------------------------------------------------------------------------|:---------------------------------------------------------------------------:|:-----------:|:-------------:|:---------:|:-----------------------------------------------------------------------------------|
|                                                                                  |                                                                             |             |               |           |                                                                                    |
| **_Zaak rechten (zie ook: Overige rechten)_**                                    |                                                                             |             |               |           |                                                                                    |
| lezen                                                                            |                                      X                                      |      X      |       X       |           | OPA policy                                                                         |
| wijzigen                                                                         |                            X (status: zaak open)                            |             |       X       |           | OPA policy                                                                         |
| toekennen                                                                        |                                      X                                      |      X      |       X       |           | OPA policy                                                                         |
| behandelen                                                                       |                                      X                                      |             |               |           | OPA policy                                                                         |
| afbreken                                                                         |                                      X                                      |             |       X       |           | OPA policy                                                                         |
| heropenen                                                                        |                                                                             |             |       X       |           | OPA policy                                                                         |
| wijzigenZaakdata                                                                 |                                                                             |             |       X       |           | OPA policy                                                                         |
| wijzigenDoorlooptijd                                                             |                                      X                                      |             |       X       |           | OPA policy                                                                         |
| verlengen                                                                        |            X (status: zaak open, niet heropend, niet opgeschort)            |             |               |           | Logica in ZakenRESTService.java. Afgeleid van OPA policy `behandelen`.             |
| opschorten                                                                       |            X (status: zaak open, niet heropend, niet opgeschort)            |             |               |           | Logica in ZakenRESTService.java. Afgeleid van OPA policy `behandelen`.             |
| hervatten                                                                        |                                      X                                      |             |               |           | Logica in ZakenRESTService.java. Afgeleid van OPA policy `behandelen`.             |
| toevoegen_document                                                               |                            X (status: zaak open)                            |             |       X       |           | Logica in InformatieObjectenRESTService.java. Afgeleid van OPA policy: `wijzigen`. |
| creeeren_document                                                                |                            X (status: zaak open)                            |             |       X       |           | Logica in InformatieObjectenRESTService.java. Afgeleid van OPA policy: `wijzigen`. |
| koppelen                                                                         |                        X (status: beide zaken open)                         |             |       X       |           | OPA policy `zaak wijzigen` (voor beide zaken)                                      |
| versturen_email                                                                  |                                      X                                      |             |               |           | OPA policy `zaak behandelen`                                                       |
| versturen_ontvangstbevestiging                                                   |                                      X                                      |             |               |           | OPA policy `zaak behandelen`                                                       |
| toevoegen_initiator_persoon                                                      |                                      X                                      |             |               |           | OPA policy `zaak behandelen`                                                       |
| toevoegen_initiator_bedrijf                                                      |                                      X                                      |             |               |           | OPA policy `zaak behandelen`                                                       |
| verwijderen_initiator                                                            |                                      X                                      |             |               |           | OPA policy `zaak behandelen`                                                       |
| toevoegen_betrokkene_persoon                                                     |                                      X                                      |             |               |           | OPA policy `zaak behandelen`                                                       |
| toevoegen_betrokkene_bedrijf                                                     |                                      X                                      |             |               |           | OPA policy `zaak behandelen`                                                       |
| verwijderen_betrokkene                                                           |                                      X                                      |             |               |           | OPA policy `zaak behandelen`                                                       |
| toevoegen_bag_object                                                             |                                      X                                      |             |               |           | OPA policy `zaak behandelen`                                                       |
| starten_taak                                                                     |                    X (afhankelijk van CMMN status zaak)                     |             |               |           | OPA policy `zaak behandelen`                                                       |
| vastleggen_besluit                                                               |     X (status: zaak open, niet in intake, zaaktype heeft besluittypen)      |             |               |           | Logica in ZakenRESTService.java. Afgeleid van OPA policy `behandelen`.             |
| verlengen_doorlooptijd                                                           |            X (status: zaak open, niet heropend, niet opgeschort)            |             |               |           | Logica in ZakenRESTService.java. Afgeleid van OPA policy `behandelen`.             |
|                                                                                  |                                                                             |             |               |           |                                                                                    |
| **_Taak rechten_**                                                               |                                                                             |             |               |           |                                                                                    |
| lezen                                                                            |                                      X                                      |             |       X       |           |                                                                                    |
| wijzigen                                                                         |                                      X                                      |             |       X       |           |                                                                                    |
| toekennen                                                                        |                                      X                                      |             |               |           |                                                                                    |
| _TODO: de rechten hieronder zijn geen OPA rechten en moeten nog bekeken worden._ |                                                                             |             |               |           |                                                                                    |
| wijzigen_toekenning                                                              |                                      X                                      |             |               |           |                                                                                    |
| wijzigen_formulier                                                               |                                      X                                      |             |       X       |           |                                                                                    |
| creeeren_document                                                                |                                      X                                      |             |       X       |           |                                                                                    |
| toevoegen_document                                                               |                                      X                                      |             |       X       |           |                                                                                    |
|                                                                                  |                                                                             |             |               |           |                                                                                    |
| **_Document rechten_**                                                           |                                                                             |             |               |           |                                                                                    |
| lezen                                                                            |                                      X                                      |      X      |       X       |           |                                                                                    |
| wijzigen                                                                         | X (status: zaak open, document onvergrendeld of vergrendeld door gebruiker) |             |       X       |           |                                                                                    |
| verwijderen                                                                      |                                      X                                      |             |       X       |           |                                                                                    |
| vergrendelen                                                                     |                            X (status: zaak open)                            |             |       X       |           |                                                                                    |
| ontgrendelen                                                                     |                   X (document vergrendeld door gebruiker)                   |             |       X       |           |                                                                                    |
| ondertekenen                                                                     | X (status: zaak open, document onvergrendeld of vergrendeld door gebruiker) |             |               |           |                                                                                    |
| toevoegen_nieuwe_versie                                                          | X (status: zaak open, document onvergrendeld of vergrendeld door gebruiker) |             |       X       |           | OPA policy `document wijzigen`                                                     |
| verplaatsen (koppelen)                                                           | X (status: zaak open, document onvergrendeld of vergrendeld door gebruiker) |             |       X       |           | OPA policy `document wijzigen` en `zaak wijzigen`                                  |
| ontkoppelen                                                                      | X (status: zaak open, document onvergrendeld of vergrendeld door gebruiker) |             |       X       |           | OPA policy `document wijzigen`                                                     |
| downloaden                                                                       |                                      X                                      |      X      |       X       |           | OPA policy `document lezen`                                                        |
|                                                                                  |                                                                             |             |               |           |                                                                                    |
| **_Werklijst rechten_**                                                          |                                                                             |             |               |           |                                                                                    |
| inbox                                                                            |                                      X                                      |             |       X       |           |                                                                                    |
| ontkoppelde_documenten_verwijderen                                               |                                                                             |             |       X       |           |                                                                                    |
| inbox_productaanvragen_verwijderen                                               |                                                                             |             |       X       |           |                                                                                    |
| zaken_taken                                                                      |                                      X                                      |      X      |       X       |           |                                                                                    |
| zaken_taken_verdelen                                                             |                                                                             |      X      |               |           |                                                                                    |
| documenten_ontkoppeld                                                            |                                      X                                      |             |       X       |           | OPA policy `inbox`                                                                 |
|                                                                                  |                                                                             |             |               |           |                                                                                    |
| **_Overige rechten_**                                                            |                                                                             |             |               |           |                                                                                    |
| starten_zaak                                                                     |                                      X                                      |             |               |           |                                                                                    |
| beheren                                                                          |                                                                             |             |               |     X     |                                                                                    |
| zoeken                                                                           |                                      X                                      |      X      |       X       |           |                                                                                    |

Notes:
- `X (status: zaak open)` means that the zaak status must be 'Open' for the user role to have this right.
The state 'zaak open' effectively means that the zaak is in the state 'Intake' or 'In behandeling' (and
not in the state 'Afgerond' or 'Heropend').
- `X (afhankelijk van CMMN status zaak)` means that the action is dependent on the state of the CMMN zaak process.
E.g. in case of a closed zaak, the CMMN state of the zaak is such that no active task 'plan items' exist for the zaak
and therefore no task can be started.
- The policies listed above are backend policies. Whether the related functionality is available to the user in the
frontend (browser) is for a large part also determined by these policies but differences may apply.

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
2. With these access control rights the ZAC backend then checks if role X is allowed to perform the requested action
(create a new zaak in this case). In some cases (see above) additional logic is also performed to check access control.
All this is done in the ZAC Java backend code, mostly in the REST Java classes.
3. If the requested action is allowed the user may perform the action.
Should this however not be allowed an error message will be logged and returned to the user.

The ZAC frontend however will normally prevent the user from even trying to perform this requested action
(the frontend also uses these access control rights) so the scenario above can normally
only happen if a user bypasses the ZAC frontend and tries to perform the action directly on the ZAC backend.

## Updating OPA policies

When updating OPA policies in a Rego file please make sure to also update the corresponding policy
matrix in this document.
