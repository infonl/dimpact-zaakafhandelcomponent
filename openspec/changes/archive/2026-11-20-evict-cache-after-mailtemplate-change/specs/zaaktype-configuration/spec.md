## ADDED Requirements

### Requirement: A changed mail template is used at once and a rolled-back one is not retained

When a beheerder changes a mail template, the ZAC instance that stores the change SHALL clear its cached
configuration of each zaaktype that links that template when the change is stored, and only those, at once. It SHALL
clear them again after that transaction has completed, whether it commits or rolls back, including a read that is
still loading at that moment.
Clearing the zaaktype configuration cache by hand on the admin page SHALL invalidate it at once.

#### Scenario: A changed mail template is used at once
- **GIVEN** a zaak whose zaaktype configuration links a mail to a mail template, and whose configuration is cached
- **WHEN** a beheerder changes the onderwerp of that mail template
- **THEN** the next request for the mail template of that zaak returns the changed onderwerp

#### Scenario: A read during the save does not keep the previous mail template
- **GIVEN** a mail template change whose transaction has not committed yet
- **WHEN** another request reads the configuration of a zaaktype that links the template before the commit, and so
  caches the previous template
- **THEN** that cached configuration is cleared after the commit, and the next read returns the changed template

#### Scenario: A rolled-back mail template change is not retained
- **GIVEN** a transaction that changes a mail template and then rolls back
- **WHEN** a configuration that links the template was read inside that transaction, and so cached with the
  uncommitted template
- **THEN** that cached configuration is cleared after the rollback, and the next read returns the committed template
- **AND** a request that read the configuration before the rollback MAY have received the uncommitted template

#### Scenario: Clearing the cache by hand
- **WHEN** a beheerder clears the zaaktype configuration cache on the admin page
- **THEN** every cached configuration is invalidated at once, and no second clear is scheduled for after a transaction
- **AND** a configuration that is still loading at that moment MAY be cached after the clear

#### Scenario: Deleting a mail template leaves the cache alone
- **GIVEN** a mail template that no zaaktype configuration links
- **WHEN** a beheerder deletes it
- **THEN** no cached zaaktype configuration is cleared, because no committed koppeling links that template
- **AND** another ZAC instance that cached the template before its last koppeling was removed MAY still return it
  until its cache entry expires or the cache is cleared by hand on it

#### Scenario: A koppeling stored during the change is not covered
- **GIVEN** a mail template change whose transaction has not committed yet
- **WHEN** another transaction links the template to another zaaktype at the same moment, by a beheerder or by the
  notification of a new zaaktype version
- **THEN** that zaaktype MAY keep the previous template until its cache entry expires or the cache is cleared by hand

#### Scenario: Another ZAC instance keeps its cache
- **GIVEN** more than one ZAC instance, each with the configuration of a zaaktype that links a mail template cached
- **WHEN** a beheerder changes that mail template on one instance
- **THEN** only that instance clears its cache; the others MAY return the previous template until their cache entry
  expires or the cache is cleared by hand on them
