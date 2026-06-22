## ADDED Requirements

### Requirement: UserTaskCompletionListener removes the task from the index on TASK_COMPLETED
When `onEvent` is called with a `TASK_COMPLETED` event, the listener SHALL call `IndexingService.removeTaak` with the task ID via `FlowableHelper`.

#### Scenario: TASK_COMPLETED event triggers index removal
- **WHEN** `onEvent` is called with a `FlowableEntityEvent` of type `TASK_COMPLETED` containing a `TaskEntity`
- **THEN** `IndexingService.removeTaak` is called with the task entity's ID

#### Scenario: Non-TASK_COMPLETED event is ignored
- **WHEN** `onEvent` is called with a `FlowableEvent` of a different type
- **THEN** `IndexingService.removeTaak` is NOT called

### Requirement: UserTaskCompletionListener lifecycle methods return correct values
The listener SHALL return the expected values for `isFailOnException`, `isFireOnTransactionLifecycleEvent`, and `getOnTransaction`.

#### Scenario: Lifecycle method return values
- **WHEN** `isFailOnException` is called
- **THEN** `true` is returned

#### Scenario: isFireOnTransactionLifecycleEvent returns true
- **WHEN** `isFireOnTransactionLifecycleEvent` is called
- **THEN** `true` is returned

#### Scenario: getOnTransaction returns committed
- **WHEN** `getOnTransaction` is called
- **THEN** `TransactionDependentExecutionListener.ON_TRANSACTION_COMMITTED` is returned
