---
status: accepted
date: 2026-10-27
decision-makers: Dimpact architecture team, Team Genève, PodiumD Zaak product owner
tags: [websocket, notifications, frontend, backend, architecture]
---

# Update the UI without waiting for notifications on changes that ZAC initiates

## Context and Problem Statement

ZAC updates its UI through websocket screen events. Many of these screen events are sent only when a notification
from Open Notificaties arrives. For a change that ZAC itself makes in an external system such as Open Zaak, the UI
therefore waits for the chain Open Zaak → Open Notificaties → ZAC. When notifications are slow or do not arrive, the
UI shows outdated progress or a timeout message, while the change in the external system has already succeeded.

For example, when an employee assigns or releases cases from the case work queue, the UI follows the progress
through `ZAAK_ROLLEN` screen events, which ZAC sends only when the `rol` notification arrives. Assigning and releasing
tasks from the task work queue does not have this problem: ZAC sends a screen event per task and a final screen event
itself, without waiting for notifications.

ZAC already knows that a change has succeeded as soon as the external system confirms it with a successful response,
such as `200 OK` or `201 Created`.

## Decision Drivers

* The UI reflects a change as soon as it has succeeded
* The UI keeps working when Open Notificaties is slow or unavailable
* Errors are shown to the employee as soon as they occur, and not after a timeout
* Notifications remain the source for changes made by other systems

## Considered Options

* Update the UI when the notification about the change arrives
* Update the UI when the external system confirms the change

## Decision Outcome

Chosen option: "Update the UI when the external system confirms the change", because ZAC already knows the outcome
of a change it initiated, so waiting for the notification only adds delay and a point of failure.

* When ZAC initiates a change in an external system, ZAC sends the screen events for that change as soon as the
  external system confirms it with a successful response.
* When the external system returns an error, ZAC continues with the remaining items of a batch and reports the
  failed items to the employee when the batch ends.
* ZAC keeps processing the notifications about these changes for their other effects, such as signaleringen and
  updating the search index. These notifications must not update the UI a second time for the same change, for
  example by counting an item twice in the progress of a batch.
* Changes made by other systems still reach the UI through notifications.

### Consequences

* Good, because the UI is updated faster and does not depend on Open Notificaties for changes that ZAC initiates.
* Good, because the employee no longer gets a timeout message for a change that has succeeded.
* Bad, because the code that changes an external system must also send the screen events, and must prevent duplicate
  UI updates from the notifications that follow.

### Confirmation

Not checked automatically. Every flow that follows this decision has unit tests and an integration test that show
that the UI is updated without notifications.

## Pros and Cons of the Options

### Update the UI when the notification arrives

* Good, because one mechanism updates the UI, for changes by ZAC and by other systems alike.
* Bad, because every UI update for a change by ZAC waits for the chain Open Zaak → Open Notificaties → ZAC.
* Bad, because the UI shows a timeout message when a notification does not arrive, although the change succeeded.

### Update the UI when the external system confirms the change

* Good, because the UI is updated as soon as the change has succeeded.
* Good, because the UI keeps working when Open Notificaties is slow or unavailable.
* Bad, because ZAC must prevent the notification from updating the UI a second time.

## More Information

Outcome of the architecture discussion in [PZ-13239](https://dimpact.atlassian.net/browse/PZ-13239). The first flow
to follow this decision is assigning and releasing cases from the case work queue
([PZ-13284](https://dimpact.atlassian.net/browse/PZ-13284)).
