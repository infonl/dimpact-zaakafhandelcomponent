## ADDED Requirements

### Requirement: A changed mail template is used at once and a rolled-back one is never served

When a beheerder stores, changes or deletes a mail template, the system SHALL clear the cached configurations of all
zaaktypes at once, and SHALL clear them again after that transaction has completed, whether it commits or rolls back.
Clearing the zaaktype configuration cache by hand on the admin page SHALL clear it at once.

#### Scenario: A changed mail template is used at once
- **GIVEN** a zaak whose zaaktype configuration links a mail to a mail template, and whose configuration is cached
- **WHEN** a beheerder changes the onderwerp of that mail template
- **THEN** the next request for the mail template of that zaak returns the changed onderwerp

#### Scenario: A read during the save does not keep the previous mail template
- **GIVEN** a mail template change whose transaction has not committed yet
- **WHEN** another request reads the configuration of a zaaktype that links the template before the commit, and so
  caches the previous template
- **THEN** that cached configuration is cleared after the commit, and the next read returns the changed template

#### Scenario: A rolled-back mail template change is not served
- **GIVEN** a transaction that changes a mail template and then rolls back
- **WHEN** a configuration that links the template was read inside that transaction, and so cached with the
  uncommitted template
- **THEN** that cached configuration is cleared after the rollback, and the next read returns the committed template

#### Scenario: Clearing the cache by hand
- **WHEN** a beheerder clears the zaaktype configuration cache on the admin page
- **THEN** the cache is empty at once, and no second clear is scheduled for after a transaction
