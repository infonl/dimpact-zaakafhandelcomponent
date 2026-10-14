## ADDED Requirements

### Requirement: An employee with edit rights can ontkoppel a medewerker from a zaakspecifiek geautoriseerde zaak

An employee with the `wijzigen` right on a zaakspecifiek geautoriseerde zaak SHALL be able to ontkoppel a
medewerker through `DELETE /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/{medewerkerId}`,
giving a required reden. ZAC SHALL then delete every *Zaakspecifiek geautoriseerde medewerker* rol of that
medewerker from the zaak in Open Zaak, with the reden as audit toelichting, before the request returns.

#### Scenario: Ontkoppelen a manually added medewerker
- **GIVEN** a zaakspecifiek geautoriseerde zaak and an employee with `wijzigen` on it
- **AND** medewerker A, who holds a *Zaakspecifiek geautoriseerde medewerker* rol on the zaak
- **WHEN** the employee ontkoppelt A with reden "Verkeerde medewerker"
- **THEN** the zaak has no *Zaakspecifiek geautoriseerde medewerker* rol for A
- **AND** the Open Zaak audittrail records the deletion with toelichting "Verkeerde medewerker"

#### Scenario: Ontkoppelen a previous zaak- or taakbehandelaar
- **GIVEN** medewerker P, a previous zaak- or taakbehandelaar who holds a *Zaakspecifiek geautoriseerde
  medewerker* rol on the zaak
- **WHEN** an employee with `wijzigen` ontkoppelt P
- **THEN** the rol of P is deleted

#### Scenario: Ontkoppelen without edit rights
- **WHEN** an employee without `wijzigen` on the zaak ontkoppelt a medewerker
- **THEN** the request is refused with the generic insufficient-rights response

#### Scenario: Ontkoppelen from a zaak that is not zaakspecifiek geautoriseerd
- **WHEN** an employee ontkoppelt a medewerker from a zaak that is not zaakspecifiek geautoriseerd
- **THEN** the request fails with `ERROR_CODE_ZAAK_NOT_ZAAKSPECIFIEK_GEAUTORISEERD`

#### Scenario: Ontkoppelen a medewerker without the rol
- **WHEN** an employee ontkoppelt a medewerker who holds no *Zaakspecifiek geautoriseerde medewerker* rol on
  the zaak
- **THEN** the request fails with `ERROR_CODE_MEDEWERKER_NOT_ZAAKSPECIFIEK_GEAUTORISEERD`

### Requirement: The zaakbehandelaar and the behandelaars of open taken cannot be ontkoppeld

ZAC SHALL refuse to ontkoppel the current zaakbehandelaar or the behandelaar of an open taak of the zaak with
`ERROR_CODE_MEDEWERKER_CANNOT_BE_ONTKOPPELD`, and SHALL then delete no rol. The list of medewerkers with access
SHALL mark a medewerker as `isOntkoppelbaar` only when this refusal does not apply to them.
`isOntkoppelbaar` says nothing about the rights of the employee who asks: the zaak view combines it with
`wijzigen`, and the endpoint checks `wijzigen` itself.

#### Scenario: Ontkoppelen the zaakbehandelaar
- **WHEN** an employee ontkoppelt the zaakbehandelaar
- **THEN** the request fails with `ERROR_CODE_MEDEWERKER_CANNOT_BE_ONTKOPPELD`

#### Scenario: Ontkoppelen the behandelaar of an open taak
- **GIVEN** medewerker T, who holds a *Zaakspecifiek geautoriseerde medewerker* rol and is the behandelaar of
  an open taak of the zaak
- **WHEN** an employee ontkoppelt T
- **THEN** the request fails with `ERROR_CODE_MEDEWERKER_CANNOT_BE_ONTKOPPELD`
- **AND** T keeps the rol

#### Scenario: A medewerker with two rollen is listed once
- **GIVEN** medewerker A, who holds two *Zaakspecifiek geautoriseerde medewerker* rollen on the zaak
- **WHEN** an employee lists the medewerkers with access to the zaak
- **THEN** A is listed once
- **AND** ontkoppelen A deletes both rollen

#### Scenario: Ontkoppelen yourself
- **GIVEN** employee E, who holds a *Zaakspecifiek geautoriseerde medewerker* rol on the zaak and `wijzigen` on it
- **WHEN** E ontkoppelt themselves in the *Medewerkers* tab
- **THEN** the rol of E is deleted
- **AND** the zaak view goes to the dashboard instead of refreshing the zaak E may no longer see

#### Scenario: Which medewerkers are ontkoppelbaar
- **GIVEN** zaakbehandelaar Z, taakbehandelaar T of an open taak, and medewerker A who was added by hand
- **WHEN** an employee lists the medewerkers with access to the zaak
- **THEN** only A is `isOntkoppelbaar`

### Requirement: An ontkoppelde medewerker loses access

An ontkoppelde medewerker SHALL lose the access that their *Zaakspecifiek geautoriseerde medewerker* rol gave.
Unless they hold the `zaakspecifiek_geautoriseerd` application role for the zaaktype, together with a role that
grants rechten on it, they SHALL no longer get
any rechten on the zaak and SHALL no longer find the zaak, its taken and its documenten in the werkvoorraden,
the zoekresultaten and when koppelen zaken, without a manual reindex. They SHALL no longer be listed in the
*Medewerkers* tab.

Ontkoppelen SHALL NOT change access that comes from the `zaakspecifiek_geautoriseerd` application role. That
role grants no rechten on its own; together with a role that does, such as `behandelaar`, it gives access to
every zaakspecifiek geautoriseerde zaak of the zaaktype. The PABC grants each role for the zaaktype or for all
zaaktypen, and ZAC combines both, so a medewerker who holds them either way keeps that access after
ontkoppelen.

#### Scenario: Ontkoppelen a medewerker who also holds the zaakspecifiek_geautoriseerd application role
- **GIVEN** medewerker B, who holds a *Zaakspecifiek geautoriseerde medewerker* rol on the zaak, and the
  `behandelaar` and `zaakspecifiek_geautoriseerd` application roles for the zaaktype
- **WHEN** an employee ontkoppelt B
- **THEN** the rol of B is deleted
- **AND** B keeps access to the zaak through those application roles

#### Scenario: The ontkoppelde medewerker no longer finds the zaak
- **GIVEN** medewerker A, with `behandelaar` but not `zaakspecifiek_geautoriseerd`, who was ontkoppeld from
  the zaak
- **WHEN** A searches for the zaak or requests it
- **THEN** A does not find the zaak and is refused access to it

### Requirement: The ontkoppeling shows in the zaakhistorie

The zaakhistorie SHALL show the deleted rol as ontkoppeld, with the label *Zaakspecifiek geautoriseerde
medewerker*, the medewerker's name and the reden.

#### Scenario: Viewing the zaakhistorie after ontkoppelen
- **GIVEN** medewerker A was ontkoppeld with reden "Verkeerde medewerker"
- **WHEN** an employee opens the zaakhistorie
- **THEN** it shows an ontkoppeld *Zaakspecifiek geautoriseerde medewerker* line with A's name and the reden

### Requirement: The Medewerkers tab offers "Medewerker ontkoppelen"

The *Medewerkers* tab SHALL show a *Medewerker ontkoppelen* action on every medewerker that is
`isOntkoppelbaar`, only when the employee has `wijzigen` on the zaak. It SHALL open a dialog that asks for a
required reden. After a successful ontkoppeling the dialog SHALL close, show a confirmation and refresh the
list.

#### Scenario: The action without edit rights
- **WHEN** an employee without `wijzigen` opens the *Medewerkers* tab
- **THEN** no medewerker has a *Medewerker ontkoppelen* action

#### Scenario: Ontkoppelen from the tab
- **GIVEN** the *Medewerkers* tab with ontkoppelbare medewerker A
- **WHEN** the employee chooses *Medewerker ontkoppelen* on A and confirms with a reden
- **THEN** A is ontkoppeld and no longer listed
