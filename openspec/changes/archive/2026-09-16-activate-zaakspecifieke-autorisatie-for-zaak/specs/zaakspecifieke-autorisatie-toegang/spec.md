## ADDED Requirements

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

## MODIFIED Requirements

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
