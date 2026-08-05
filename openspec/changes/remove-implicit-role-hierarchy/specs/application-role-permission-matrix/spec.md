## ADDED Requirements

### Requirement: Every application role's permissions are granted explicitly, without relying on inherited roles

The OPA policies (`zaak-rechten.rego`, `taak-rechten.rego`, `document-rechten.rego`, `werklijst-rechten.rego`, `notitie-rechten.rego`, `overige-rechten.rego`) SHALL grant each permission directly to every application role (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) that is allowed to perform it, without depending on a user also carrying any "lower" application role in the hierarchy `raadpleger < behandelaar < coordinator < recordmanager < beheerder`.

#### Scenario: A user with only the coordinator role, and no raadpleger role, can read a zaak
- **WHEN** a user has only the `coordinator` application role (and not `raadpleger`, `behandelaar`, `recordmanager`, or `beheerder`) in `user.rollen`
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `true` for a zaak of an allowed zaaktype

#### Scenario: A user with only the recordmanager role, and no behandelaar role, can read a taak
- **WHEN** a user has only the `recordmanager` application role in `user.rollen`
- **THEN** the `taak-rechten` policy's `lezen` permission SHALL evaluate to `true` for a taak of an allowed zaaktype

#### Scenario: A user with only the beheerder role, and no recordmanager role, can change an already-completed zaak
- **WHEN** a user has only the `beheerder` application role in `user.rollen`
- **THEN** the `zaak-rechten` policy's `wijzigen` permission SHALL evaluate to `true` for a zaak of an allowed zaaktype, regardless of whether the zaak is open

#### Scenario: A user with only the beheerder role, and no behandelaar/coordinator role, can change an open zaak
- **WHEN** a user has only the `beheerder` application role in `user.rollen`
- **THEN** the `zaak-rechten` policy's `wijzigen` permission SHALL evaluate to `true` for an open zaak of an allowed zaaktype

#### Scenario: A user with only the raadpleger role cannot change a zaak
- **WHEN** a user has only the `raadpleger` application role in `user.rollen`
- **THEN** the `zaak-rechten` policy's `wijzigen` permission SHALL evaluate to `false`

#### Scenario: No net change to any role's effective permissions
- **WHEN** any application role's effective set of allowed permissions is compared before and after this change, assuming the same user previously held every role in its inherited chain
- **THEN** the effective set of allowed permissions SHALL be identical

### Requirement: The permission matrix documentation lists every explicit grant with no inheritance narrative

`docs/solution-architecture/accessControlPolicies.md` SHALL NOT describe application roles as requiring any other "lower-level" role, and its permission table SHALL mark every role that has a direct, explicit grant for a permission (per the corresponding `.rego` policy) with a checkmark in that role's column.

#### Scenario: Documentation contains no hierarchy narrative
- **WHEN** `accessControlPolicies.md` is read
- **THEN** it SHALL NOT contain text stating that one application role requires another application role in order to function

#### Scenario: Permission table matches policy grants
- **WHEN** a permission row in the table has a checkmark for role `R`
- **THEN** the corresponding `.rego` policy file SHALL contain an explicit clause granting that permission to role `R`, not solely to a role `R` would previously have inherited from
