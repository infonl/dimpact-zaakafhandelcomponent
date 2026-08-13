## MODIFIED Requirements

### Requirement: The permission matrix documentation lists every explicit grant with no inheritance narrative

`docs/solution-architecture/accessControlPolicies.md` and `docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md` SHALL NOT describe application roles as requiring, or automatically also having the rights of, any other application role, and `accessControlPolicies.md`'s permission table SHALL mark every role that has a direct, explicit grant for a permission (per the corresponding `.rego` policy) with a checkmark in that role's column.

#### Scenario: Documentation contains no hierarchy narrative
- **WHEN** `accessControlPolicies.md` is read
- **THEN** it SHALL NOT contain text stating that one application role requires another application role in order to function

#### Scenario: Permission table matches policy grants
- **WHEN** a permission row in the table has a checkmark for role `R`
- **THEN** the corresponding `.rego` policy file SHALL contain an explicit clause granting that permission to role `R`, not solely to a role `R` would previously have inherited from

#### Scenario: User manual contains no additive/inherited rights narrative
- **WHEN** the "Rollen en rechten" section of `ZAC-gebruikershandleiding.md` is read
- **THEN** it SHALL NOT describe any role's rights as "aanvullend" (additional) to, or dependent on, another role's rights, and it SHALL describe each of the 5 application roles (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) with its own directly-granted rights

#### Scenario: User manual role list is complete
- **WHEN** the "Rollen en rechten" section of `ZAC-gebruikershandleiding.md` is read
- **THEN** it SHALL list all 5 application roles that the OPA policies grant zaak/taak/document/notitie permissions to, including `beheerder`
