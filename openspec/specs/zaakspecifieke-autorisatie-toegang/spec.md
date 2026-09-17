# zaakspecifieke-autorisatie-toegang Specification

## Purpose

OPA-enforced access restriction that denies every application role (`raadpleger`, `behandelaar`,
`coordinator`, `recordmanager`, `beheerder`) access to a zaakspecifiek geautoriseerde zaak (and its
taken/documenten) unless the user also holds the `zaakspecifiek_geautoriseerd` flag for that zaaktype.
The flag itself grants no rights and is inert without another application role.

## Requirements

### Requirement: zaakspecifiek_geautoriseerd is a flag, not a rights-bearing role

The `zaakspecifiek_geautoriseerd` application role SHALL NOT be granted any permission on its own in the
`zaak-rechten`, `taak-rechten`, or `document-rechten` OPA policies. Holding it alone, without also holding
another application role (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, or `beheerder`) for
the same zaaktype, SHALL result in no rights at all on any zaak, taak, or document of that zaaktype.

#### Scenario: The flag alone grants no rights on a zaakspecifiek geautoriseerde zaak
- **WHEN** a user holds only the `zaakspecifiek_geautoriseerd` application role (and none of `raadpleger`,
  `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) for a zaaktype
- **THEN** the `zaak-rechten` policy's `lezen`, `wijzigen`, `behandelen`, and `afbreken` permissions SHALL
  all evaluate to `false` for a zaak of that zaaktype whose `zaakspecifiekGeautoriseerd` input is `true`

#### Scenario: The flag alone grants no rights on a non-geautoriseerde zaak either
- **WHEN** a user holds only the `zaakspecifiek_geautoriseerd` application role for a zaaktype
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `false` for a zaak of that
  zaaktype whose `zaakspecifiekGeautoriseerd` input is `false`, identical to a user holding no application
  role at all for that zaaktype

### Requirement: Any application role combined with zaakspecifiek_geautoriseerd unlocks that role's rights on a zaakspecifiek geautoriseerde zaak

The `zaak-rechten` OPA policy SHALL, for a user who holds both `zaakspecifiek_geautoriseerd` and another
application role (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, or `beheerder`) for the same
zaaktype, grant that other role's full rights on a zaakspecifiek geautoriseerde zaak of that zaaktype,
identical to the rights that role already has on a non-geautoriseerde zaak of that zaaktype. This applies
uniformly to `recordmanager` and `beheerder` exactly as it does to `raadpleger`, `behandelaar`, and
`coordinator`.

#### Scenario: A behandelaar with the flag can read and treat a zaakspecifiek geautoriseerde zaak
- **WHEN** a user holds both the `behandelaar` and `zaakspecifiek_geautoriseerd` application roles for a
  zaaktype
- **THEN** the `zaak-rechten` policy's `lezen`, `wijzigen`, `behandelen`, and `afbreken` permissions SHALL
  evaluate to `true` for an open zaak of that zaaktype whose `zaakspecifiekGeautoriseerd` input is `true`,
  identical to what the same user would get for a non-geautoriseerde zaak of that zaaktype

#### Scenario: A raadpleger with the flag can read a zaakspecifiek geautoriseerde zaak
- **WHEN** a user holds both the `raadpleger` and `zaakspecifiek_geautoriseerd` application roles for a
  zaaktype
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `true` for a zaak of that
  zaaktype whose `zaakspecifiekGeautoriseerd` input is `true`

#### Scenario: A coordinator with the flag can change a zaakspecifiek geautoriseerde zaak
- **WHEN** a user holds both the `coordinator` and `zaakspecifiek_geautoriseerd` application roles for a
  zaaktype
- **THEN** the `zaak-rechten` policy's `wijzigen` permission SHALL evaluate to `true` for an open zaak of
  that zaaktype whose `zaakspecifiekGeautoriseerd` input is `true`

#### Scenario: A recordmanager with the flag can read a zaakspecifiek geautoriseerde zaak
- **WHEN** a user holds both the `recordmanager` and `zaakspecifiek_geautoriseerd` application roles for a
  zaaktype
- **THEN** the `zaak-rechten` policy's `lezen` and `heropenen` permissions SHALL evaluate to `true` for a
  zaak of that zaaktype whose `zaakspecifiekGeautoriseerd` input is `true`

#### Scenario: A beheerder with the flag can read a zaakspecifiek geautoriseerde zaak
- **WHEN** a user holds both the `beheerder` and `zaakspecifiek_geautoriseerd` application roles for a
  zaaktype
- **THEN** the `zaak-rechten` policy's `lezen` and `bekijken_zaakdata` permissions SHALL evaluate to `true`
  for a zaak of that zaaktype whose `zaakspecifiekGeautoriseerd` input is `true`

### Requirement: Any application role without the flag is denied on a zaakspecifiek geautoriseerde zaak

The `zaak-rechten` OPA policy SHALL deny every permission on a zaak whose `zaakspecifiekGeautoriseerd` input
is `true` to a user who holds any application role (`raadpleger`, `behandelaar`, `coordinator`,
`recordmanager`, or `beheerder`) but does not also hold `zaakspecifiek_geautoriseerd` for the same zaaktype
and is not the zaak's current behandelaar. No application role is exempt from this restriction:
`recordmanager` and `beheerder` reach a zaakspecifiek geautoriseerde zaak by being granted
`zaakspecifiek_geautoriseerd` for the zaaktype through the usual PABC configuration, not through a rule of
their own in the policy.

#### Scenario: A plain behandelaar cannot read a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `behandelaar` application role for a zaaktype (without
  `zaakspecifiek_geautoriseerd`) and who is not the zaak's behandelaar requests the `lezen` permission on a
  zaak of that zaaktype whose `zaakspecifiekGeautoriseerd` input is `true`
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A coordinator cannot change a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `coordinator` application role for a zaaktype (without
  `zaakspecifiek_geautoriseerd`) requests the `wijzigen` permission on an open zaak of that zaaktype whose
  `zaakspecifiekGeautoriseerd` input is `true`
- **THEN** the `zaak-rechten` policy's `wijzigen` permission SHALL evaluate to `false`

#### Scenario: A raadpleger cannot read a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `raadpleger` application role for a zaaktype (without
  `zaakspecifiek_geautoriseerd`) requests the `lezen` permission on a zaak of that zaaktype whose
  `zaakspecifiekGeautoriseerd` input is `true`
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A recordmanager cannot read a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `recordmanager` application role for a zaaktype (without
  `zaakspecifiek_geautoriseerd`) requests the `lezen` permission on a zaak of that zaaktype whose
  `zaakspecifiekGeautoriseerd` input is `true`
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A beheerder cannot read a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `beheerder` application role for a zaaktype (without
  `zaakspecifiek_geautoriseerd`) requests the `lezen` permission on a zaak of that zaaktype whose
  `zaakspecifiekGeautoriseerd` input is `true`
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A zaak that is not zaakspecifiek geautoriseerd is unaffected
- **WHEN** any user requests any permission on a zaak whose `zaakspecifiekGeautoriseerd` input is `false`
- **THEN** that permission SHALL evaluate exactly as it did before this capability existed, unaffected by
  whether the user holds `zaakspecifiek_geautoriseerd` or is the zaak's behandelaar

### Requirement: Access restriction and flag mechanism extend to taken and documenten of a zaakspecifiek geautoriseerde zaak

The `taak-rechten` and `document-rechten` OPA policies SHALL apply the same flag mechanism, the same current
behandelaar exception, and the same application-role access restriction as the `zaak-rechten` policy, based
on whether the taak's or document's associated zaak is zaakspecifiek geautoriseerd and on whether the
requesting user is that zaak's current behandelaar. This applies uniformly to every application role,
including `recordmanager` and `beheerder`.

#### Scenario: A plain behandelaar cannot read a taak of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `behandelaar` application role for a zaaktype, and who is not the
  associated zaak's behandelaar, requests the `lezen` permission on a taak whose associated zaak is of that
  zaaktype and whose `zaakspecifiekGeautoriseerd` input is `true`
- **THEN** the `taak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A behandelaar with the flag can read and treat a taak of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds both the `behandelaar` and `zaakspecifiek_geautoriseerd` application roles for
  a zaaktype requests the `lezen` or `wijzigen` permission on a taak whose associated zaak is of that
  zaaktype and whose `zaakspecifiekGeautoriseerd` input is `true`
- **THEN** both permissions SHALL evaluate to `true`

#### Scenario: A recordmanager without the flag cannot read a taak of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `recordmanager` application role for a zaaktype requests the `lezen`
  permission on a taak whose associated zaak is of that zaaktype and whose `zaakspecifiekGeautoriseerd`
  input is `true`
- **THEN** the `taak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A plain behandelaar cannot read a document of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `behandelaar` application role for a zaaktype, and who is not the
  associated zaak's behandelaar, requests the `lezen` permission on a document linked to a zaak of that
  zaaktype whose `zaakspecifiekGeautoriseerd` input is `true`
- **THEN** the `document-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A behandelaar with the flag can read and manage a document of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds both the `behandelaar` and `zaakspecifiek_geautoriseerd` application roles for
  a zaaktype requests the `lezen` or `downloaden` permission on a document linked to a zaak of that
  zaaktype whose `zaakspecifiekGeautoriseerd` input is `true`
- **THEN** both permissions SHALL evaluate to `true`

#### Scenario: A recordmanager without the flag cannot download a document of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `recordmanager` application role for a zaaktype requests the
  `downloaden` permission on a document linked to a zaak of that zaaktype whose `zaakspecifiekGeautoriseerd`
  input is `true`
- **THEN** the `document-rechten` policy's `downloaden` permission SHALL evaluate to `false`

#### Scenario: A document not linked to any zaak is unaffected
- **WHEN** any user requests any permission on a document that is not linked to any zaak
- **THEN** that permission SHALL evaluate exactly as it did before this capability existed, since such a
  document's `zaakspecifiekGeautoriseerd` input is `false`

### Requirement: Direct access to a zaakspecifiek geautoriseerde zaak an employee may not see fails with the generic insufficient-rights message

When a user without the `zaakspecifiek_geautoriseerd` application role requests a zaak, taak, or document
that is denied per the requirements above, the system SHALL respond with the same generic
insufficient-rights error the system already returns for any other policy denial, and SHALL NOT reveal that
the underlying reason is that the resource is zaakspecifiek geautoriseerd.

#### Scenario: Direct URL access to a zaakspecifiek geautoriseerde zaak by an unauthorised employee
- **WHEN** an employee who holds an application role for the zaaktype but not
  `zaakspecifiek_geautoriseerd` requests a zaakspecifiek geautoriseerde zaak directly (for example via a
  bookmarked URL)
- **THEN** the request is rejected with the same HTTP 403 / generic "insufficient rights" response the
  system already returns for any other policy denial, with no indication that the zaak is zaakspecifiek
  geautoriseerd

### Requirement: Rechten computed for werklijst and zoekresultaat rows respect zaakspecifieke autorisatie

Rechten computed from a `ZaakZoekObject`, `TaakZoekObject`, or `DocumentZoekObject` SHALL reflect whether
the underlying zaak is zaakspecifiek geautoriseerd, applying the same flag+role rule already enforced for
the equivalent single-resource (`Zaak`/`TaskInfo`/`EnkelvoudigInformatieObject`) rechten in this capability.

#### Scenario: A zaakspecifiek geautoriseerde zaak's rechten in a werklijst match its detail-view rechten
- **WHEN** rechten are computed for a `ZaakZoekObject` representing a zaakspecifiek geautoriseerde zaak, for
  a user who holds a given combination of application roles (with or without `zaakspecifiek_geautoriseerd`)
  for that zaak's zaaktype
- **THEN** the computed rechten are identical to the rechten that would be computed for the same zaak's
  detail view for the same user

#### Scenario: A zaakspecifiek geautoriseerde taak's rechten in a werklijst match its detail-view rechten
- **WHEN** rechten are computed for a `TaakZoekObject` whose associated zaak is zaakspecifiek geautoriseerd,
  for a user who holds a given combination of application roles for that zaaktype
- **THEN** the computed rechten are identical to the rechten that would be computed for the same taak's
  detail view for the same user

#### Scenario: A zaakspecifiek geautoriseerde document's rechten in a zoekresultaat match its detail-view rechten
- **WHEN** rechten are computed for a `DocumentZoekObject` whose associated zaak is zaakspecifiek
  geautoriseerd, for a user who holds a given combination of application roles for that zaaktype
- **THEN** the computed rechten are identical to the rechten that would be computed for the same document's
  detail view for the same user

#### Scenario: A non-geautoriseerde zaak's werklijst rechten are unaffected
- **WHEN** rechten are computed for a `ZaakZoekObject`, `TaakZoekObject`, or `DocumentZoekObject` whose
  underlying zaak is not zaakspecifiek geautoriseerd
- **THEN** the computed rechten are unaffected by this requirement, identical to what they were before this
  capability existed

### Requirement: The PABC application role string matches the OPA role string

The PABC application role that ZAC's OPA policies check for SHALL be named `zaakspecifiek_geautoriseerd`,
matching the role string PABC hands out to a logged-in user for a zaaktype (or, for a globally-scoped
functional role such as `beheerder_elk_domein`, for every zaaktype) in a domain where this role is mapped.

#### Scenario: PABC seed data grants the renamed role
- **WHEN** a user's functional role is mapped, via PABC, to the zaakspecifiek-geautoriseerd application
  role for a zaaktype
- **THEN** the application role name returned for that zaaktype is exactly `zaakspecifiek_geautoriseerd`

### Requirement: Documentation explains the flag mechanism without a permission-matrix column

`docs/solution-architecture/accessControlPolicies.md` SHALL list `zaakspecifiek_geautoriseerd` in the
application roles table with a description stating that it is a flag combined with another application
role, and SHALL NOT give it its own column with checkmarks in the permission matrix, since it grants no
permission on its own. The same documentation SHALL describe the current-behandelaar exception: that the
employee who is a zaakspecifiek geautoriseerde zaak's behandelaar may exercise their own application role on
that zaak without holding the flag.

#### Scenario: Role table lists the flag with an accurate description
- **WHEN** the application roles table in `accessControlPolicies.md` is read
- **THEN** it SHALL list `zaakspecifiek_geautoriseerd` and describe it as extending another application
  role's rights to zaakspecifiek geautoriseerde zaken, not as granting rights by itself

#### Scenario: Permission matrix has no zaakspecifiek_geautoriseerd column
- **WHEN** the permission matrix table in `accessControlPolicies.md` is read
- **THEN** it SHALL NOT contain a `zaakspecifiek_geautoriseerd` column, and a note near the table SHALL
  explain that this role's effect is documented separately because it is a flag, not a directly-granted role

#### Scenario: The behandelaar exception is documented
- **WHEN** the section of `accessControlPolicies.md` covering zaakspecifieke autorisatie is read
- **THEN** it SHALL state that a zaakspecifiek geautoriseerde zaak's current behandelaar keeps their own
  application role's rights on that zaak, its taken and its documenten, without holding
  `zaakspecifiek_geautoriseerd`

### Requirement: The current behandelaar keeps their application role's rights on their own zaakspecifiek geautoriseerde zaak

The `zaak-rechten`, `taak-rechten`, and `document-rechten` OPA policies SHALL grant the employee who is the
zaak's current behandelaar the full rights of whichever other application role (`raadpleger`, `behandelaar`,
`coordinator`, `recordmanager`, or `beheerder`) that employee holds for the zaaktype, on that zaak and on
its taken and documenten, even when the employee does not hold `zaakspecifiek_geautoriseerd` for the
zaaktype. This exception SHALL apply only to the zaak whose behandelaar the employee currently is; it SHALL
NOT extend to any other zaakspecifiek geautoriseerde zaak of the same zaaktype. Like
`zaakspecifiek_geautoriseerd`, being the behandelaar SHALL grant no rights of its own: an employee who holds
no application role for the zaaktype gets nothing from it.

#### Scenario: The behandelaar without the flag can read and treat their own marked zaak
- **WHEN** a user who holds the `behandelaar` application role for a zaaktype but not
  `zaakspecifiek_geautoriseerd`, and who is the current behandelaar of a zaak of that zaaktype whose
  `zaakspecifiekGeautoriseerd` input is `true`, requests permissions on that zaak
- **THEN** the `zaak-rechten` policy's `lezen`, `wijzigen`, `behandelen`, and `afbreken` permissions SHALL
  all evaluate to `true`, identical to what the same user would get for a non-geautoriseerde zaak of that
  zaaktype

#### Scenario: The behandelaar without the flag can read the taken and documenten of their own marked zaak
- **WHEN** a user who holds the `behandelaar` application role for a zaaktype but not
  `zaakspecifiek_geautoriseerd`, and who is the current behandelaar of a zaak of that zaaktype whose
  `zaakspecifiekGeautoriseerd` input is `true`, requests the `lezen` permission on a taak of that zaak and
  on a document linked to that zaak
- **THEN** the `taak-rechten` and `document-rechten` policies' `lezen` permissions SHALL both evaluate to
  `true`

#### Scenario: The exception does not extend to another employee's marked zaak
- **WHEN** a user who holds the `behandelaar` application role for a zaaktype but not
  `zaakspecifiek_geautoriseerd`, and who is the behandelaar of one zaak of that zaaktype, requests the
  `lezen` permission on a *different* zaakspecifiek geautoriseerde zaak of that zaaktype of which they are
  not the behandelaar
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: Being the behandelaar without any application role grants nothing
- **WHEN** a user who holds no application role at all for a zaaktype is nevertheless recorded as the
  behandelaar of a zaakspecifiek geautoriseerde zaak of that zaaktype
- **THEN** every permission in the `zaak-rechten` policy SHALL evaluate to `false` for that zaak

### Requirement: Recordmanagers and beheerders reach zaakspecifiek geautoriseerde zaken through PABC configuration

The recordmanager and beheerder functional roles SHALL be mapped, in the PABC configuration of the Docker
Compose stack and the INFO test environment, to the `zaakspecifiek_geautoriseerd` application role, so that
employees holding those roles can exercise them on zaakspecifiek geautoriseerde zaken. This access SHALL come
from that configuration alone; the system SHALL NOT grant `recordmanager` or `beheerder` any access to a
zaakspecifiek geautoriseerde zaak that it does not equally grant to any other application role holding the
same flag.

#### Scenario: A recordmanager with the mapping reaches a zaakspecifiek geautoriseerde zaak
- **WHEN** a recordmanager whose functional role is mapped to `zaakspecifiek_geautoriseerd` for a zaaktype
  opens a zaakspecifiek geautoriseerde zaak of that zaaktype, its taken and its documenten
- **THEN** access is granted, and the same holds for that zaak in werklijsten and zoekresultaten

#### Scenario: A recordmanager without the mapping is refused
- **WHEN** a recordmanager whose functional role is not mapped to `zaakspecifiek_geautoriseerd` for a
  zaaktype requests a zaakspecifiek geautoriseerde zaak of that zaaktype
- **THEN** the request is refused with the generic insufficient-rights response, demonstrating that the
  mapping, and not a rule specific to the recordmanager role, is what grants the access

#### Scenario: A recordmanager can recover a zaak whose behandelaar was removed outside ZAC
- **WHEN** a zaakspecifiek geautoriseerde zaak has its behandelaar removed directly in the zaakregister, and
  a recordmanager whose functional role is mapped to `zaakspecifiek_geautoriseerd` for that zaaktype opens it
- **THEN** the recordmanager can open the zaak and assign a new behandelaar, after which that new behandelaar
  reaches the zaak through the current-behandelaar exception without holding the flag
