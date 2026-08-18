## Requirements

### Requirement: Zaak mutations update the screen from their own response
The system SHALL update the zaak screen from the `RestZaak` returned by a mutation, without waiting for an Open Notificaties webhook. Every zaak REST endpoint that returns a `RestZaak` SHALL have its response written into the shared zaak cache entry rather than discarded.

#### Scenario: Editing zaakgegevens updates the screen immediately
- **WHEN** a user saves the "zaakgegevens bewerken" form and the `PATCH /rest/zaken/zaak/{uuid}` call succeeds
- **THEN** the zaak screen shows the new values immediately, driven by the response body, with no refetch and no wait for a websocket event

#### Scenario: A lifecycle action reflects its post-operation state
- **WHEN** a user completes afsluiten, heropenen or afbreken
- **THEN** the screen immediately shows the resulting status, resultaat and einddatum, taken from a zaak the backend re-read after the operation rather than from the pre-operation object

#### Scenario: Rights shown match the state the response describes
- **WHEN** a zaak is closed and the response is written to the cache
- **THEN** the `rechten` in that response are evaluated on the closed zaak, so no edit affordance remains visible for a zaak that can no longer be edited

#### Scenario: Authorisation is decided on the pre-mutation state
- **WHEN** a lifecycle endpoint checks whether the caller may perform the action
- **THEN** the policy decision uses the rights evaluated on the zaak as it was when the user acted, not the re-read state

### Requirement: The zaak has one cache entry with one key
The system SHALL hold the zaak in a single cache entry keyed by uuid, constructed in exactly one place, so any component can update the screen without an output binding to the component that renders it.

#### Scenario: A child component updates the screen without a binding
- **WHEN** a form or dialog other than the zaak screen writes an updated zaak to the cache
- **THEN** the zaak screen re-renders from that write, without the child knowing which component displays the zaak and without a chain of output bindings

#### Scenario: A missing cache provider fails loudly
- **WHEN** the cache client is not provided in a context that constructs the zaak service
- **THEN** construction fails at injection, rather than the cache write silently doing nothing and leaving the screen stale with no error to trace

#### Scenario: Navigating to a zaak does not refetch what was just resolved
- **WHEN** the route resolves a zaak and the screen points its query at that zaak's uuid
- **THEN** the resolved zaak is written to the cache before the query is pointed at it, so the query activates onto fresh data and issues no initial fetch

### Requirement: Screen side effects re-run only when what they depend on changes
The system SHALL scope the zaak screen's side effects to the data each one actually reads, so that a cache write refreshes what the change affects and nothing else.

#### Scenario: Content-only changes do not re-fetch uuid-scoped data
- **WHEN** an updated zaak for the same uuid is written to the cache
- **THEN** BAG objects are not reloaded, since they depend only on the zaak's identity

#### Scenario: The action menu reflects changed rights
- **WHEN** an updated zaak carries different `rechten`
- **THEN** the action menu is rebuilt, so actions the user may no longer perform stop being offered

#### Scenario: The historie reflects a change made while it is open
- **WHEN** a zaak changes while the user is on the historie tab
- **THEN** the historie is invalidated and shows the new entry, without requiring a tab switch

### Requirement: A user's own change is not announced back to them
The system SHALL decide whether to announce a websocket zaak event by comparing content, not by elapsed time. An event whose refetched zaak is unchanged from what is already cached SHALL be absorbed silently.

#### Scenario: The echo of your own edit is silent
- **WHEN** the webhook for a change the user just made arrives, 5–20 seconds after the screen already updated from the save response
- **THEN** no "zaak is gewijzigd" notification is shown and the screen does not visibly change

#### Scenario: Another user's change is announced
- **WHEN** a websocket event arrives and the refetched zaak differs from what is cached
- **THEN** the screen updates and the change is announced, with the same wording as before this capability existed

#### Scenario: A failed refetch is not mistaken for an echo
- **WHEN** the refetch triggered by a websocket event fails
- **THEN** the change is still announced, rather than the unchanged cache entry being read as "nothing happened"

#### Scenario: Role changes are always announced
- **WHEN** a websocket event reports changed roles on the zaak
- **THEN** the betrokkenen list is invalidated and the change is announced unconditionally, because the zaak payload does not carry the betrokkenen list and comparing it would suppress every such change

### Requirement: Search-backed screens keep their notification-driven refresh
The system SHALL continue to refresh search-index-backed screens from notifications rather than from mutation responses, because those screens read an index that is only updated when the notification arrives.

#### Scenario: Work list still waits for reindexing
- **WHEN** a zaak is changed and a work list, search result or dashboard card would display it
- **THEN** that screen refreshes on the notification, since writing the mutation response there would be reverted by the next refetch until the search index catches up
