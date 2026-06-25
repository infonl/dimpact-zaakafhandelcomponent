## ADDED Requirements

### Requirement: SignaleringMailHelper resolves target mail for group
When the signalering target type is `GROUP`, the helper SHALL look up the group via `IdentityService` and return a `SignaleringTarget.Mail` with the group description and email address when an email is present, or null when the group has no email.

#### Scenario: Group has email
- **WHEN** `getTargetMail` is called with a signalering whose target type is `GROUP` and the group has an email address
- **THEN** a `SignaleringTarget.Mail` with the group description and email is returned

#### Scenario: Group has no email
- **WHEN** `getTargetMail` is called with a signalering whose target type is `GROUP` and the group has no email address
- **THEN** null is returned

### Requirement: SignaleringMailHelper resolves target mail for user
When the target type is `USER`, the helper SHALL look up the user via `IdentityService` and return a `SignaleringTarget.Mail` with the user's full name and email, or null when the user has no email.

#### Scenario: User has email
- **WHEN** `getTargetMail` is called with a signalering whose target type is `USER` and the user has an email address
- **THEN** a `SignaleringTarget.Mail` with the user's full name and email is returned

#### Scenario: User has no email
- **WHEN** `getTargetMail` is called with a signalering whose target type is `USER` and the user has no email
- **THEN** null is returned

### Requirement: SignaleringMailHelper returns null for unknown target type
When the target type is neither `GROUP` nor `USER`, null SHALL be returned.

#### Scenario: Unknown target type
- **WHEN** `getTargetMail` is called with a signalering whose target type is not `GROUP` or `USER`
- **THEN** null is returned

### Requirement: SignaleringMailHelper resolves the correct mail template for each signalering type
The helper SHALL map each `SignaleringType.Type` value to the correct `Mail` enum value and delegate to `MailTemplateService`.

#### Scenario: TAAK_OP_NAAM maps to SIGNALERING_TAAK_OP_NAAM
- **WHEN** `getMailTemplate` is called with signalering type `TAAK_OP_NAAM`
- **THEN** `MailTemplateService.readMailtemplate` is called with `Mail.SIGNALERING_TAAK_OP_NAAM`

#### Scenario: ZAAK_VERLOPEND with STREEFDATUM detail maps to SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM
- **WHEN** `getMailTemplate` is called with type `ZAAK_VERLOPEND` and detail `STREEFDATUM`
- **THEN** `MailTemplateService.readMailtemplate` is called with `Mail.SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM`

#### Scenario: ZAAK_VERLOPEND with FATALE_DATUM detail maps to SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM
- **WHEN** `getMailTemplate` is called with type `ZAAK_VERLOPEND` and detail `FATALE_DATUM`
- **THEN** `MailTemplateService.readMailtemplate` is called with `Mail.SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM`
