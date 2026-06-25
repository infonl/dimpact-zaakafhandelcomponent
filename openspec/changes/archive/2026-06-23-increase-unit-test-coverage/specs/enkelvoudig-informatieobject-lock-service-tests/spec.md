## ADDED Requirements

### Requirement: EnkelvoudigInformatieObjectLockService creates a lock
The system SHALL persist an `EnkelvoudigInformatieObjectLock` and call the DRC client to lock the document when `createLock` is invoked.

#### Scenario: Lock created successfully
- **WHEN** `createLock` is called with a valid information object UUID and user ID
- **THEN** the DRC client is called to lock the document
- **THEN** the lock entity is persisted via the entity manager with the correct UUID, user ID, and lock value returned by the DRC client
- **THEN** the persisted lock is returned

### Requirement: EnkelvoudigInformatieObjectLockService finds an existing lock
The system SHALL return an existing lock when `findLock` is called with a UUID that has a lock record, and return null when no lock exists.

#### Scenario: Lock found
- **WHEN** `findLock` is called with a UUID for which a lock exists
- **THEN** the lock entity is returned

#### Scenario: No lock found
- **WHEN** `findLock` is called with a UUID for which no lock exists
- **THEN** null is returned

### Requirement: EnkelvoudigInformatieObjectLockService reads a lock or throws
The system SHALL return the lock when it exists, and throw `EnkelvoudigInformatieObjectLockNotFoundException` when it does not.

#### Scenario: Lock exists
- **WHEN** `readLock` is called with a UUID for which a lock exists
- **THEN** the lock entity is returned

#### Scenario: Lock not found
- **WHEN** `readLock` is called with a UUID for which no lock exists
- **THEN** `EnkelvoudigInformatieObjectLockNotFoundException` is thrown

### Requirement: EnkelvoudigInformatieObjectLockService deletes a lock
The system SHALL call the DRC client to unlock the document and remove the lock entity when `deleteLock` is called and a lock exists. When no lock exists, no action SHALL be taken.

#### Scenario: Lock deleted successfully
- **WHEN** `deleteLock` is called with a UUID for which a lock exists
- **THEN** the DRC client is called to unlock the document
- **THEN** the lock entity is removed via the entity manager

#### Scenario: No lock to delete
- **WHEN** `deleteLock` is called with a UUID for which no lock exists
- **THEN** neither the DRC client nor the entity manager remove method is called
