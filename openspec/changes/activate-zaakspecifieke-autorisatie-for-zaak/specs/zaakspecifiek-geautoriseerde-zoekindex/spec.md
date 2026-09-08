## ADDED Requirements

### Requirement: The search index records the behandelaar of the zaak each row belongs to

The search index entry for a zaak, and for every taak and document associated with that zaak, SHALL record
which employee is that zaak's current behandelaar, so that werklijst and zoekresultaat queries can apply the
current-behandelaar exception without a live call to the zaakregister. An entry whose zaak has no
behandelaar SHALL record none.

#### Scenario: A zaak with a behandelaar is indexed with that behandelaar
- **WHEN** a zaak that has a behandelaar is indexed
- **THEN** the zaak's search index entry, and the search index entries of its taken and documenten, record
  that employee as the zaak's behandelaar

#### Scenario: A zaak without a behandelaar is indexed without one
- **WHEN** a zaak that has no behandelaar is indexed
- **THEN** the zaak's search index entry, and the search index entries of its taken and documenten, record
  no zaak-behandelaar

#### Scenario: Reassigning a zaak updates the recorded behandelaar
- **WHEN** a zaak's behandelaar changes
- **THEN** the zaak's search index entry, and the search index entries of its taken and documenten, record
  the new behandelaar

## MODIFIED Requirements

### Requirement: Werklijsten and zoekresultaten exclude zaakspecifiek geautoriseerde zaken from zaaktypes the user isn't authorized for

A werklijst or zoekresultaat query SHALL exclude a zaakspecifiek geautoriseerde zaak, and its taken and
documenten, from its results for a zaaktype where the requesting user does not hold the
`zaakspecifiek_geautoriseerd` application role, even if the user holds another application role
(`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, or `beheerder`) for that zaaktype. This
applies regardless of how many application roles the user holds for the zaaktype. A zaak of which the
requesting user is the current behandelaar SHALL NOT be excluded by this rule, and neither SHALL its taken
and documenten, mirroring the current-behandelaar exception that already applies to the same resources'
detail views.

#### Scenario: A user without the flag does not find a zaakspecifiek geautoriseerde zaak in a worklist
- **WHEN** a user who holds an application role for a zaaktype but not `zaakspecifiek_geautoriseerd`, and
  who is not that zaak's behandelaar, requests a werklijst that would otherwise include a zaakspecifiek
  geautoriseerde zaak of that zaaktype
- **THEN** that zaak is absent from the werklijst results

#### Scenario: A user without the flag does not find a task of a zaakspecifiek geautoriseerde zaak in a worklist
- **WHEN** a user who holds an application role for a zaaktype but not `zaakspecifiek_geautoriseerd`, and
  who is not the associated zaak's behandelaar, requests a werklijst that would otherwise include a taak
  belonging to a zaakspecifiek geautoriseerde zaak of that zaaktype
- **THEN** that taak is absent from the werklijst results

#### Scenario: A user without the flag does not find a zaakspecifiek geautoriseerde zaak in search results
- **WHEN** a user who holds an application role for a zaaktype but not `zaakspecifiek_geautoriseerd`, and
  who is not that zaak's behandelaar, performs a search that would otherwise match a zaakspecifiek
  geautoriseerde zaak, or a document linked to one, of that zaaktype
- **THEN** that zaak, respectively that document, is absent from the search results

#### Scenario: The behandelaar of a zaakspecifiek geautoriseerde zaak still finds it without the flag
- **WHEN** a user who holds an application role for a zaaktype but not `zaakspecifiek_geautoriseerd`
  requests a werklijst or performs a search that would include a zaakspecifiek geautoriseerde zaak of that
  zaaktype of which that user is the current behandelaar
- **THEN** that zaak is present in the results, as are its taken and its documenten

#### Scenario: A user without the flag for one zaaktype still sees flagged zaken of another zaaktype they hold the flag for
- **WHEN** a user holds `zaakspecifiek_geautoriseerd` together with another application role for zaaktype
  A, and holds only another application role (without `zaakspecifiek_geautoriseerd`) for zaaktype B, and
  requests a werklijst or zoekresultaat that would otherwise include zaakspecifiek geautoriseerde zaken of
  both zaaktypes of which that user is not the behandelaar
- **THEN** zaaktype A's zaakspecifiek geautoriseerde zaken are present in the results and zaaktype B's are
  absent
