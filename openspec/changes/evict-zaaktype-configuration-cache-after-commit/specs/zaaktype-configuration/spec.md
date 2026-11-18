## ADDED Requirements

### Requirement: A stored configuration change is visible at once and a rolled-back one is never served

The system SHALL clear the cached configuration of a zaaktype version, and the cached list of deadline warning
windows, only after the transaction that changes the configuration has completed, whether it commits or rolls
back. Without a transaction, the system SHALL clear them at once. This SHALL apply to every change of the
configuration: storing it on the admin screen, storing it on the notification of a zaaktype version, and storing or
deleting a mailtemplate koppeling. Clearing the caches by hand on the admin page SHALL take effect at once.

#### Scenario: A stored configuration is used at once
- **GIVEN** a zaaktype version whose configuration is in the cache
- **WHEN** a beheerder stores a changed configuration for it on the admin screen
- **THEN** the next read of its configuration returns the stored change, without waiting for the cache to expire

#### Scenario: A read during the save does not keep the previous configuration
- **GIVEN** a zaaktype version whose configuration is being stored, and whose transaction has not committed yet
- **WHEN** another request reads its configuration before the commit and so caches the previous configuration
- **THEN** that cached configuration is cleared after the commit, and the next read returns the stored change

#### Scenario: A rolled-back change is not served
- **GIVEN** a transaction that stores a configuration and then rolls back
- **WHEN** the configuration was read inside that transaction, and so cached with the uncommitted change
- **THEN** that cached configuration is cleared after the rollback, and the next read returns the committed
  configuration

#### Scenario: A new zaaktype version gets its configuration at once
- **WHEN** ZAC stores the configuration of a new zaaktype version on its notification, and copies its SmartDocuments
  template mappings in the same transaction
- **THEN** the cache of that zaaktype version is cleared after the transaction completes, not before the mappings
  are copied

#### Scenario: A removed mailtemplate koppeling is not used
- **GIVEN** a zaak whose zaaktype configuration links a mail to a mail template of its own, and whose configuration
  is in the cache
- **WHEN** a beheerder removes that koppeling, by storing the configuration or by deleting the koppeling
- **THEN** the next request for the mail template of that zaak proposes the default mail template

#### Scenario: Clearing the cache by hand
- **WHEN** a beheerder clears the zaaktype configuration caches on the admin page
- **THEN** the caches are empty at once
