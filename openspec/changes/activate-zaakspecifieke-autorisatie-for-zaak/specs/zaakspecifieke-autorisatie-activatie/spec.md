## Purpose

Lets an authorised employee mark an individual zaak as zaakspecifiek geautoriseerd from the zaakgegevens
edit form in ZAC, under business rules the zaakregister itself cannot enforce, and fixes the zaak's
behandelaar for as long as that marking stands.

## ADDED Requirements

### Requirement: A zaak can be marked as zaakspecifiek geautoriseerd while editing its zaakgegevens

The zaak update operation SHALL accept a request to mark a zaak as zaakspecifiek geautoriseerd. When the
request is accepted, the zaak SHALL be recorded in the zaakregister as zaakspecifiek geautoriseerd, using
the same representation that the system already reads when determining a zaak's zaakspecifiek geautoriseerd
status, and the updated zaak returned by the operation SHALL report itself as zaakspecifiek geautoriseerd.

#### Scenario: Marking an eligible zaak succeeds
- **WHEN** an authorised employee updates the zaakgegevens of a zaak that is not yet zaakspecifiek
  geautoriseerd, whose zaaktype is zaakspecifiek autoriseerbaar and which has a behandelaar, and requests
  that it be marked as zaakspecifiek geautoriseerd
- **THEN** the zaak is recorded as zaakspecifiek geautoriseerd in the zaakregister, and the zaak returned by
  the operation reports `isZaakspecifiekGeautoriseerd` as `true`

#### Scenario: Updating zaakgegevens without touching the marking
- **WHEN** an employee updates the zaakgegevens of a zaak without requesting a change to its zaakspecifiek
  geautoriseerd status
- **THEN** the zaak's zaakspecifiek geautoriseerd status is unchanged, and the rest of the update behaves
  exactly as it did before this capability existed

#### Scenario: Marking a zaak that is already marked changes nothing
- **WHEN** an employee updates the zaakgegevens of a zaak that is already zaakspecifiek geautoriseerd and
  requests that it be marked as zaakspecifiek geautoriseerd
- **THEN** the update succeeds, the zaak remains zaakspecifiek geautoriseerd, and no duplicate record of the
  marking is created in the zaakregister

### Requirement: Only a zaak of a zaakspecifiek autoriseerbaar zaaktype can be marked

The system SHALL reject a request to mark a zaak as zaakspecifiek geautoriseerd when the zaak's zaaktype is
not configured as zaakspecifiek autoriseerbaar, and SHALL leave the zaak entirely unchanged.

#### Scenario: The zaaktype is not zaakspecifiek autoriseerbaar
- **WHEN** an employee requests that a zaak of a zaaktype that is not zaakspecifiek autoriseerbaar be marked
  as zaakspecifiek geautoriseerd
- **THEN** the request is rejected with a distinct error identifying this cause, and neither the zaak's
  zaakgegevens nor its zaakspecifiek geautoriseerd status is changed

### Requirement: A zaak must have a behandelaar to be marked as zaakspecifiek geautoriseerd

The system SHALL reject a request to mark a zaak as zaakspecifiek geautoriseerd unless the zaak has a
behandelaar once the requested update has been applied. This rule is enforced by ZAC alone, because the
zaakregister has no knowledge of zaakspecifieke autorisatie.

#### Scenario: The zaak has no behandelaar
- **WHEN** an employee requests that a zaak without a behandelaar be marked as zaakspecifiek geautoriseerd,
  and the request does not assign one
- **THEN** the request is rejected with a distinct error identifying this cause, and the zaak is left
  entirely unchanged

#### Scenario: The same request assigns a behandelaar and marks the zaak
- **WHEN** an employee updates a zaak that has no behandelaar, assigning one and requesting that the zaak be
  marked as zaakspecifiek geautoriseerd in the same update
- **THEN** the update succeeds and the zaak is both assigned to that behandelaar and marked as zaakspecifiek
  geautoriseerd

### Requirement: Only specific employees may mark a zaak as zaakspecifiek geautoriseerd

The system SHALL permit a request to mark a zaak as zaakspecifiek geautoriseerd only from the zaak's current
behandelaar, or from an employee who holds the `zaakspecifiek_geautoriseerd` application role for the zaak's
zaaktype. Any other employee SHALL be refused, even when that employee is otherwise permitted to change the
zaak's zaakgegevens.

#### Scenario: The current behandelaar marks their own zaak
- **WHEN** the zaak's current behandelaar requests that the zaak be marked as zaakspecifiek geautoriseerd
- **THEN** the request is permitted

#### Scenario: An employee holding the flag for the zaaktype marks the zaak
- **WHEN** an employee who holds `zaakspecifiek_geautoriseerd` for the zaak's zaaktype, together with an
  application role that allows changing the zaakgegevens, requests that the zaak be marked as zaakspecifiek
  geautoriseerd
- **THEN** the request is permitted

#### Scenario: Another employee with edit rights is refused
- **WHEN** an employee who may change the zaak's zaakgegevens, but who is neither the zaak's behandelaar nor
  a holder of `zaakspecifiek_geautoriseerd` for its zaaktype, requests that the zaak be marked as
  zaakspecifiek geautoriseerd
- **THEN** the request is rejected and the zaak is left entirely unchanged

### Requirement: Zaakspecifieke autorisatie cannot be lifted

The system SHALL reject a request to mark an already zaakspecifiek geautoriseerde zaak as no longer
zaakspecifiek geautoriseerd, with an error code distinct from every other error code the system returns.

#### Scenario: An employee tries to lift the marking
- **WHEN** an employee updates the zaakgegevens of a zaakspecifiek geautoriseerde zaak and requests that it
  no longer be zaakspecifiek geautoriseerd
- **THEN** the request is rejected with an error code identifying this cause, and the zaak remains
  zaakspecifiek geautoriseerd

### Requirement: A zaak cannot be released while it is zaakspecifiek geautoriseerd

The system SHALL refuse every attempt to remove the behandelaar from a zaak while that zaak is zaakspecifiek
geautoriseerd, from the zaakdetailpagina as well as from the zaken werkvoorraad, with an error code
identifying this cause. Zaken that are not zaakspecifiek geautoriseerd in the same werkvoorraad selection
SHALL still be released. The refusal SHALL be evaluated against the zaak's zaakspecifiek geautoriseerd status
at the time of the request, so that a zaak whose marking is later lifted becomes releasable again without
further intervention.

#### Scenario: Releasing a zaakspecifiek geautoriseerde zaak from the zaakdetailpagina
- **WHEN** an employee attempts to remove the behandelaar from a zaakspecifiek geautoriseerde zaak on the
  zaakdetailpagina
- **THEN** the attempt is refused with an error code identifying this cause and the zaak keeps its
  behandelaar

#### Scenario: Releasing a selection of zaken from the werkvoorraad that includes a marked zaak
- **WHEN** an employee releases a selection of zaken from the zaken werkvoorraad in which one or more zaken
  are zaakspecifiek geautoriseerd
- **THEN** those zaken keep their behandelaar and the employee is informed that they were not released,
  while the remaining zaken in the selection are released as before

#### Scenario: A zaak that is not zaakspecifiek geautoriseerd is released as before
- **WHEN** an employee removes the behandelaar from a zaak that is not zaakspecifiek geautoriseerd
- **THEN** the zaak is released exactly as it was before this capability existed

### Requirement: The behandelaar cannot be changed while a zaak is zaakspecifiek geautoriseerd

The system SHALL refuse every attempt to assign a different behandelaar to a zaak while that zaak is
zaakspecifiek geautoriseerd, from the zaakdetailpagina as well as from the zaken werkvoorraad, with an error
code identifying this cause. This restriction is enforced in the ZAC backend; the frontend is not required to
prevent the attempt. The refusal SHALL be evaluated against the zaak's zaakspecifiek geautoriseerd status at
the time of the request, so that a zaak whose marking is later lifted becomes reassignable again without
further intervention.

#### Scenario: Reassigning a zaakspecifiek geautoriseerde zaak from the zaakdetailpagina
- **WHEN** an employee attempts to assign a zaakspecifiek geautoriseerde zaak to a behandelaar other than
  its current one
- **THEN** the attempt is refused with an error code identifying this cause and the zaak keeps its current
  behandelaar

#### Scenario: Reassigning a zaakspecifiek geautoriseerde zaak from the werkvoorraad
- **WHEN** an employee distributes a selection of zaken from the zaken werkvoorraad in which one or more
  zaken are zaakspecifiek geautoriseerd
- **THEN** those zaken keep their current behandelaar and the employee is informed that they were not
  reassigned, while the remaining zaken in the selection are assigned as before

#### Scenario: Reassigning the zaak to the behandelaar it already has
- **WHEN** an employee updates a zaakspecifiek geautoriseerde zaak leaving its behandelaar unchanged
- **THEN** the update is not refused on account of the behandelaar

### Requirement: Refusals are reported as error codes that the frontend translates

Every refusal defined by this capability SHALL be reported by the backend as an error code string, in the
same form the system already uses for its other error codes, and SHALL NOT contain human-readable message
text in any language. Each such error code SHALL have a corresponding translation in both the Dutch and the
English frontend translation files, so that the employee sees a clear message in their own language.

#### Scenario: The backend response carries a code, not a sentence
- **WHEN** any refusal defined by this capability is returned to a client
- **THEN** the response carries an error code string in the system's existing error-code form, and carries no
  human-readable message text

#### Scenario: Every error code has a translation in both languages
- **WHEN** the error codes introduced by this capability are looked up in the frontend translation files
- **THEN** each one has an entry in both the Dutch and the English translation file

#### Scenario: The employee sees a translated message
- **WHEN** an employee triggers a refusal defined by this capability in the user interface
- **THEN** the message shown is the translation of that refusal's error code, not the code itself

### Requirement: The werkvoorraad dialogs state why a zaak was left out, distinguishing the reasons

When a werkvoorraad verdelen or vrijgeven action leaves one or more of the selected zaken unprocessed, the
system SHALL tell the employee how many zaken were left out and why, naming each distinct reason separately
rather than combining them into a single undifferentiated message. Being zaakspecifiek geautoriseerd and
being already afgehandeld SHALL be reported as distinct reasons.

#### Scenario: A selection containing only zaakspecifiek geautoriseerde zaken among the skipped
- **WHEN** an employee distributes or releases a selection of zaken in which the only zaken that cannot be
  processed are zaakspecifiek geautoriseerd
- **THEN** the employee is told how many zaken were left out because they are zaakspecifiek geautoriseerd,
  in a message that names that reason

#### Scenario: A selection whose skipped zaken have both reasons
- **WHEN** an employee distributes or releases a selection of zaken in which some cannot be processed
  because they are zaakspecifiek geautoriseerd and others because they are already afgehandeld
- **THEN** the employee is told how many zaken were left out for each reason, reported separately

#### Scenario: A selection in which every zaak can be processed
- **WHEN** an employee distributes or releases a selection of zaken in which none are zaakspecifiek
  geautoriseerd or already afgehandeld
- **THEN** no reason message is shown, and the feedback is exactly what it was before this capability
  existed
