# productaanvraag-notification-idempotency Specification

## Purpose

Guarantees that a given productaanvraag object (identified by its Objects API UUID) results in at most
one zaak/case/process being created, no matter how many times ZAC receives a notification for it. Open
Notificaties guarantees at-least-once delivery, not exactly-once, so `NotificationReceiver.handleProductaanvraag`
must tolerate redelivery of the same notification without creating a duplicate zaak, CMMN case, BPMN
process, role assignment, or confirmation email.

## Requirements

### Requirement: At most one zaak per productaanvraag object

The system SHALL create at most one zaak (and, where applicable, at most one CMMN case or BPMN process)
for a given productaanvraag object UUID, regardless of how many times ZAC receives a notification
referencing that UUID.

#### Scenario: Single notification creates exactly one zaak
- **WHEN** ZAC receives one notification for a productaanvraag object with a configured CMMN or BPMN
  zaaktype mapping
- **THEN** exactly one zaak is created for that productaanvraag object

#### Scenario: Redelivered notification does not create a second zaak
- **WHEN** ZAC receives a second notification with the same `resourceUrl` (and therefore the same
  productaanvraag object UUID) as a notification it already fully processed
- **THEN** no additional zaak, CMMN case, or BPMN process is created for that productaanvraag object

#### Scenario: Concurrent redelivery across pods does not create a second zaak
- **WHEN** two notifications for the same productaanvraag object UUID are handled concurrently, whether by
  the same ZAC pod or by two different pods
- **THEN** at most one of the two results in zaak creation, and the other is skipped

### Requirement: Duplicate notifications are acknowledged, not treated as errors

When a notification for a productaanvraag object UUID that is already claimed or already processed is
received, the system SHALL treat this as a normal no-op and SHALL still acknowledge the notification
successfully to the caller.

#### Scenario: Skipped duplicate still returns a successful response
- **WHEN** `NotificationReceiver` receives a notification whose productaanvraag object UUID is already
  claimed (in progress and not stale) or already marked done
- **THEN** the HTTP response to Open Notificaties is the same successful response as for a newly-handled
  notification, and no exception is raised to the caller

### Requirement: A stalled claim is automatically reclaimable

If a productaanvraag object UUID was claimed for processing but never marked as completed (for example,
due to a crash or restart during processing), the system SHALL allow that productaanvraag to be reclaimed
and reprocessed after a configurable staleness period (default 10 minutes), without requiring manual
intervention.

#### Scenario: Staleness period is configurable

- **WHEN** the staleness period is set to a non-default value via configuration
- **THEN** the system uses that configured value, instead of the 10-minute default, to decide whether an
  in-progress claim is stale

#### Scenario: Stale in-progress claim is reclaimed
- **WHEN** a productaanvraag object UUID's claim record has status "in progress" and was started longer
  ago than the configured staleness period
- **AND** a new notification is received for that same productaanvraag object UUID
- **THEN** the claim is reclaimed and `handleProductaanvraag` processing proceeds for that notification

#### Scenario: Fresh in-progress claim is not reclaimed
- **WHEN** a productaanvraag object UUID's claim record has status "in progress" and was started more
  recently than the configured staleness period
- **AND** a new notification is received for that same productaanvraag object UUID
- **THEN** the claim is not reclaimed and processing is skipped for that notification

### Requirement: Idempotency guard is scoped to productaanvraag handling only

The persisted claim/idempotency mechanism SHALL apply only to the productaanvraag handling path
(`ProductaanvraagService.handleProductaanvraag`). It SHALL NOT alter the behavior of other notification
handlers invoked from `NotificationReceiver.notificatieReceive` (signaleringen, indexing, inbox documents,
Flowable process data, zaaktype configuration, websockets).

#### Scenario: Other handlers still run on every notification delivery
- **WHEN** a redelivered notification is received that is skipped for productaanvraag processing due to an
  existing claim
- **THEN** the other notification handlers (signaleringen, indexing, inbox documents, Flowable process
  data, zaaktype configuration, websockets) still run as normal for that notification
